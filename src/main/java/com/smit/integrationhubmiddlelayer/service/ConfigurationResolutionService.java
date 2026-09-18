package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.ConfigSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedConfigEntryDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfigurationOverride;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplate;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplateEntry;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.CompanyConfigurationNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationOverrideNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationOverrideRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a Company Configuration's effective values through the 4-level cascade:
 * an explicit Override on this Company Configuration, then the Company's own Additional Data,
 * then the parent Mandator's Additional Data, then the Configuration Template's default/expression.
 */
@Service
@Transactional
public class ConfigurationResolutionService
{
    private static final Logger log = LoggerFactory.getLogger(ConfigurationResolutionService.class);

    private final CompanyConfigurationRepository companyConfigurationRepository;
    private final ConfigurationTemplateRepository configurationTemplateRepository;
    private final CompanyConfigurationOverrideRepository overrideRepository;

    public ConfigurationResolutionService(CompanyConfigurationRepository companyConfigurationRepository,
            ConfigurationTemplateRepository configurationTemplateRepository,
            CompanyConfigurationOverrideRepository overrideRepository)
    {
        this.companyConfigurationRepository = companyConfigurationRepository;
        this.configurationTemplateRepository = configurationTemplateRepository;
        this.overrideRepository = overrideRepository;
    }

    @Transactional(readOnly = true)
    public List<ResolvedConfigEntryDto> resolveEffectiveConfig(Long companyConfigurationId)
    {
        CompanyConfiguration companyConfiguration = findCompanyConfigurationOrThrow(companyConfigurationId);
        Company company = companyConfiguration.getCompany();
        Mandator mandator = company.getMandator();
        Long interfaceId = companyConfiguration.getInterfaceEntity().getId();

        ConfigurationTemplate configurationTemplate = configurationTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseThrow(() -> new ConfigurationTemplateNotFoundException(interfaceId));

        Map<Long, CompanyConfigurationOverride> overridesByEntryId = overrideRepository
                .findByCompanyConfigurationId(companyConfigurationId).stream()
                .collect(Collectors.toMap(override -> override.getTemplateEntry().getId(), Function.identity()));

        Map<String, String> companyData = company.getAdditionalData();
        Map<String, String> mandatorData = mandator.getAdditionalData();

        return configurationTemplate.getEntries().stream()
                .map(entry -> resolveEntry(entry, overridesByEntryId.get(entry.getId()), companyData, mandatorData))
                .toList();
    }

    public ResolvedConfigEntryDto setOverride(Long companyConfigurationId, Long templateEntryId, String value)
    {
        CompanyConfiguration companyConfiguration = findCompanyConfigurationOrThrow(companyConfigurationId);
        ConfigurationTemplateEntry entry = findEntryOrThrow(companyConfiguration, templateEntryId);

        CompanyConfigurationOverride override = overrideRepository
                .findByCompanyConfigurationIdAndTemplateEntryId(companyConfigurationId, templateEntryId)
                .orElseGet(() -> CompanyConfigurationOverride.builder()
                        .companyConfiguration(companyConfiguration)
                        .templateEntry(entry)
                        .build());
        override.setValue(value);

        CompanyConfigurationOverride saved = overrideRepository.save(override);
        log.info("Set configuration override companyConfigurationId={} key={}", companyConfigurationId, entry.getKey()); //$NON-NLS-1$
        return toResolved(entry, saved.getValue(), ConfigSourceLevel.OVERRIDE, saved.getId());
    }

    public ResolvedConfigEntryDto deleteOverride(Long companyConfigurationId, Long templateEntryId)
    {
        CompanyConfigurationOverride override = overrideRepository
                .findByCompanyConfigurationIdAndTemplateEntryId(companyConfigurationId, templateEntryId)
                .orElseThrow(() -> new ConfigurationOverrideNotFoundException(companyConfigurationId, templateEntryId));

        ConfigurationTemplateEntry entry = override.getTemplateEntry();
        Company company = override.getCompanyConfiguration().getCompany();
        Mandator mandator = company.getMandator();

        overrideRepository.delete(override);
        log.info("Deleted configuration override companyConfigurationId={} key={}", companyConfigurationId, entry.getKey()); //$NON-NLS-1$

        return resolveEntry(entry, null, company.getAdditionalData(), mandator.getAdditionalData());
    }

    private ResolvedConfigEntryDto resolveEntry(ConfigurationTemplateEntry entry, CompanyConfigurationOverride override,
            Map<String, String> companyData, Map<String, String> mandatorData)
    {
        if (override != null)
        {
            return toResolved(entry, override.getValue(), ConfigSourceLevel.OVERRIDE, override.getId());
        }
        if (companyData.containsKey(entry.getKey()))
        {
            return toResolved(entry, companyData.get(entry.getKey()), ConfigSourceLevel.COMPANY, null);
        }
        if (mandatorData.containsKey(entry.getKey()))
        {
            return toResolved(entry, mandatorData.get(entry.getKey()), ConfigSourceLevel.MANDATOR, null);
        }
        String templateValue = entry.getExpression() != null && !entry.getExpression().isBlank()
                ? entry.getExpression()
                : entry.getDefaultValue();
        return toResolved(entry, templateValue, ConfigSourceLevel.TEMPLATE, null);
    }

    private ResolvedConfigEntryDto toResolved(ConfigurationTemplateEntry entry, String value, ConfigSourceLevel level, Long overrideId)
    {
        return new ResolvedConfigEntryDto(entry.getId(), entry.getKey(), entry.getType(), entry.getDescription(), value, level, overrideId);
    }

    private ConfigurationTemplateEntry findEntryOrThrow(CompanyConfiguration companyConfiguration, Long templateEntryId)
    {
        Long interfaceId = companyConfiguration.getInterfaceEntity().getId();
        ConfigurationTemplate configurationTemplate = configurationTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseThrow(() -> new ConfigurationTemplateNotFoundException(interfaceId));
        return configurationTemplate.getEntries().stream()
                .filter(entry -> entry.getId().equals(templateEntryId))
                .findFirst()
                .orElseThrow(() -> new ConfigurationOverrideNotFoundException(companyConfiguration.getId(), templateEntryId));
    }

    private CompanyConfiguration findCompanyConfigurationOrThrow(Long companyConfigurationId)
    {
        return companyConfigurationRepository.findById(companyConfigurationId)
                .orElseThrow(() -> new CompanyConfigurationNotFoundException(companyConfigurationId));
    }
}
