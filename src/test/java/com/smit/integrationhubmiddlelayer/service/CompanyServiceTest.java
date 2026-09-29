package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyUpdateRequest;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.DuplicateCompanyException;
import com.smit.integrationhubmiddlelayer.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the Company ID (dmsCompanyId) rules: blank IDs are stored as NULL (the ID is UNIQUE per
 * Mandator) and an ID already used by another Company of the same Mandator is rejected on create and update.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest
{
    private static final Long MANDATOR_ID = 1L;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MandatorService mandatorService;

    @Mock
    private CompanyMappingService companyMappingService;

    @Mock
    private CompanyConfigurationService companyConfigurationService;

    @InjectMocks
    private CompanyService companyService;

    private final Mandator mandator = Mandator.builder().id(MANDATOR_ID).name("Autokuca Longin").build(); //$NON-NLS-1$

    @Test
    void create_storesBlankCompanyIdAsNull()
    {
        when(mandatorService.findOrThrow(MANDATOR_ID)).thenReturn(mandator);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        companyService.create(MANDATOR_ID, createRequest(""));

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getDmsCompanyId()).isNull();
        assertThat(captor.getValue().getUuid()).isNotNull();
        verify(companyRepository, never()).findByMandatorIdAndDmsCompanyId(anyLong(), anyString());
    }

    @Test
    void create_throwsDuplicate_whenCompanyIdTakenWithinMandator()
    {
        when(mandatorService.findOrThrow(MANDATOR_ID)).thenReturn(mandator);
        when(companyRepository.findByMandatorIdAndDmsCompanyId(MANDATOR_ID, "1")).thenReturn(Optional.of(company(20L, "1"))); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> companyService.create(MANDATOR_ID, createRequest("1"))) //$NON-NLS-1$
                .isInstanceOf(DuplicateCompanyException.class);
        verify(companyRepository, never()).save(any());
    }

    @Test
    void update_throwsDuplicate_whenCompanyIdBelongsToAnotherCompany()
    {
        Company current = company(10L, "1"); //$NON-NLS-1$
        when(companyRepository.findById(10L)).thenReturn(Optional.of(current));
        when(companyRepository.findByMandatorIdAndDmsCompanyId(MANDATOR_ID, "2")).thenReturn(Optional.of(company(20L, "2"))); //$NON-NLS-1$ //$NON-NLS-2$

        assertThatThrownBy(() -> companyService.update(10L, updateRequest("2"))) //$NON-NLS-1$
                .isInstanceOf(DuplicateCompanyException.class);
        assertThat(current.getDmsCompanyId()).isEqualTo("1"); //$NON-NLS-1$
    }

    @Test
    void update_allowsKeepingOwnCompanyId()
    {
        Company current = company(10L, "1"); //$NON-NLS-1$
        when(companyRepository.findById(10L)).thenReturn(Optional.of(current));
        when(companyRepository.findByMandatorIdAndDmsCompanyId(MANDATOR_ID, "1")).thenReturn(Optional.of(current)); //$NON-NLS-1$

        companyService.update(10L, updateRequest("1")); //$NON-NLS-1$

        assertThat(current.getDmsCompanyId()).isEqualTo("1"); //$NON-NLS-1$
    }

    @Test
    void update_storesBlankCompanyIdAsNull()
    {
        Company current = company(10L, "1"); //$NON-NLS-1$
        when(companyRepository.findById(10L)).thenReturn(Optional.of(current));

        companyService.update(10L, updateRequest("  ")); //$NON-NLS-1$

        assertThat(current.getDmsCompanyId()).isNull();
    }

    private Company company(Long id, String dmsCompanyId)
    {
        return Company.builder().id(id).mandator(mandator).name("Poslovnica " + id).dmsCompanyId(dmsCompanyId).build(); //$NON-NLS-1$
    }

    private static CompanyCreateRequest createRequest(String dmsCompanyId)
    {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName("Poslovnica Zagreb 1"); //$NON-NLS-1$
        request.setDmsCompanyId(dmsCompanyId);
        return request;
    }

    private static CompanyUpdateRequest updateRequest(String dmsCompanyId)
    {
        CompanyUpdateRequest request = new CompanyUpdateRequest();
        request.setName("Poslovnica Zagreb 1"); //$NON-NLS-1$
        request.setDmsCompanyId(dmsCompanyId);
        return request;
    }
}
