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

        String dmsCompanyId = normalizeDmsCompanyId(request.getDmsCompanyId());
        if (dmsCompanyId != null && companyRepository.findByMandatorIdAndDmsCompanyId(mandatorId, dmsCompanyId).isPresent())
        {
            throw new DuplicateCompanyException(mandatorId, dmsCompanyId);
        }

        Company company = Company.builder()
                .mandator(mandator)
                .name(request.getName())
                .dmsCompanyId(dmsCompanyId)
                .location(request.getLocation())
                .address(request.getAddress())
                .countryCode(request.getCountryCode())
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
        Long mandatorId = company.getMandator().getId();
        String dmsCompanyId = normalizeDmsCompanyId(request.getDmsCompanyId());
        if (dmsCompanyId != null)
        {
            companyRepository.findByMandatorIdAndDmsCompanyId(mandatorId, dmsCompanyId)
                    .filter(existing -> !existing.getId().equals(companyId))
                    .ifPresent(existing ->
                    {
                        throw new DuplicateCompanyException(mandatorId, dmsCompanyId);
                    });
        }

        company.setName(request.getName());
        company.setDmsCompanyId(dmsCompanyId);
        company.setLocation(request.getLocation());
        company.setAddress(request.getAddress());
        company.setCountryCode(request.getCountryCode());
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

    /**
     * (mandator_id, dms_company_id) is UNIQUE: PostgreSQL allows many NULLs there but only one
     * empty string per Mandator, so a blank ID (the UI sends "" for an empty field) must be stored as NULL.
     */
    private static String normalizeDmsCompanyId(String dmsCompanyId)
    {
        return StringUtils.hasText(dmsCompanyId) ? dmsCompanyId : null;
    }

    private CompanyDetailDto toDetailDto(Company company)
    {
        return new CompanyDetailDto(company.getId(), company.getUuid(), company.getMandator().getId(), company.getMandator().getName(),
                company.getName(), company.getDmsCompanyId(), company.getLocation(), company.getAddress(), company.getCountryCode(),
                company.getDefaultLocale(), companyMappingService.list(company.getId()),
                companyConfigurationService.list(company.getId()));
    }
}
