package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A named group of mapping rows within a Mapping Template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingTemplateSectionDto
{
    private Long id;

    @NotBlank
    private String name;

    private Integer sortOrder;

    @Valid
    private List<MappingTemplateRowDto> rows;
}
