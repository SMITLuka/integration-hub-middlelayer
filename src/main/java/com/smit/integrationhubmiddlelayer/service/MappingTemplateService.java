package com.smit.integrationhubmiddlelayer.service;

import com.smit.integrationhubmiddlelayer.dto.MappingTemplateDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateRowDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateSectionDto;
import com.smit.integrationhubmiddlelayer.dto.MappingTemplateUpsertRequest;
import com.smit.integrationhubmiddlelayer.entity.Interface;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplate;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateRow;
import com.smit.integrationhubmiddlelayer.entity.MappingTemplateSection;
import com.smit.integrationhubmiddlelayer.exception.DuplicateMappingTemplateException;
import com.smit.integrationhubmiddlelayer.exception.InterfaceNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.MappingTemplateInUseException;
import com.smit.integrationhubmiddlelayer.exception.MappingTemplateNotFoundException;
import com.smit.integrationhubmiddlelayer.exception.MappingTemplateRowInUseException;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingValueRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
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
 * Create/read/full-replace/delete of an Interface's Mapping Template (Sections of Rows).
 */
@Service
@Transactional
public class MappingTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(MappingTemplateService.class);

    private final InterfaceRepository interfaceRepository;
    private final MappingTemplateRepository mappingTemplateRepository;
    private final CompanyMappingRepository companyMappingRepository;
    private final CompanyMappingValueRepository companyMappingValueRepository;

    public MappingTemplateService(InterfaceRepository interfaceRepository, MappingTemplateRepository mappingTemplateRepository,
            CompanyMappingRepository companyMappingRepository, CompanyMappingValueRepository companyMappingValueRepository)
    {
        this.interfaceRepository = interfaceRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
        this.companyMappingRepository = companyMappingRepository;
        this.companyMappingValueRepository = companyMappingValueRepository;
    }

    @Transactional(readOnly = true)
    public MappingTemplateDto get(Long interfaceId)
    {
        return toDto(findOrThrow(interfaceId));
    }

    public MappingTemplateDto create(Long interfaceId, MappingTemplateUpsertRequest request)
    {
        Interface interfaceEntity = findInterfaceOrThrow(interfaceId);
        if (mappingTemplateRepository.findByInterfaceEntityId(interfaceId).isPresent())
        {
            throw new DuplicateMappingTemplateException(interfaceId);
        }

        MappingTemplate mappingTemplate = MappingTemplate.builder().interfaceEntity(interfaceEntity).build();
        applySections(mappingTemplate, request.getSections());

        MappingTemplate saved = mappingTemplateRepository.save(mappingTemplate);
        log.info("Created mapping template for interfaceId={}", interfaceId); //$NON-NLS-1$
        return toDto(saved);
    }

    public MappingTemplateDto replace(Long interfaceId, MappingTemplateUpsertRequest request)
    {
        Interface interfaceEntity = findInterfaceOrThrow(interfaceId);
        MappingTemplate mappingTemplate = mappingTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseGet(() -> MappingTemplate.builder().interfaceEntity(interfaceEntity).build());

        applySections(mappingTemplate, request.getSections());

        MappingTemplate saved = mappingTemplateRepository.save(mappingTemplate);
        log.info("Replaced mapping template for interfaceId={}", interfaceId); //$NON-NLS-1$
        return toDto(saved);
    }

    public void delete(Long interfaceId)
    {
        MappingTemplate mappingTemplate = findOrThrow(interfaceId);
        if (companyMappingRepository.existsByInterfaceEntityId(interfaceId))
        {
            throw new MappingTemplateInUseException(interfaceId);
        }
        mappingTemplateRepository.delete(mappingTemplate);
        log.info("Deleted mapping template for interfaceId={}", interfaceId); //$NON-NLS-1$
    }

    /**
     * Merges the requested sections and rows into the template instead of recreating them: existing
     * sections and rows (matched by id) are updated in place so their ids stay stable, because
     * Company Mapping values reference template rows by id. Items missing from the request are removed.
     */
    private void applySections(MappingTemplate mappingTemplate, List<MappingTemplateSectionDto> sectionDtos)
    {
        List<MappingTemplateSectionDto> sourceSections = sectionDtos != null ? sectionDtos : List.of();
        rejectRemovalOfRowsInUse(mappingTemplate, sourceSections);
        Map<Long, MappingTemplateSection> existingById = new HashMap<>();
        mappingTemplate.getSections().forEach(section -> existingById.put(section.getId(), section));

        List<MappingTemplateSection> sections = new ArrayList<>();
        for (MappingTemplateSectionDto sectionDto : sourceSections)
        {
            MappingTemplateSection section = sectionDto.getId() != null ? existingById.get(sectionDto.getId()) : null;
            if (section == null)
            {
                section = MappingTemplateSection.builder().mappingTemplate(mappingTemplate).build();
            }
            section.setName(sectionDto.getName());
            section.setSortOrder(sectionDto.getSortOrder());
            applyRows(section, sectionDto.getRows());
            sections.add(section);
        }
        replaceContents(mappingTemplate.getSections(), sections);
    }

    /**
     * Checked before anything is changed: a row that Company Mappings still hold values for must not be
     * removed, either directly or together with its section, or the database rejects the save with a 500.
     */
    private void rejectRemovalOfRowsInUse(MappingTemplate mappingTemplate, List<MappingTemplateSectionDto> sourceSections)
    {
        Map<Long, Set<Long>> requestedRowIdsBySection = new HashMap<>();
        for (MappingTemplateSectionDto sectionDto : sourceSections)
        {
            Set<Long> rowIds = new HashSet<>();
            (sectionDto.getRows() != null ? sectionDto.getRows() : List.<MappingTemplateRowDto>of()).forEach(rowDto -> rowIds.add(rowDto.getId()));
            requestedRowIdsBySection.put(sectionDto.getId(), rowIds);
        }
        for (MappingTemplateSection section : mappingTemplate.getSections())
        {
            Set<Long> keptRowIds = requestedRowIdsBySection.getOrDefault(section.getId(), Set.of());
            for (MappingTemplateRow row : section.getRows())
            {
                if (!keptRowIds.contains(row.getId()) && companyMappingValueRepository.existsByTemplateRowId(row.getId()))
                {
                    throw new MappingTemplateRowInUseException(row.getDescriptor());
                }
            }
        }
    }

    private void applyRows(MappingTemplateSection section, List<MappingTemplateRowDto> rowDtos)
    {
        List<MappingTemplateRowDto> sourceRows = rowDtos != null ? rowDtos : List.of();
        Map<Long, MappingTemplateRow> existingById = new HashMap<>();
        section.getRows().forEach(row -> existingById.put(row.getId(), row));

        List<MappingTemplateRow> rows = new ArrayList<>();
        for (MappingTemplateRowDto rowDto : sourceRows)
        {
            MappingTemplateRow row = rowDto.getId() != null ? existingById.get(rowDto.getId()) : null;
            if (row == null)
            {
                row = MappingTemplateRow.builder().section(section).build();
            }
            row.setDescriptor(rowDto.getDescriptor());
            row.setThirdPartyValue(rowDto.getThirdPartyValue());
            row.setSortOrder(rowDto.getSortOrder());
            rows.add(row);
        }
        replaceContents(section.getRows(), rows);
    }

    /**
     * Keeps the same (Hibernate-managed) collection instance, which orphanRemoval requires; replacing
     * the collection object itself would fail. Kept elements are the same instances, so they are
     * updated rather than deleted and re-inserted.
     */
    private static <T> void replaceContents(List<T> target, List<T> newContents)
    {
        target.clear();
        target.addAll(newContents);
    }

    private MappingTemplate findOrThrow(Long interfaceId)
    {
        return mappingTemplateRepository.findByInterfaceEntityId(interfaceId)
                .orElseThrow(() -> new MappingTemplateNotFoundException(interfaceId));
    }

    private Interface findInterfaceOrThrow(Long interfaceId)
    {
        return interfaceRepository.findById(interfaceId).orElseThrow(() -> new InterfaceNotFoundException(interfaceId));
    }

    private MappingTemplateDto toDto(MappingTemplate mappingTemplate)
    {
        List<MappingTemplateSectionDto> sections = mappingTemplate.getSections().stream()
                .map(this::toSectionDto)
                .toList();
        return new MappingTemplateDto(mappingTemplate.getId(), sections);
    }

    private MappingTemplateSectionDto toSectionDto(MappingTemplateSection section)
    {
        List<MappingTemplateRowDto> rows = section.getRows().stream()
                .map(row -> new MappingTemplateRowDto(row.getId(), row.getDescriptor(), row.getThirdPartyValue(), row.getSortOrder()))
                .toList();
        return new MappingTemplateSectionDto(section.getId(), section.getName(), section.getSortOrder(), rows);
    }
}
