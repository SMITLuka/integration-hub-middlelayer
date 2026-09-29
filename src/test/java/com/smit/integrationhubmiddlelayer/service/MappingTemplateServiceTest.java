package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.MappingTemplateRowDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateSectionDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplate;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateRow;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateSection;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for saving an existing Mapping Template: sections and rows must be merged in
 * place (stable ids), because Company Mapping values reference template rows by id and recreating
 * them broke those references (foreign key failure once a Company had mapping values).
 */
@ExtendWith(MockitoExtension.class)
class MappingTemplateServiceTest
{
    private static final Long INTERFACE_ID = 1L;

    @Mock
    private InterfaceRepository interfaceRepository;

    @Mock
    private MappingTemplateRepository mappingTemplateRepository;

    @Mock
    private CompanyMappingRepository companyMappingRepository;

    @InjectMocks
    private MappingTemplateService mappingTemplateService;

    @Test
    void replace_updatesExistingSectionsAndRowsInPlace_removesMissing_addsNew()
    {
        Interface interfaceEntity = Interface.builder().id(INTERFACE_ID).name("SPA Integration").build(); //$NON-NLS-1$
        MappingTemplate template = MappingTemplate.builder().id(5L).interfaceEntity(interfaceEntity).sections(new ArrayList<>()).build();
        MappingTemplateSection customerSection = MappingTemplateSection.builder().id(10L).mappingTemplate(template)
                .name("Customer Data").sortOrder(1).rows(new ArrayList<>()).build(); //$NON-NLS-1$
        MappingTemplateRow customerNumberRow = MappingTemplateRow.builder().id(100L).section(customerSection)
                .descriptor("Customer Number").thirdPartyValue("CUST_NO").sortOrder(1).build(); //$NON-NLS-1$ //$NON-NLS-2$
        customerSection.getRows().add(customerNumberRow);
        customerSection.getRows().add(MappingTemplateRow.builder().id(101L).section(customerSection)
                .descriptor("Obsolete").sortOrder(2).build()); //$NON-NLS-1$
        template.getSections().add(customerSection);
        template.getSections().add(MappingTemplateSection.builder().id(11L).mappingTemplate(template)
                .name("Removed Section").rows(new ArrayList<>()).build()); //$NON-NLS-1$

        when(interfaceRepository.findById(INTERFACE_ID)).thenReturn(Optional.of(interfaceEntity));
        when(mappingTemplateRepository.findByInterfaceEntityId(INTERFACE_ID)).thenReturn(Optional.of(template));
        when(mappingTemplateRepository.save(any(MappingTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MappingTemplateUpsertRequest request = new MappingTemplateUpsertRequest();
        request.setSections(List.of(
                new MappingTemplateSectionDto(10L, "Customer", 1, List.of( //$NON-NLS-1$
                        new MappingTemplateRowDto(100L, "Customer Number", "KUNDEN_NR", 1), //$NON-NLS-1$ //$NON-NLS-2$
                        new MappingTemplateRowDto(null, "VAT ID", "VAT", 2))), //$NON-NLS-1$ //$NON-NLS-2$
                new MappingTemplateSectionDto(null, "Vehicle Data", 2, List.of( //$NON-NLS-1$
                        new MappingTemplateRowDto(null, "VIN", "VIN", 1))))); //$NON-NLS-1$ //$NON-NLS-2$

        mappingTemplateService.replace(INTERFACE_ID, request);

        assertThat(template.getSections()).hasSize(2);
        assertThat(template.getSections().get(0)).isSameAs(customerSection);
        assertThat(customerSection.getName()).isEqualTo("Customer"); //$NON-NLS-1$
        assertThat(customerSection.getRows()).hasSize(2);
        assertThat(customerSection.getRows().get(0)).isSameAs(customerNumberRow);
        assertThat(customerNumberRow.getId()).isEqualTo(100L);
        assertThat(customerNumberRow.getThirdPartyValue()).isEqualTo("KUNDEN_NR"); //$NON-NLS-1$
        assertThat(customerSection.getRows().get(1).getDescriptor()).isEqualTo("VAT ID"); //$NON-NLS-1$
        assertThat(customerSection.getRows().get(1).getSection()).isSameAs(customerSection);

        MappingTemplateSection vehicleSection = template.getSections().get(1);
        assertThat(vehicleSection.getName()).isEqualTo("Vehicle Data"); //$NON-NLS-1$
        assertThat(vehicleSection.getMappingTemplate()).isSameAs(template);
        assertThat(vehicleSection.getRows()).extracting(MappingTemplateRow::getDescriptor).containsExactly("VIN"); //$NON-NLS-1$
    }
}
