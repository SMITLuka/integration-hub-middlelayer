package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.CompanyMappingCreateRequest;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingDetailDto;
import com.smit.integrationhubmiddlelayer.dto.CompanyMappingSummaryDto;
import com.smit.integrationhubmiddlelayer.entity.Company;
import com.smit.integrationhubmiddlelayer.entity.CompanyMapping;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.exception.CompanyMappingNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.CompanyNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.DuplicateCompanyMappingException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceHasNoMappingTemplateException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creation, listing and deletion of Company Mapping instances (the effective row values
 * are resolved separately by CompanyMappingResolutionService).
 */
@Service
@Transactional
public class CompanyMappingService
{
    private static final Logger log = LoggerFactory.getLogger(CompanyMappingService.class);

    private final CompanyRepository companyRepository;
    private final InterfaceRepository interfaceRepository;
    private final MappingTemplateRepository mappingTemplateRepository;
    private final CompanyMappingRepository companyMappingRepository;
    private final CompanyMappingResolutionService resolutionService;

    public CompanyMappingService(CompanyRepository companyRepository, InterfaceRepository interfaceRepository,
            MappingTemplateRepository mappingTemplateRepository, CompanyMappingRepository companyMappingRepository,
            CompanyMappingResolutionService resolutionService)
    {
        this.companyRepository = companyRepository;
        this.interfaceRepository = interfaceRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
        this.companyMappingRepository = companyMappingRepository;
        this.resolutionService = resolutionService;
    }

    @Transactional(readOnly = true)
    public List<CompanyMappingSummaryDto> list(Long companyId)
    {
        return companyMappingRepository.findByCompanyId(companyId).stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public CompanyMappingDetailDto create(Long companyId, CompanyMappingCreateRequest request)
    {
        Company company = companyRepository.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
        Interface interfaceEntity = interfaceRepository.findById(request.getInterfaceId())
                .orElseThrow(() -> new InterfaceNotFoundException(request.getInterfaceId()));

        if (mappingTemplateRepository.findByInterfaceEntityId(interfaceEntity.getId()).isEmpty())
        {
            throw new InterfaceHasNoMappingTemplateException(interfaceEntity.getId());
        }
        if (companyMappingRepository.findByCompanyIdAndInterfaceEntityId(companyId, interfaceEntity.getId()).isPresent())
        {
            throw new DuplicateCompanyMappingException(companyId, interfaceEntity.getId());
        }

        CompanyMapping companyMapping = CompanyMapping.builder().company(company).interfaceEntity(interfaceEntity).build();
        CompanyMapping saved = companyMappingRepository.save(companyMapping);
        log.info("Created company mapping id={} companyId={} interfaceId={}", saved.getId(), companyId, interfaceEntity.getId()); //$NON-NLS-1$
        return toDetailDto(saved);
    }

    @Transactional(readOnly = true)
    public CompanyMappingDetailDto getDetail(Long companyId, Long companyMappingId)
    {
        return toDetailDto(findOrThrow(companyId, companyMappingId));
    }

    public void delete(Long companyId, Long companyMappingId)
    {
        CompanyMapping companyMapping = findOrThrow(companyId, companyMappingId);
        companyMappingRepository.delete(companyMapping);
        log.info("Deleted company mapping id={}", companyMappingId); //$NON-NLS-1$
    }

    private CompanyMapping findOrThrow(Long companyId, Long companyMappingId)
    {
        CompanyMapping companyMapping = companyMappingRepository.findById(companyMappingId)
                .orElseThrow(() -> new CompanyMappingNotFoundException(companyMappingId));
        if (!companyMapping.getCompany().getId().equals(companyId))
        {
            throw new CompanyMappingNotFoundException(companyMappingId);
        }
        return companyMapping;
    }

    private CompanyMappingSummaryDto toSummaryDto(CompanyMapping companyMapping)
    {
        return new CompanyMappingSummaryDto(companyMapping.getId(), companyMapping.getInterfaceEntity().getId(),
                companyMapping.getInterfaceEntity().getName(), companyMapping.getUpdatedAt());
    }

    private CompanyMappingDetailDto toDetailDto(CompanyMapping companyMapping)
    {
        return new CompanyMappingDetailDto(companyMapping.getId(), companyMapping.getInterfaceEntity().getId(),
                companyMapping.getInterfaceEntity().getName(), resolutionService.resolveEffectiveMapping(companyMapping.getId()));
    }
}
