package com.smit.integrationhubmiddlelayer.dto;

import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One configuration key definition (Key/Type/Default Value/Expression/Description) within a Configuration Template.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationTemplateEntryDto
{
    private Long id;

    @NotBlank
    private String key;

    @NotNull
    private ConfigValueType type;

    private String defaultValue;
    private String expression;
    private String description;
    private Integer sortOrder;
}
