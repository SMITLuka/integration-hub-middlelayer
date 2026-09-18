package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full detail shape for the Interface "View Details" screen.
 */
@Data
@AllArgsConstructor
public class InterfaceDetailDto
{
    private Long id;
    private String name;
    private String dmsToMiddlewareUrl;
    private String oemToMiddlewareUrl;
    private String middlewareToOemUrl;
    private List<AdditionalDataEntryDto> additionalData;
    private boolean hasMappingTemplate;
    private boolean hasConfigurationTemplate;
    private InterfaceUsagesDto usages;
}
