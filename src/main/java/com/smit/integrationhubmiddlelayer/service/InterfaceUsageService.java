package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.InterfaceUsageDto;
import com.smit.integrationhubmiddlelayer.dto.InterfaceUsagesDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyConfiguration;
import com.smit.integrationhubmiddlelayer.entity.CompanyMapping;
import com.smit.integrationhubmiddlelayer.repository.CompanyConfigurationRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Builds the "which Mandator/Company pairs use this Interface" lists shown on the Interface detail screen.
 */
@Service
@Transactional(readOnly = true)
public class InterfaceUsageService
{
    private final CompanyMappingRepository companyMappingRepository;
    private final CompanyConfigurationRepository companyConfigurationRepository;

    public InterfaceUsageService(CompanyMappingRepository companyMappingRepository,
            CompanyConfigurationRepository companyConfigurationRepository)
    {
        this.companyMappingRepository = companyMappingRepository;
        this.companyConfigurationRepository = companyConfigurationRepository;
    }

    public InterfaceUsagesDto getUsages(Long interfaceId)
    {
        return new InterfaceUsagesDto(
                companyMappingRepository.findByInterfaceEntityId(interfaceId).stream().map(this::toUsageDto).toList(),
                companyConfigurationRepository.findByInterfaceEntityId(interfaceId).stream().map(this::toUsageDto).toList());
    }

    private InterfaceUsageDto toUsageDto(CompanyMapping companyMapping)
    {
        return buildUsageDto(companyMapping.getCompany(), companyMapping.getUpdatedAt());
    }

    private InterfaceUsageDto toUsageDto(CompanyConfiguration companyConfiguration)
    {
        return buildUsageDto(companyConfiguration.getCompany(), companyConfiguration.getUpdatedAt());
    }

    private InterfaceUsageDto buildUsageDto(Company company, Instant updatedAt)
    {
        return new InterfaceUsageDto(company.getMandator().getId(), company.getMandator().getName(),
                company.getId(), company.getName(), updatedAt);
    }
}
