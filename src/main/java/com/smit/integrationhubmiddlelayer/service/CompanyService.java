package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.CompanyCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyUpdateRequest;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.Mandator;
import com.smit.integrationhubmiddlelayer.exception.CompanyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateCompanyException;
import com.smit.integrationhubmiddlelayer.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CompanyService
{
    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final MandatorService mandatorService;
    private final CompanyMappingService companyMappingService;
    private final CompanyConfigurationService companyConfigurationService;

    public CompanyService(CompanyRepository companyRepository, MandatorService mandatorService,
            CompanyMappingService companyMappingService, CompanyConfigurationService companyConfigurationService)
    {
        this.companyRepository = companyRepository;
        this.mandatorService = mandatorService;
        this.companyMappingService = companyMappingService;
        this.companyConfigurationService = companyConfigurationService;
    }

    public CompanyDetailDto create(Long mandatorId, CompanyCreateRequest request)
    {
        Mandator mandator = mandatorService.findOrThrow(mandatorId);

        if (StringUtils.hasText(request.getDmsCompanyId())
                && companyRepository.findByMandatorIdAndDmsCompanyId(mandatorId, request.getDmsCompanyId()).isPresent())
        {
            throw new DuplicateCompanyException(mandatorId, request.getDmsCompanyId());
        }

        Company company = Company.builder()
                .mandator(mandator)
                .name(request.getName())
                .dmsCompanyId(request.getDmsCompanyId())
                .location(request.getLocation())
                .countryCode(request.getCountryCode())
                .customerNumber(request.getCustomerNumber())
                .defaultLocale(request.getDefaultLocale())
                .build();

        Company saved = companyRepository.save(company);
        log.info("Created company id={} under mandatorId={}", saved.getId(), mandatorId); //$NON-NLS-1$
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public CompanyDetailDto getDetail(Long companyId)
    {
        return toDetailDto(findOrThrow(companyId));
    }

    public CompanyDetailDto update(Long companyId, CompanyUpdateRequest request)
    {
        Company company = findOrThrow(companyId);
        company.setName(request.getName());
        company.setDmsCompanyId(request.getDmsCompanyId());
        company.setLocation(request.getLocation());
        company.setCountryCode(request.getCountryCode());
        company.setCustomerNumber(request.getCustomerNumber());
        company.setDefaultLocale(request.getDefaultLocale());
        log.info("Updated company id={}", companyId); //$NON-NLS-1$
        return toDetailDto(company);
    }

    public void delete(Long companyId)
    {
        Company company = findOrThrow(companyId);
        companyRepository.delete(company);
        log.info("Deleted company id={}", companyId); //$NON-NLS-1$
    }

    Company findOrThrow(Long companyId)
    {
        return companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private CompanyDetailDto toDetailDto(Company company)
    {
        return new CompanyDetailDto(company.getId(), company.getMandator().getId(), company.getMandator().getName(),
                company.getName(), company.getDmsCompanyId(), company.getLocation(), company.getCountryCode(),
                company.getCustomerNumber(), company.getDefaultLocale(), companyMappingService.list(company.getId()),
                companyConfigurationService.list(company.getId()));
    }
}
