package com.smit.integrationhubmiddlelayer.integration;

import com.smit.integrationhubmiddlelayer.AbstractIntegrationTest;
import com.smit.integrationhubmiddlelayer.dto.AdditionalDataValueRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.dto.ErrorResponse;
import com.smit.integrationhubmiddlelayer.dto.InterfaceCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ServiceConfigurationDto;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The machine-to-machine API: API-key authentication and lookup of a Company's effective
 * configuration by DMS identifiers (Mandator ID + padded Company ID + Interface name).
 */
@TestPropertySource(properties = "integration-hub.service-api.keys=old-key, test-service-key") //$NON-NLS-1$
class ServiceConfigurationIntegrationTest extends AbstractIntegrationTest
{
    private static final String API_KEY = "test-service-key"; //$NON-NLS-1$

    @Test
    void serviceConfiguration_resolvesByPaddedCompanyIdAndCaseInsensitiveNames()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String mandatorExternalId = "MD_" + suffix; //$NON-NLS-1$
        Long mandatorId = createMandator("SMIT " + suffix, mandatorExternalId); //$NON-NLS-1$
        Long companyId = createCompany(mandatorId, "Bjelovar " + suffix, "1"); //$NON-NLS-1$ //$NON-NLS-2$
        Long interfaceId = createInterface("MIMW integration " + suffix); //$NON-NLS-1$
        createConfigurationTemplate(interfaceId, List.of(
                new ConfigurationTemplateEntryDto(null, "SPRING_DATASOURCE_URL", ConfigValueType.TEXT, "jdbc:sqlserver://x", null, "url", 1), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                new ConfigurationTemplateEntryDto(null, "AS400_BRANCH", ConfigValueType.NUMBER, "1", null, "branch", 2))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        createCompanyConfiguration(companyId, interfaceId);
        setCompanyAdditionalData(companyId, "db_user", "test"); //$NON-NLS-1$ //$NON-NLS-2$

        ResponseEntity<ServiceConfigurationDto> response = get(
                url(mandatorExternalId.toLowerCase(), "000001", "mimw INTEGRATION " + suffix), API_KEY, ServiceConfigurationDto.class); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ServiceConfigurationDto body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMandator().getExternalMandatorId()).isEqualTo(mandatorExternalId);
        assertThat(body.getCompany().getDmsCompanyId()).isEqualTo("1"); //$NON-NLS-1$
        assertThat(body.getConfig()).containsEntry("SPRING_DATASOURCE_URL", "jdbc:sqlserver://x") //$NON-NLS-1$ //$NON-NLS-2$
                .containsEntry("AS400_BRANCH", "1"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(body.getAdditionalData()).containsEntry("db_user", "test"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    void serviceConfiguration_returns404_whenCompanyHasNoConfigurationForInterface()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long mandatorId = createMandator("SMIT " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        createCompany(mandatorId, "Bjelovar " + suffix, "7"); //$NON-NLS-1$ //$NON-NLS-2$
        createInterface("Unused " + suffix); //$NON-NLS-1$

        ResponseEntity<ErrorResponse> response = get(url("MD_" + suffix, "7", "Unused " + suffix), API_KEY, ErrorResponse.class); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError().getCode()).isEqualTo("CONFIGURATION_NOT_FOUND"); //$NON-NLS-1$
    }

    @Test
    void serviceConfiguration_returns404_forUnknownMandator()
    {
        ResponseEntity<ErrorResponse> response = get(url("NOPE_" + UUID.randomUUID(), "1", "x"), API_KEY, ErrorResponse.class); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError().getCode()).isEqualTo("MANDATOR_NOT_FOUND"); //$NON-NLS-1$
    }

    @Test
    void serviceConfiguration_returns401_withoutOrWithWrongApiKey_evenWithAValidUserLogin()
    {
        assertThat(get(url("B0", "1", "x"), null, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertThat(get(url("B0", "1", "x"), "wrong", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Test
    void serviceApi_isReadOnly()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", API_KEY); //$NON-NLS-1$
        ResponseEntity<String> response = restTemplate.exchange(url("B0", "1", "x"), HttpMethod.POST, new HttpEntity<>(headers), String.class); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private static String url(String mandatorId, String companyId, String interfaceName)
    {
        return "/service/configuration?mandatorId=" + mandatorId + "&companyId=" + companyId //$NON-NLS-1$ //$NON-NLS-2$
                + "&interfaceName=" + interfaceName; //$NON-NLS-1$
    }

    private <T> ResponseEntity<T> get(String url, String apiKey, Class<T> type)
    {
        HttpHeaders headers = new HttpHeaders();
        if (apiKey != null)
        {
            headers.set("X-Api-Key", apiKey); //$NON-NLS-1$
        }
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), type);
    }

    private Long createMandator(String name, String externalMandatorId)
    {
        MandatorCreateRequest request = new MandatorCreateRequest();
        request.setName(name);
        request.setExternalMandatorId(externalMandatorId);
        return restTemplate.postForEntity("/mandators", request, MandatorDetailDto.class).getBody().getId(); //$NON-NLS-1$
    }

    private Long createCompany(Long mandatorId, String name, String dmsCompanyId)
    {
        CompanyCreateRequest request = new CompanyCreateRequest();
        request.setName(name);
        request.setDmsCompanyId(dmsCompanyId);
        return restTemplate.postForEntity("/mandators/" + mandatorId + "/companies", request, CompanyDetailDto.class) //$NON-NLS-1$ //$NON-NLS-2$
                .getBody().getId();
    }

    private Long createInterface(String name)
    {
        InterfaceCreateRequest request = new InterfaceCreateRequest();
        request.setName(name);
        return restTemplate.postForEntity("/interfaces", request, InterfaceDetailDto.class).getBody().getId(); //$NON-NLS-1$
    }

    private void createConfigurationTemplate(Long interfaceId, List<ConfigurationTemplateEntryDto> entries)
    {
        ConfigurationTemplateUpsertRequest request = new ConfigurationTemplateUpsertRequest();
        request.setEntries(entries);
        ResponseEntity<ConfigurationTemplateDto> response = restTemplate.postForEntity(
                "/interfaces/" + interfaceId + "/configuration-template", request, ConfigurationTemplateDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void createCompanyConfiguration(Long companyId, Long interfaceId)
    {
        CompanyConfigurationCreateRequest request = new CompanyConfigurationCreateRequest();
        request.setInterfaceId(interfaceId);
        ResponseEntity<CompanyConfigurationDetailDto> response = restTemplate.postForEntity(
                "/companies/" + companyId + "/configurations", request, CompanyConfigurationDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void setCompanyAdditionalData(Long companyId, String key, String value)
    {
        AdditionalDataValueRequest request = new AdditionalDataValueRequest();
        request.setValue(value);
        restTemplate.exchange("/companies/" + companyId + "/additional-data/" + key, HttpMethod.PUT, new HttpEntity<>(request), Void.class); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
