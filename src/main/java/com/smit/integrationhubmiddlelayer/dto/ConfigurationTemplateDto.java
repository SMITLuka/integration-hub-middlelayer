package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full shape of an Interface's Configuration Template, as returned to the client.
 */
@Data
@AllArgsConstructor
public class ConfigurationTemplateDto
{
    private Long id;
    private List<ConfigurationTemplateEntryDto> entries;
}
