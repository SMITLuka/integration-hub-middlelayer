package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One field-mapping row (Descriptor / Third Party Value) within a Mapping Template Section.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingTemplateRowDto
{
    private Long id;

    @NotBlank
    private String descriptor;

    private String thirdPartyValue;
    private Integer sortOrder;
}
