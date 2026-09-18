package com.smit.integrationhubmiddlelayer.integration;

import com.smit.integrationhubmiddlelayer.AbstractIntegrationTest;
import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ErrorResponse;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the Mandator delete lifecycle: a Mandator with Companies attached must
 * refuse deletion (409, MandatorHasCompaniesException), and only once its Companies are removed
 * does the delete succeed, after which the Mandator is genuinely gone (404 on GET).
 */
class MandatorLifecycleIntegrationTest extends AbstractIntegrationTest
{
    @Test
    void deleteMandator_isRefusedWhileCompaniesExist_thenSucceedsOnceTheyAreRemoved()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long mandatorId = createMandator("Autohaus Rath GmbH " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long companyId = createCompany(mandatorId, "Wien Zentrale " + suffix, "DMS_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$

        // Mandator still has a Company attached -> delete is refused with a 409 error envelope.
        ResponseEntity<ErrorResponse> conflictResponse = restTemplate.exchange(
                "/mandators/" + mandatorId, HttpMethod.DELETE, HttpEntity.EMPTY, ErrorResponse.class); //$NON-NLS-1$
        assertThat(conflictResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflictResponse.getBody()).isNotNull();
        assertThat(conflictResponse.getBody().getError().getCode()).isEqualTo("MANDATOR_HAS_COMPANIES"); //$NON-NLS-1$
        assertThat(conflictResponse.getBody().getError().getDescription()).contains(String.valueOf(mandatorId));

        // Remove the Company first.
        ResponseEntity<Void> deleteCompanyResponse = restTemplate.exchange(
                "/companies/" + companyId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class); //$NON-NLS-1$
        assertThat(deleteCompanyResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Now the Mandator can be deleted.
        ResponseEntity<Void> deleteMandatorResponse = restTemplate.exchange(
                "/mandators/" + mandatorId, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class); //$NON-NLS-1$
        assertThat(deleteMandatorResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // It is genuinely gone.
        ResponseEntity<ErrorResponse> getResponse = restTemplate.getForEntity(
                "/mandators/" + mandatorId, ErrorResponse.class); //$NON-NLS-1$
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
}
