package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

/**
 * Request body for creating or fully replacing an Interface's Configuration Template.
 */
@Data
public class ConfigurationTemplateUpsertRequest
{
    @Valid
    private List<ConfigurationTemplateEntryDto> entries;
}
