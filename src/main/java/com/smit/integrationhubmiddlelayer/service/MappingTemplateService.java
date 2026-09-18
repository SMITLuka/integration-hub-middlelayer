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
import com.smit.integrationhubmiddlelayer.repository.CompanyMappingRepository;
import com.smit.integrationhubmiddlelayer.repository.InterfaceRepository;
import com.smit.integrationhubmiddlelayer.repository.MappingTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    public MappingTemplateService(InterfaceRepository interfaceRepository, MappingTemplateRepository mappingTemplateRepository,
            CompanyMappingRepository companyMappingRepository)
    {
        this.interfaceRepository = interfaceRepository;
        this.mappingTemplateRepository = mappingTemplateRepository;
        this.companyMappingRepository = companyMappingRepository;
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

    private void applySections(MappingTemplate mappingTemplate, List<MappingTemplateSectionDto> sectionDtos)
    {
        List<MappingTemplateSectionDto> sourceSections = sectionDtos != null ? sectionDtos : List.of();
        List<MappingTemplateSection> sections = new ArrayList<>();
        for (MappingTemplateSectionDto sectionDto : sourceSections)
        {
            MappingTemplateSection section = MappingTemplateSection.builder()
                    .mappingTemplate(mappingTemplate)
                    .name(sectionDto.getName())
                    .sortOrder(sectionDto.getSortOrder())
                    .build();

            List<MappingTemplateRowDto> sourceRows = sectionDto.getRows() != null ? sectionDto.getRows() : List.of();
            List<MappingTemplateRow> rows = new ArrayList<>();
            for (MappingTemplateRowDto rowDto : sourceRows)
            {
                rows.add(MappingTemplateRow.builder()
                        .section(section)
                        .descriptor(rowDto.getDescriptor())
                        .thirdPartyValue(rowDto.getThirdPartyValue())
                        .sortOrder(rowDto.getSortOrder())
                        .build());
            }
            section.getRows().clear();
            section.getRows().addAll(rows);
            sections.add(section);
        }
        mappingTemplate.getSections().clear();
        mappingTemplate.getSections().addAll(sections);
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
