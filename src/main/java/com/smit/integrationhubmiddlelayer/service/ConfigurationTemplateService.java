package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateEntryDto;
import com.smit.integrationhubmiddlelayer.dto.ConfigurationTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplate;
import com.smit.integrationhubmiddlelayer.entity.ConfigurationTemplateEntry;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationTemplateEntryInUseException;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationTemplateInUseException;
import com.smit.integrationhubmiddlelayer.exception.ConfigurationTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationTemplateException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationTemplateKeyException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationOverrideRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create/read/full-replace/delete of an Interface's Configuration Template (a schema of config key entries).
 */
@Service
@Transactional
public class ConfigurationTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(ConfigurationTemplateService.class);

    private final InterfaceRepository interfaceRepository;
    private final ConfigurationTemplateRepository configurationTemplateRepository;
    private final CompanyConfigurationRepository companyConfigurationRepository;
    private final CompanyConfigurationOverrideRepository companyConfigurationOverrideRepository;

    public ConfigurationTemplateService(InterfaceRepository interfaceRepository,
            ConfigurationTemplateRepository configurationTemplateRepository,
            CompanyConfigurationRepository companyConfigurationRepository,
            CompanyConfigurationOverrideRepository companyConfigurationOverrideRepository)
    {
        this.interfaceRepository = interfaceRepository;
        this.configurationTemplateRepository = configurationTemplateRepository;
        this.companyConfigurationRepository = companyConfigurationRepository;
        this.companyConfigurationOverrideRepository = companyConfigurationOverrideRepository;
    }

    @Transactional(readOnly = true)
    public ConfigurationTemplateDto get(Long interfaceId)
    {
        return toDto(findOrThrow(interfaceId));
    }

    public ConfigurationTemplateDto create(Long interfaceId, ConfigurationTemplateUpsertRequest request)
    {
        Interface interfaceEntity = findInterfaceOrThrow(interfaceId);
        if (configurationTemplateRepository.findByInterfaceEntityId(interfaceId).isPresent())
        {
            throw new DuplicateConfigurationTemplateException(interfaceId);
        }

        validateNoDuplicateKeys(request.getEntries());
        ConfigurationTemplate configurationTemplate = ConfigurationTemplate.builder().interfaceEntity(interfaceEntity).build();
        applyEntries(configurationTemplate, request.getEntries());

        ConfigurationTemplate saved = configurationTemplateRepository.save(configurationTemplate);
        log.info("Created configuration template for interfaceId={}", interfaceId); //$NON-NLS-1$
        return toDto(saved);
    }

    public ConfigurationTemplateDto replace(Long interfaceId, ConfigurationTemplateUpsertRequest request)
    {
        Interface interfaceEntity = findInterfaceOrThrow(interfaceId);
        validateNoDuplicateKeys(request.getEntries());

        ConfigurationTemplate configurationTemplate = configurationTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseGet(() -> ConfigurationTemplate.builder().interfaceEntity(interfaceEntity).build());

        applyEntries(configurationTemplate, request.getEntries());

        ConfigurationTemplate saved = configurationTemplateRepository.save(configurationTemplate);
        log.info("Replaced configuration template for interfaceId={}", interfaceId); //$NON-NLS-1$
        return toDto(saved);
    }

    public void delete(Long interfaceId)
    {
        ConfigurationTemplate configurationTemplate = findOrThrow(interfaceId);
        if (companyConfigurationRepository.existsByInterfaceEntityId(interfaceId))
        {
            throw new ConfigurationTemplateInUseException(interfaceId);
        }
        configurationTemplateRepository.delete(configurationTemplate);
        log.info("Deleted configuration template for interfaceId={}", interfaceId); //$NON-NLS-1$
    }

    private void validateNoDuplicateKeys(List<ConfigurationTemplateEntryDto> entries)
    {
        Set<String> seenKeys = new HashSet<>();
        for (ConfigurationTemplateEntryDto entry : entries != null ? entries : List.<ConfigurationTemplateEntryDto>of())
        {
            if (!seenKeys.add(entry.getKey()))
            {
                throw new DuplicateConfigurationTemplateKeyException(entry.getKey());
            }
        }
    }

    /**
     * Merges the requested entries into the template instead of recreating them: existing entries
     * (matched by id) are updated in place so their ids stay stable, because Company Configuration
     * overrides reference them by id. Removals and updates are flushed before new entries are
     * inserted; Hibernate would otherwise insert first and hit the UNIQUE (template, key) constraint
     * when a new entry reuses a key that was just removed or renamed.
     */
    private void applyEntries(ConfigurationTemplate configurationTemplate, List<ConfigurationTemplateEntryDto> entryDtos)
    {
        List<ConfigurationTemplateEntryDto> source = entryDtos != null ? entryDtos : List.of();
        Map<Long, ConfigurationTemplateEntry> existingById = new HashMap<>();
        configurationTemplate.getEntries().forEach(entry -> existingById.put(entry.getId(), entry));

        Set<Long> requestedIds = new HashSet<>();
        source.forEach(entryDto -> requestedIds.add(entryDto.getId()));
        for (ConfigurationTemplateEntry entry : configurationTemplate.getEntries())
        {
            if (!requestedIds.contains(entry.getId()) && companyConfigurationOverrideRepository.existsByTemplateEntryId(entry.getId()))
            {
                throw new ConfigurationTemplateEntryInUseException(entry.getKey());
            }
        }
        configurationTemplate.getEntries().removeIf(entry -> !requestedIds.contains(entry.getId()));
        flushIfPersisted(configurationTemplate);

        List<ConfigurationTemplateEntryDto> newEntryDtos = new ArrayList<>();
        for (ConfigurationTemplateEntryDto entryDto : source)
        {
            ConfigurationTemplateEntry existing = entryDto.getId() != null ? existingById.get(entryDto.getId()) : null;
            if (existing != null)
            {
                copyFields(entryDto, existing);
            }
            else
            {
                newEntryDtos.add(entryDto);
            }
        }
        flushIfPersisted(configurationTemplate);

        for (ConfigurationTemplateEntryDto entryDto : newEntryDtos)
        {
            ConfigurationTemplateEntry entry = ConfigurationTemplateEntry.builder().configurationTemplate(configurationTemplate).build();
            copyFields(entryDto, entry);
            configurationTemplate.getEntries().add(entry);
        }
    }

    private void flushIfPersisted(ConfigurationTemplate configurationTemplate)
    {
        if (configurationTemplate.getId() != null)
        {
            configurationTemplateRepository.flush();
        }
    }

    private static void copyFields(ConfigurationTemplateEntryDto source, ConfigurationTemplateEntry target)
    {
        target.setKey(source.getKey());
        target.setType(source.getType());
        target.setDefaultValue(source.getDefaultValue());
        target.setExpression(source.getExpression());
        target.setDescription(source.getDescription());
        target.setSortOrder(source.getSortOrder());
    }

    private ConfigurationTemplate findOrThrow(Long interfaceId)
    {
        return configurationTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseThrow(() -> new ConfigurationTemplateNotFoundException(interfaceId));
    }

    private Interface findInterfaceOrThrow(Long interfaceId)
    {
        return interfaceRepository.findById(interfaceId).orElseThrow(() -> new InterfaceNotFoundException(interfaceId));
    }

    private ConfigurationTemplateDto toDto(ConfigurationTemplate configurationTemplate)
    {
        List<ConfigurationTemplateEntryDto> entries = configurationTemplate.getEntries().stream()
                .map(entry -> new ConfigurationTemplateEntryDto(entry.getId(), entry.getKey(), entry.getType(),
                        entry.getDefaultValue(), entry.getExpression(), entry.getDescription(), entry.getSortOrder()))
                .toList();
        return new ConfigurationTemplateDto(configurationTemplate.getId(), entries);
    }
}
