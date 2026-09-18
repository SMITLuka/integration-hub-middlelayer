package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * Request body for creating or fully replacing an Interface's Mapping Template.
 */
@Data
public class MappingTemplateUpsertRequest
{
    @Valid
    private List<MappingTemplateSectionDto> sections;
}
