package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.AdditionalDataEntryDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.InterfaceDetailDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceSummaryDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceUpdateRequest;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.exception.AdditionalDataKeyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateInterfaceException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceInUseException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * CRUD and Additional Data management for Interfaces.
 */
@Service
@Transactional
public class InterfaceService
{
    private static final Logger log = LoggerFactory.getLogger(InterfaceService.class);

    private final InterfaceRepository interfaceRepository;
    private final MappingTemplateRepository mappingTemplateRepository;
    private final ConfigurationTemplateRepository configurationTemplateRepository;
    private final CompanyMappingRepository companyMappingRepository;
    private final CompanyConfigurationRepository companyConfigurationRepository;
    private final InterfaceUsageService interfaceUsageService;

    public InterfaceService(InterfaceRepository interfaceRepository, MappingTemplateRepository mappingTemplateRepository,
            ConfigurationTemplateRepository configurationTemplateRepository, CompanyMappingRepository companyMappingRepository,
            CompanyConfigurationRepository companyConfigurationRepository, InterfaceUsageService interfaceUsageService)
    {
        this.interfaceRepository = interfaceRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
        this.configurationTemplateRepository = configurationTemplateRepository;
        this.companyMappingRepository = companyMappingRepository;
        this.companyConfigurationRepository = companyConfigurationRepository;
        this.interfaceUsageService = interfaceUsageService;
    }

    @Transactional(readOnly = true)
    public Page<InterfaceSummaryDto> list(String search, Pageable pageable)
    {
        Page<Interface> page = StringUtils.hasText(search)
                ? interfaceRepository.findByNameContainingIgnoreCase(search, pageable)
                : interfaceRepository.findAll(pageable);
        return page.map(this::toSummaryDto);
    }

    public InterfaceDetailDto create(InterfaceCreateRequest request)
    {
        if (interfaceRepository.findByName(request.getName()).isPresent())
        {
            throw new DuplicateInterfaceException(request.getName());
        }

        Interface interfaceEntity = Interface.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Interface saved = interfaceRepository.save(interfaceEntity);
        log.info("Created interface id={}", saved.getId()); //$NON-NLS-1$
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public InterfaceDetailDto getDetail(Long interfaceId)
    {
        return toDetailDto(findOrThrow(interfaceId));
    }

    public InterfaceDetailDto update(Long interfaceId, InterfaceUpdateRequest request)
    {
        Interface interfaceEntity = findOrThrow(interfaceId);
        interfaceEntity.setName(request.getName());
        interfaceEntity.setDescription(request.getDescription());
        log.info("Updated interface id={}", interfaceId); //$NON-NLS-1$
        return toDetailDto(interfaceEntity);
    }

    public void delete(Long interfaceId)
    {
        Interface interfaceEntity = findOrThrow(interfaceId);
        if (companyMappingRepository.existsByInterfaceEntityId(interfaceId)
                || companyConfigurationRepository.existsByInterfaceEntityId(interfaceId))
        {
            throw new InterfaceInUseException(interfaceId);
        }
        interfaceRepository.delete(interfaceEntity);
        log.info("Deleted interface id={}", interfaceId); //$NON-NLS-1$
    }

    @Transactional(readOnly = true)
    public List<AdditionalDataEntryDto> getAdditionalData(Long interfaceId)
    {
        return findOrThrow(interfaceId).getAdditionalData().entrySet().stream()
                .map(entry -> new AdditionalDataEntryDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    public AdditionalDataEntryDto setAdditionalData(Long interfaceId, String key, String value)
    {
        Interface interfaceEntity = findOrThrow(interfaceId);
        interfaceEntity.getAdditionalData().put(key, value);
        log.info("Set interface additional data id={} key={}", interfaceId, key); //$NON-NLS-1$
        return new AdditionalDataEntryDto(key, value);
    }

    public void deleteAdditionalData(Long interfaceId, String key)
    {
        Interface interfaceEntity = findOrThrow(interfaceId);
        if (interfaceEntity.getAdditionalData().remove(key) == null)
        {
            throw new AdditionalDataKeyNotFoundException(key);
        }
        log.info("Deleted interface additional data id={} key={}", interfaceId, key); //$NON-NLS-1$
    }

    Interface findOrThrow(Long interfaceId)
    {
        return interfaceRepository.findById(interfaceId).orElseThrow(() -> new InterfaceNotFoundException(interfaceId));
    }

    private InterfaceSummaryDto toSummaryDto(Interface interfaceEntity)
    {
        return new InterfaceSummaryDto(interfaceEntity.getId(), interfaceEntity.getUuid(), interfaceEntity.getName(),
                mappingTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isPresent(),
                configurationTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isPresent());
    }

    private InterfaceDetailDto toDetailDto(Interface interfaceEntity)
    {
        List<AdditionalDataEntryDto> additionalData = interfaceEntity.getAdditionalData().entrySet().stream()
                .map(entry -> new AdditionalDataEntryDto(entry.getKey(), entry.getValue()))
                .toList();
        return new InterfaceDetailDto(interfaceEntity.getId(), interfaceEntity.getUuid(), interfaceEntity.getName(),
                interfaceEntity.getDescription(), additionalData,
                mappingTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isPresent(),
                configurationTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isPresent(),
                interfaceUsageService.getUsages(interfaceEntity.getId()));
    }
}
