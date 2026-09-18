package com.smit.integrationhubmiddlelayer.integration;

import com.smit.integrationhubmiddlelayer.AbstractIntegrationTest;
import com.smit.integrationhubmiddlelayer.dto.AdditionalDataValueRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.dto.ErrorResponse;
import com.smit.integrationhubmiddlelayer.dto.InterfaceCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.MandatorCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.MandatorDetailDto;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.dto.RowValueRequest;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end coverage of the Company Configuration cascade (Override / Company / Mandator / Template),
 * exercised through real HTTP calls against a real Postgres-backed application instance rather than
 * mocked repositories. This is the same resolution logic already covered in isolation by
 * ConfigurationResolutionServiceTest, verified here through the full controller -> service ->
 * repository -> database stack.
 */
class ConfigurationResolutionIntegrationTest extends AbstractIntegrationTest
{
    @Test
    void resolvedConfiguration_walksThroughTemplateMandatorCompanyOverride_inPriorityOrder()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long mandatorId = createMandator("Autohaus Rath GmbH " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long companyId = createCompany(mandatorId, "Wien Zentrale " + suffix, "DMS_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long interfaceId = createInterface("Volvo Grip Api " + suffix); //$NON-NLS-1$

        ConfigurationTemplateEntryDto marketEntry = new ConfigurationTemplateEntryDto(null, "Market", ConfigValueType.TEXT, //$NON-NLS-1$
                "AT", null, "Target market", 1); //$NON-NLS-1$ //$NON-NLS-2$
        ConfigurationTemplateEntryDto timeoutEntry = new ConfigurationTemplateEntryDto(null, "TimeoutSeconds", //$NON-NLS-1$
                ConfigValueType.NUMBER, "30", null, "Request timeout", 2); //$NON-NLS-1$ //$NON-NLS-2$
        List<ConfigurationTemplateEntryDto> createdEntries = createConfigurationTemplate(interfaceId, List.of(marketEntry, timeoutEntry));
        Long marketEntryId = entryIdByKey(createdEntries, "Market"); //$NON-NLS-1$

        Long companyConfigurationId = createCompanyConfiguration(companyId, interfaceId);

        // Nothing overridden anywhere yet -> every entry resolves at TEMPLATE level with its default value.
        CompanyConfigurationDetailDto detail = getCompanyConfiguration(companyId, companyConfigurationId);
        assertThat(detail.getEntries()).hasSize(2);
        assertThat(detail.getEntries()).allSatisfy(entry -> assertThat(entry.getSourceLevel()).isEqualTo(ConfigSourceLevel.TEMPLATE));
        assertThat(resolvedByKey(detail, "Market").getEffectiveValue()).isEqualTo("AT"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(resolvedByKey(detail, "TimeoutSeconds").getEffectiveValue()).isEqualTo("30"); //$NON-NLS-1$ //$NON-NLS-2$

        // Mandator Additional Data sets "Market" -> that entry resolves at MANDATOR level, the other entry is unchanged.
        setMandatorAdditionalData(mandatorId, "Market", "DE"); //$NON-NLS-1$ //$NON-NLS-2$
        detail = getCompanyConfiguration(companyId, companyConfigurationId);
        assertThat(resolvedByKey(detail, "Market").getEffectiveValue()).isEqualTo("DE"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(resolvedByKey(detail, "Market").getSourceLevel()).isEqualTo(ConfigSourceLevel.MANDATOR); //$NON-NLS-1$
        assertThat(resolvedByKey(detail, "TimeoutSeconds").getSourceLevel()).isEqualTo(ConfigSourceLevel.TEMPLATE); //$NON-NLS-1$

        // Company Additional Data sets the SAME key -> Company wins over Mandator.
        setCompanyAdditionalData(companyId, "Market", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
        detail = getCompanyConfiguration(companyId, companyConfigurationId);
        assertThat(resolvedByKey(detail, "Market").getEffectiveValue()).isEqualTo("CH"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(resolvedByKey(detail, "Market").getSourceLevel()).isEqualTo(ConfigSourceLevel.COMPANY); //$NON-NLS-1$

        // An explicit Override on the Company Configuration entry wins over Company, Mandator and Template.
        setConfigurationEntryOverride(companyId, companyConfigurationId, marketEntryId, "IT"); //$NON-NLS-1$
        detail = getCompanyConfiguration(companyId, companyConfigurationId);
        assertThat(resolvedByKey(detail, "Market").getEffectiveValue()).isEqualTo("IT"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(resolvedByKey(detail, "Market").getSourceLevel()).isEqualTo(ConfigSourceLevel.OVERRIDE); //$NON-NLS-1$

        // Regression case: deleting the Override falls back to the NEXT level down (Company), not all the way to Template.
        deleteConfigurationEntryOverride(companyId, companyConfigurationId, marketEntryId);
        detail = getCompanyConfiguration(companyId, companyConfigurationId);
        assertThat(resolvedByKey(detail, "Market").getEffectiveValue()).isEqualTo("CH"); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(resolvedByKey(detail, "Market").getSourceLevel()).isEqualTo(ConfigSourceLevel.COMPANY); //$NON-NLS-1$
    }

    @Test
    void createCompanyConfiguration_returns409WithErrorEnvelope_whenOneAlreadyExistsForSameCompanyAndInterface()
    {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long mandatorId = createMandator("Autohaus Rath GmbH " + suffix, "MD_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long companyId = createCompany(mandatorId, "Wien Zentrale " + suffix, "DMS_" + suffix); //$NON-NLS-1$ //$NON-NLS-2$
        Long interfaceId = createInterface("Volvo Grip Api " + suffix); //$NON-NLS-1$
        createConfigurationTemplate(interfaceId, List.of(
                new ConfigurationTemplateEntryDto(null, "Market", ConfigValueType.TEXT, "AT", null, "Target market", 1))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        createCompanyConfiguration(companyId, interfaceId);

        CompanyConfigurationCreateRequest request = new CompanyConfigurationCreateRequest();
        request.setInterfaceId(interfaceId);
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/companies/" + companyId + "/configurations", request, ErrorResponse.class); //$NON-NLS-1$ //$NON-NLS-2$

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError().getCode()).isEqualTo("DUPLICATE_CONFIGURATION"); //$NON-NLS-1$
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

    private List<ConfigurationTemplateEntryDto> createConfigurationTemplate(Long interfaceId, List<ConfigurationTemplateEntryDto> entries)
    {
        ConfigurationTemplateUpsertRequest request = new ConfigurationTemplateUpsertRequest();
        request.setEntries(entries);
        ResponseEntity<ConfigurationTemplateDto> response = restTemplate.postForEntity(
                "/interfaces/" + interfaceId + "/configuration-template", request, ConfigurationTemplateDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getEntries();
    }

    private Long createCompanyConfiguration(Long companyId, Long interfaceId)
    {
        CompanyConfigurationCreateRequest request = new CompanyConfigurationCreateRequest();
        request.setInterfaceId(interfaceId);
        ResponseEntity<CompanyConfigurationDetailDto> response = restTemplate.postForEntity(
                "/companies/" + companyId + "/configurations", request, CompanyConfigurationDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private CompanyConfigurationDetailDto getCompanyConfiguration(Long companyId, Long companyConfigurationId)
    {
        ResponseEntity<CompanyConfigurationDetailDto> response = restTemplate.getForEntity(
                "/companies/" + companyId + "/configurations/" + companyConfigurationId, CompanyConfigurationDetailDto.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private void setMandatorAdditionalData(Long mandatorId, String key, String value)
    {
        AdditionalDataValueRequest request = new AdditionalDataValueRequest();
        request.setValue(value);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/mandators/" + mandatorId + "/additional-data/" + key, HttpMethod.PUT, new HttpEntity<>(request), Void.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void setCompanyAdditionalData(Long companyId, String key, String value)
    {
        AdditionalDataValueRequest request = new AdditionalDataValueRequest();
        request.setValue(value);
        ResponseEntity<Void> response = restTemplate.exchange(
                "/companies/" + companyId + "/additional-data/" + key, HttpMethod.PUT, new HttpEntity<>(request), Void.class); //$NON-NLS-1$ //$NON-NLS-2$
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void setConfigurationEntryOverride(Long companyId, Long companyConfigurationId, Long templateEntryId, String value)
    {
        RowValueRequest request = new RowValueRequest();
        request.setValue(value);
        ResponseEntity<ResolvedConfigEntryDto> response = restTemplate.exchange(
                "/companies/" + companyId + "/configurations/" + companyConfigurationId + "/entries/" + templateEntryId, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                HttpMethod.PUT, new HttpEntity<>(request), ResolvedConfigEntryDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private void deleteConfigurationEntryOverride(Long companyId, Long companyConfigurationId, Long templateEntryId)
    {
        ResponseEntity<ResolvedConfigEntryDto> response = restTemplate.exchange(
                "/companies/" + companyId + "/configurations/" + companyConfigurationId + "/entries/" + templateEntryId, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                HttpMethod.DELETE, HttpEntity.EMPTY, ResolvedConfigEntryDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static Long entryIdByKey(List<ConfigurationTemplateEntryDto> entries, String key)
    {
        return entries.stream().filter(entry -> entry.getKey().equals(key)).findFirst().orElseThrow().getId();
    }

    private static ResolvedConfigEntryDto resolvedByKey(CompanyConfigurationDetailDto detail, String key)
    {
        return detail.getEntries().stream().filter(entry -> entry.getKey().equals(key)).findFirst().orElseThrow();
    }
}
