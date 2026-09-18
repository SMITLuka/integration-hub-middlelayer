package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.ConfigSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfigurationOverride;
import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplate;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplateEntry;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationOverrideNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationOverrideRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies the 4-level cascade (Override / Company / Mandator / Template) in isolation,
 * the highest-value test surface in the project per the implementation plan.
 */
@ExtendWith(MockitoExtension.class)
class ConfigurationResolutionServiceTest
{
    private static final Long COMPANY_CONFIGURATION_ID = 100L;
    private static final Long INTERFACE_ID = 1L;
    private static final Long ENTRY_ID = 10L;

    @Mock
    private CompanyConfigurationRepository companyConfigurationRepository;

    @Mock
    private ConfigurationTemplateRepository configurationTemplateRepository;

    @Mock
    private CompanyConfigurationOverrideRepository overrideRepository;

    private ConfigurationResolutionService service;

    private Mandator mandator;
    private Company company;
    private CompanyConfiguration companyConfiguration;
    private ConfigurationTemplateEntry entry;
    private ConfigurationTemplate configurationTemplate;

    @BeforeEach
    void setUp()
    {
        service = new ConfigurationResolutionService(companyConfigurationRepository, configurationTemplateRepository, overrideRepository);

        mandator = Mandator.builder().id(1L).additionalData(new HashMap<>()).build();
        company = Company.builder().id(1L).mandator(mandator).additionalData(new HashMap<>()).build();

        Interface interfaceEntity = Interface.builder().id(INTERFACE_ID).name("Volvo Grip Api").build(); //$NON-NLS-1$

        companyConfiguration = CompanyConfiguration.builder()
                .id(COMPANY_CONFIGURATION_ID)
                .company(company)
                .interfaceEntity(interfaceEntity)
                .build();

        entry = ConfigurationTemplateEntry.builder()
                .id(ENTRY_ID)
                .key("Market") //$NON-NLS-1$
                .type(ConfigValueType.TEXT)
                .defaultValue("AT") //$NON-NLS-1$
                .description("Target Market") //$NON-NLS-1$
                .build();

        configurationTemplate = ConfigurationTemplate.builder()
                .interfaceEntity(interfaceEntity)
                .entries(List.of(entry))
                .build();
    }

    private void stubCompanyConfigurationAndTemplateLookup()
    {
        when(companyConfigurationRepository.findById(COMPANY_CONFIGURATION_ID)).thenReturn(Optional.of(companyConfiguration));
        when(configurationTemplateRepository.findByInterfaceEntityId(INTERFACE_ID)).thenReturn(Optional.of(configurationTemplate));
    }

    @Test
    void resolve_returnsTemplateDefault_whenNoOverrideAnywhere()
    {
        stubCompanyConfigurationAndTemplateLookup();
        when(overrideRepository.findByCompanyConfigurationId(COMPANY_CONFIGURATION_ID)).thenReturn(List.of());

        List<ResolvedConfigEntryDto> resolved = service.resolveEffectiveConfig(COMPANY_CONFIGURATION_ID);

        assertThat(resolved).hasSize(1);
        assertThat(resolved.get(0).getEffectiveValue()).isEqualTo("AT"); //$NON-NLS-1$
        assertThat(resolved.get(0).getSourceLevel()).isEqualTo(ConfigSourceLevel.TEMPLATE);
    }

    @Test
    void resolve_prefersExpressionOverDefaultValue_atTemplateLevel()
    {
        stubCompanyConfigurationAndTemplateLookup();
        entry.setExpression("concat(Market, '-X')"); //$NON-NLS-1$
        when(overrideRepository.findByCompanyConfigurationId(COMPANY_CONFIGURATION_ID)).thenReturn(List.of());

        List<ResolvedConfigEntryDto> resolved = service.resolveEffectiveConfig(COMPANY_CONFIGURATION_ID);

        assertThat(resolved.get(0).getEffectiveValue()).isEqualTo("concat(Market, '-X')"); //$NON-NLS-1$
        assertThat(resolved.get(0).getSourceLevel()).isEqualTo(ConfigSourceLevel.TEMPLATE);
    }

    @Test
    void resolve_returnsMandatorValue_whenMandatorAdditionalDataHasKey()
    {
        stubCompanyConfigurationAndTemplateLookup();
        mandator.getAdditionalData().put("Market", "DE"); //$NON-NLS-1$ //$NON-NLS-2$
        when(overrideRepository.findByCompanyConfigurationId(COMPANY_CONFIGURATION_ID)).thenReturn(List.of());

        List<ResolvedConfigEntryDto> resolved = service.resolveEffectiveConfig(COMPANY_CONFIGURATION_ID);

        assertThat(resolved.get(0).getEffectiveValue()).isEqualTo("DE"); //$NON-NLS-1$
        assertThat(resolved.get(0).getSourceLevel()).isEqualTo(ConfigSourceLevel.MANDATOR);
    }

    @Test
    void resolve_prefersCompanyOverMandator_whenBothHaveKey()
    {
        stubCompanyConfigurationAndTemplateLookup();
        mandator.getAdditionalData().put("Market", "DE"); //$NON-NLS-1$ //$NON-NLS-2$
        company.getAdditionalData().put("Market", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
        when(overrideRepository.findByCompanyConfigurationId(COMPANY_CONFIGURATION_ID)).thenReturn(List.of());

        List<ResolvedConfigEntryDto> resolved = service.resolveEffectiveConfig(COMPANY_CONFIGURATION_ID);

        assertThat(resolved.get(0).getEffectiveValue()).isEqualTo("CH"); //$NON-NLS-1$
        assertThat(resolved.get(0).getSourceLevel()).isEqualTo(ConfigSourceLevel.COMPANY);
    }

    @Test
    void resolve_prefersOverrideOverEverythingElse()
    {
        stubCompanyConfigurationAndTemplateLookup();
        mandator.getAdditionalData().put("Market", "DE"); //$NON-NLS-1$ //$NON-NLS-2$
        company.getAdditionalData().put("Market", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
        CompanyConfigurationOverride override = CompanyConfigurationOverride.builder()
                .id(500L).companyConfiguration(companyConfiguration).templateEntry(entry).value("IT").build(); //$NON-NLS-1$
        when(overrideRepository.findByCompanyConfigurationId(COMPANY_CONFIGURATION_ID)).thenReturn(List.of(override));

        List<ResolvedConfigEntryDto> resolved = service.resolveEffectiveConfig(COMPANY_CONFIGURATION_ID);

        assertThat(resolved.get(0).getEffectiveValue()).isEqualTo("IT"); //$NON-NLS-1$
        assertThat(resolved.get(0).getSourceLevel()).isEqualTo(ConfigSourceLevel.OVERRIDE);
        assertThat(resolved.get(0).getOverrideId()).isEqualTo(500L);
    }

    @Test
    void setOverride_createsNewOverride_whenNoneExistsYet()
    {
        stubCompanyConfigurationAndTemplateLookup();
        when(overrideRepository.findByCompanyConfigurationIdAndTemplateEntryId(COMPANY_CONFIGURATION_ID, ENTRY_ID))
                .thenReturn(Optional.empty());
        when(overrideRepository.save(any())).thenAnswer(invocation ->
        {
            CompanyConfigurationOverride toSave = invocation.getArgument(0);
            toSave.setId(999L);
            return toSave;
        });

        ResolvedConfigEntryDto result = service.setOverride(COMPANY_CONFIGURATION_ID, ENTRY_ID, "FR"); //$NON-NLS-1$

        assertThat(result.getEffectiveValue()).isEqualTo("FR"); //$NON-NLS-1$
        assertThat(result.getSourceLevel()).isEqualTo(ConfigSourceLevel.OVERRIDE);
        assertThat(result.getOverrideId()).isEqualTo(999L);
    }

    @Test
    void deleteOverride_revertsToNextLevelDown_afterRemoval()
    {
        company.getAdditionalData().put("Market", "CH"); //$NON-NLS-1$ //$NON-NLS-2$
        CompanyConfigurationOverride override = CompanyConfigurationOverride.builder()
                .id(500L).companyConfiguration(companyConfiguration).templateEntry(entry).value("IT").build(); //$NON-NLS-1$
        when(overrideRepository.findByCompanyConfigurationIdAndTemplateEntryId(COMPANY_CONFIGURATION_ID, ENTRY_ID))
                .thenReturn(Optional.of(override));

        ResolvedConfigEntryDto result = service.deleteOverride(COMPANY_CONFIGURATION_ID, ENTRY_ID);

        assertThat(result.getEffectiveValue()).isEqualTo("CH"); //$NON-NLS-1$
        assertThat(result.getSourceLevel()).isEqualTo(ConfigSourceLevel.COMPANY);
    }

    @Test
    void deleteOverride_throws_whenNoOverrideSetAtThisLevel()
    {
        when(overrideRepository.findByCompanyConfigurationIdAndTemplateEntryId(COMPANY_CONFIGURATION_ID, ENTRY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteOverride(COMPANY_CONFIGURATION_ID, ENTRY_ID))
                .isInstanceOf(ConfigurationOverrideNotFoundException.class);
    }
}
