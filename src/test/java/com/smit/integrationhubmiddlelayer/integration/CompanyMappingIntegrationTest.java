package com.smit.integrationhubmiddlelayer.integration;

import com.smit.integrationhubmiddlelayer.AbstractIntegrationTest;
import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ErrorResponse;
import com.smit.integrationhubmiddlelayer.dto.InterfaceCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MappingSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateRowDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateSectionDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.dto.ResolvedMappingRowDto;
import com.smit.integrationhubmiddlelayer.dto.RowValueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the Company Mapping 2-level cascade (Override / Template), exercised
 * through real HTTP calls against a real Postgres-backed application instance rather than mocked
 * repositories, plus the duplicate-creation conflict case.
 */
class CompanyMappingIntegrationTest extends AbstractIntegrationTest
{
    @Test
    void resolvedMapping_walksThroughTemplateAndOverride_inPriorityOrder()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long mandatorId = createMandator("Autohaus Rath GmbH " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long companyId = createCompany(mandatorId, "Wien Zentrale " + suffix, "DMS_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long interfaceId = createInterface("Volvo Grip Api " + suffix); //$NON-NLS-1$

        MappingTemplateRowDto rowTemplate = new MappingTemplateRowDto(null, "Customer Number", "CUST_NO", 1); //$NON-NLS-1$ //$NON-NLS-2$
        MappingTemplateSectionDto sectionTemplate = new MappingTemplateSectionDto(null, "Customer Data", 1, List.of(rowTemplate)); //$NON-NLS-1$
        MappingTemplateDto createdTemplate = createMappingTemplate(interfaceId, List.of(sectionTemplate));
        MappingTemplateRowDto createdRow = createdTemplate.getSections().get(0).getRows().get(0);

        Long companyMappingId = createCompanyMapping(companyId, interfaceId);

        // Nothing overridden yet -> row resolves at TEMPLATE level with the template's third-party value.
        CompanyMappingDetailDto detail = getCompanyMapping(companyId, companyMappingId);
        ResolvedMappingRowDto resolvedRow = onlyRow(detail);
        assertThat(resolvedRow.getSourceLevel()).isEqualTo(MappingSourceLevel.TEMPLATE);
        assertThat(resolvedRow.getEffectiveValue()).isEqualTo("CUST_NO"); //$NON-NLS-1$

        // Overriding the row wins over the template value.
        setRowOverride(companyId, companyMappingId, createdRow.getId(), "CUST_NR_OVERRIDE"); //$NON-NLS-1$
        detail = getCompanyMapping(companyId, companyMappingId);
        resolvedRow = onlyRow(detail);
        assertThat(resolvedRow.getSourceLevel()).isEqualTo(MappingSourceLevel.OVERRIDE);
        assertThat(resolvedRow.getEffectiveValue()).isEqualTo("CUST_NR_OVERRIDE"); //$NON-NLS-1$

        // Deleting the override falls back to the template's original value.
        deleteRowOverride(companyId, companyMappingId, createdRow.getId());
        detail = getCompanyMapping(companyId, companyMappingId);
        resolvedRow = onlyRow(detail);
        assertThat(resolvedRow.getSourceLevel()).isEqualTo(MappingSourceLevel.TEMPLATE);
        assertThat(resolvedRow.getEffectiveValue()).isEqualTo("CUST_NO"); //$NON-NLS-1$
    }

    @Test
    void createCompanyMapping_returns409WithErrorEnvelope_whenOneAlreadyExistsForSameCompanyAndInterface()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long mandatorId = createMandator("Autohaus Rath GmbH " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long companyId = createCompany(mandatorId, "Wien Zentrale " + suffix, "DMS_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long interfaceId = createInterface("Volvo Grip Api " + suffix); //$NON-NLS-1$
        createMappingTemplate(interfaceId, List.of(
                new MappingTemplateSectionDto(null, "Customer Data", 1, //$NON-NLS-1$
                        List.of(new MappingTemplateRowDto(null, "Customer Number", "CUST_NO", 1))))); //$NON-NLS-1$ //$NON-NLS-2$
        createCompanyMapping(companyId, interfaceId);

        CompanyMappingCreateRequest request = new CompanyMappingCreateRequest();
        request.setInterfaceId(interfaceId);
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/companies/" + companyId + "/mappings", request, ErrorResponse.class); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_COMPANY_MAPPING"); //$NON-NLS-1$
    }

    private Long createMandator(String name, String externalMandatorId)
    {
        MandatorCreateRequest request = new MandatorCreateRequest();
        request.setName(name);
        request.setExternalMandatorId(externalMandatorId);
        ResponseEntity<MandatorDetailDto> response = restTemplate.postForEntity("/mandators", request, MandatorDetailDto.class); //$NON-NLS-1$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private Long createCompany(Long mandatorId, String name, String dmsCompanyId)
    {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName(name);
        request.setDmsCompanyId(dmsCompanyId);
        ResponseEntity<CompanyDetailDto> response = restTemplate.postForEntity(
                "/mandators/" + mandatorId + "/companies", request, CompanyDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private Long createInterface(String name)
    {
        InterfaceCreateRequest request = new InterfaceCreateRequest();
        request.setName(name);
        ResponseEntity<InterfaceDetailDto> response = restTemplate.postForEntity("/interfaces", request, InterfaceDetailDto.class); //$NON-NLS-1$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private MappingTemplateDto createMappingTemplate(Long interfaceId, List<MappingTemplateSectionDto> sections)
    {
        MappingTemplateUpsertRequest request = new MappingTemplateUpsertRequest();
        request.setSections(sections);
        ResponseEntity<MappingTemplateDto> response = restTemplate.postForEntity(
                "/interfaces/" + interfaceId + "/mapping-template", request, MappingTemplateDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private Long createCompanyMapping(Long companyId, Long interfaceId)
    {
        CompanyMappingCreateRequest request = new CompanyMappingCreateRequest();
        request.setInterfaceId(interfaceId);
        ResponseEntity<CompanyMappingDetailDto> response = restTemplate.postForEntity(
                "/companies/" + companyId + "/mappings", request, CompanyMappingDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private CompanyMappingDetailDto getCompanyMapping(Long companyId, Long companyMappingId)
    {
        ResponseEntity<CompanyMappingDetailDto> response = restTemplate.getForEntity(
                "/companies/" + companyId + "/mappings/" + companyMappingId, CompanyMappingDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private void setRowOverride(Long companyId, Long companyMappingId, Long templateRowId, String value)
    {
        RowValueRequest request = new RowValueRequest();
        request.setValue(value);
        ResponseEntity<ResolvedMappingRowDto> response = restTemplate.exchange(
                "/companies/" + companyId + "/mappings/" + companyMappingId + "/rows/" + templateRowId, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                HttpMethod.PUT, new HttpEntity<>(request), ResolvedMappingRowDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void deleteRowOverride(Long companyId, Long companyMappingId, Long templateRowId)
    {
        ResponseEntity<ResolvedMappingRowDto> response = restTemplate.exchange(
                "/companies/" + companyId + "/mappings/" + companyMappingId + "/rows/" + templateRowId, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                HttpMethod.DELETE, HttpEntity.EMPTY, ResolvedMappingRowDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static ResolvedMappingRowDto onlyRow(CompanyMappingDetailDto detail)
    {
        assertThat(detail.getSections()).hasSize(1);
        assertThat(detail.getSections().get(0).getRows()).hasSize(1);
        return detail.getSections().get(0).getRows().get(0);
    }
}
