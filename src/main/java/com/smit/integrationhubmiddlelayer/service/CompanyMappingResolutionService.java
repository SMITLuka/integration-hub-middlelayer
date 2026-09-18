package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.CompanyMappingSectionDto;
import com.smit.integrationhubmiddlelayer.dto.MappingSourceLevel;
import com.smit.integrationhubmiddlelayer.dto.ResolvedMappingRowDto;
import com.smit.integrationhubmiddlelayer.entity.CompanyMapping;
import com.smit.integrationhubmiddlelayer.entity.CompanyMappingValue;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplate;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateRow;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateSection;
import com.smit.integrationhubmiddlelayer.exception.CompanyMappingNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.MappingRowOverrideNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.MappingTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingValueRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves a Company Mapping's effective row values through the 2-level cascade:
 * an explicit Override on this Company Mapping, else the Mapping Template row's own Third Party Value.
 */
@Service
@Transactional
public class CompanyMappingResolutionService
{
    private static final Logger log = LoggerFactory.getLogger(CompanyMappingResolutionService.class);

    private final CompanyMappingRepository companyMappingRepository;
    private final MappingTemplateRepository mappingTemplateRepository;
    private final CompanyMappingValueRepository valueRepository;

    public CompanyMappingResolutionService(CompanyMappingRepository companyMappingRepository,
            MappingTemplateRepository mappingTemplateRepository, CompanyMappingValueRepository valueRepository)
    {
        this.companyMappingRepository = companyMappingRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
        this.valueRepository = valueRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyMappingSectionDto> resolveEffectiveMapping(Long companyMappingId)
    {
        CompanyMapping companyMapping = findCompanyMappingOrThrow(companyMappingId);
        MappingTemplate mappingTemplate = findTemplateOrThrow(companyMapping.getInterfaceEntity().getId());

        Map<Long, CompanyMappingValue> valuesByRowId = valueRepository.findByCompanyMappingId(companyMappingId).stream()
                .collect(Collectors.toMap(value -> value.getTemplateRow().getId(), Function.identity()));

        return mappingTemplate.getSections().stream()
                .map(section -> toSectionDto(section, valuesByRowId))
                .toList();
    }

    public ResolvedMappingRowDto setRowOverride(Long companyMappingId, Long templateRowId, String value)
    {
        CompanyMapping companyMapping = findCompanyMappingOrThrow(companyMappingId);
        MappingTemplateRow row = findRowOrThrow(companyMapping, templateRowId);

        CompanyMappingValue mappingValue = valueRepository.findByCompanyMappingIdAndTemplateRowId(companyMappingId, templateRowId)
                .orElseGet(() -> CompanyMappingValue.builder().companyMapping(companyMapping).templateRow(row).build());
        mappingValue.setValue(value);

        CompanyMappingValue saved = valueRepository.save(mappingValue);
        log.info("Set mapping row override companyMappingId={} rowId={}", companyMappingId, templateRowId); //$NON-NLS-1$
        return toResolved(row, saved.getValue(), MappingSourceLevel.OVERRIDE, saved.getId());
    }

    public ResolvedMappingRowDto deleteRowOverride(Long companyMappingId, Long templateRowId)
    {
        CompanyMappingValue mappingValue = valueRepository
                .findByCompanyMappingIdAndTemplateRowId(companyMappingId, templateRowId)
                .orElseThrow(() -> new MappingRowOverrideNotFoundException(companyMappingId, templateRowId));

        MappingTemplateRow row = mappingValue.getTemplateRow();
        valueRepository.delete(mappingValue);
        log.info("Deleted mapping row override companyMappingId={} rowId={}", companyMappingId, templateRowId); //$NON-NLS-1$

        return toResolved(row, row.getThirdPartyValue(), MappingSourceLevel.TEMPLATE, null);
    }

    private CompanyMappingSectionDto toSectionDto(MappingTemplateSection section, Map<Long, CompanyMappingValue> valuesByRowId)
    {
        List<ResolvedMappingRowDto> rows = section.getRows().stream()
                .map(row -> resolveRow(row, valuesByRowId.get(row.getId())))
                .toList();
        return new CompanyMappingSectionDto(section.getId(), section.getName(), rows);
    }

    private ResolvedMappingRowDto resolveRow(MappingTemplateRow row, CompanyMappingValue value)
    {
        if (value != null)
        {
            return toResolved(row, value.getValue(), MappingSourceLevel.OVERRIDE, value.getId());
        }
        return toResolved(row, row.getThirdPartyValue(), MappingSourceLevel.TEMPLATE, null);
    }

    private ResolvedMappingRowDto toResolved(MappingTemplateRow row, String value, MappingSourceLevel level, Long valueId)
    {
        return new ResolvedMappingRowDto(row.getId(), row.getDescriptor(), value, level, valueId);
    }

    private MappingTemplateRow findRowOrThrow(CompanyMapping companyMapping, Long templateRowId)
    {
        MappingTemplate mappingTemplate = findTemplateOrThrow(companyMapping.getInterfaceEntity().getId());
        return mappingTemplate.getSections().stream()
                .flatMap(section -> section.getRows().stream())
                .filter(row -> row.getId().equals(templateRowId))
                .findFirst()
                .orElseThrow(() -> new MappingRowOverrideNotFoundException(companyMapping.getId(), templateRowId));
    }

    private MappingTemplate findTemplateOrThrow(Long interfaceId)
    {
        return mappingTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseThrow(() -> new MappingTemplateNotFoundException(interfaceId));
    }

    private CompanyMapping findCompanyMappingOrThrow(Long companyMappingId)
    {
        return companyMappingRepository.findById(companyMappingId)
                .orElseThrow(() -> new CompanyMappingNotFoundException(companyMappingId));
    }
}
