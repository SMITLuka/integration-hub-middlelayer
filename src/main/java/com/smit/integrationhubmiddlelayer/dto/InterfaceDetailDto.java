package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Full detail shape for the Interface "View Details" screen.
 */
@Data
@AllArgsConstructor
public class InterfaceDetailDto
{
    private Long id;
    private UUID uuid;
    private String name;
    private String description;
    private List<AdditionalDataEntryDto> additionalData;
    private boolean hasMappingTemplate;
    private boolean hasConfigurationTemplate;
    private InterfaceUsagesDto usages;
}
