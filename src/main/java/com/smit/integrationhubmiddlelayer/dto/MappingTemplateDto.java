package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full shape of an Interface's Mapping Template, as returned to the client.
 */
@Data
@AllArgsConstructor
public class MappingTemplateDto
{
    private Long id;
    private List<MappingTemplateSectionDto> sections;
}
