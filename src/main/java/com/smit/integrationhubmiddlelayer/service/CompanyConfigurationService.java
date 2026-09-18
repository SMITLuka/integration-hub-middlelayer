package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyConfigurationSummaryDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.exception.CompanyConfigurationNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.CompanyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateConfigurationException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceHasNoConfigurationTemplateException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyRepository;
import com.smit.integrationhubmiddlelayer.repository.ConfigurationTemplateRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creation, listing and deletion of Company Configuration instances (effective key values
 * are resolved separately by ConfigurationResolutionService).
 */
@Service
@Transactional
public class CompanyConfigurationService
{
    private static final Logger log = LoggerFactory.getLogger(CompanyConfigurationService.class);

    private final CompanyRepository companyRepository;
    private final InterfaceRepository interfaceRepository;
    private final ConfigurationTemplateRepository configurationTemplateRepository;
    private final CompanyConfigurationRepository companyConfigurationRepository;
    private final ConfigurationResolutionService resolutionService;

    public CompanyConfigurationService(CompanyRepository companyRepository, InterfaceRepository interfaceRepository,
            ConfigurationTemplateRepository configurationTemplateRepository,
            CompanyConfigurationRepository companyConfigurationRepository, ConfigurationResolutionService resolutionService)
    {
        this.companyRepository = companyRepository;
        this.interfaceRepository = interfaceRepository;
        this.configurationTemplateRepository = configurationTemplateRepository;
        this.companyConfigurationRepository = companyConfigurationRepository;
        this.resolutionService = resolutionService;
    }

    @Transactional(readOnly = true)
    public List<CompanyConfigurationSummaryDto> list(Long companyId)
    {
        return companyConfigurationRepository.findByCompanyId(companyId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public CompanyConfigurationDetailDto create(Long companyId, CompanyConfigurationCreateRequest request)
    {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        Interface interfaceEntity = interfaceRepository.findById(request.getInterfaceId())
                .orElseThrow(() -> new InterfaceNotFoundException(request.getInterfaceId()));

        if (configurationTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isEmpty())
        {
            throw new InterfaceHasNoConfigurationTemplateException(interfaceEntity.getId());
        }
        if (companyConfigurationRepository.findByCompanyIdAndInterfaceEntityId(companyId, interfaceEntity.getId()).isPresent())
        {
            throw new DuplicateConfigurationException(companyId, interfaceEntity.getId());
        }

        CompanyConfiguration companyConfiguration = CompanyConfiguration.builder()
                .company(company)
                .interfaceEntity(interfaceEntity)
                .build();
        CompanyConfiguration saved = companyConfigurationRepository.save(companyConfiguration);
        log.info("Created company configuration id={} companyId={} interfaceId={}", saved.getId(), companyId, //$NON-NLS-1$
                interfaceEntity.getId());
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public CompanyConfigurationDetailDto getDetail(Long companyId, Long companyConfigurationId)
    {
        return toDetailDto(findOrThrow(companyId, companyConfigurationId));
    }

    public void delete(Long companyId, Long companyConfigurationId)
    {
        CompanyConfiguration companyConfiguration = findOrThrow(companyId, companyConfigurationId);
        companyConfigurationRepository.delete(companyConfiguration);
        log.info("Deleted company configuration id={}", companyConfigurationId); //$NON-NLS-1$
    }

    private CompanyConfiguration findOrThrow(Long companyId, Long companyConfigurationId)
    {
        CompanyConfiguration companyConfiguration = companyConfigurationRepository.findById(companyConfigurationId)
                .orElseThrow(() -> new CompanyConfigurationNotFoundException(companyConfigurationId));
        if (!companyConfiguration.getCompany().getId().equals(companyId))
        {
            throw new CompanyConfigurationNotFoundException(companyConfigurationId);
        }
        return companyConfiguration;
    }

    private CompanyConfigurationSummaryDto toSummaryDto(CompanyConfiguration companyConfiguration)
    {
        return new CompanyConfigurationSummaryDto(companyConfiguration.getId(), companyConfiguration.getInterfaceEntity().getId(),
                companyConfiguration.getInterfaceEntity().getName(), companyConfiguration.getUpdatedAt());
    }

    private CompanyConfigurationDetailDto toDetailDto(CompanyConfiguration companyConfiguration)
    {
        return new CompanyConfigurationDetailDto(companyConfiguration.getId(), companyConfiguration.getInterfaceEntity().getId(),
                companyConfiguration.getInterfaceEntity().getName(),
                resolutionService.resolveEffectiveConfig(companyConfiguration.getId()));
    }
}
