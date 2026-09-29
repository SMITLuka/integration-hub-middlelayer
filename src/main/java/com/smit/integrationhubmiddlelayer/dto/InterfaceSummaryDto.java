package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Row shape for the Interfaces list screen.
 */
@Data
@AllArgsConstructor
public class InterfaceSummaryDto
{
    private Long id;
    private UUID uuid;
    private String name;
    private boolean hasMappingTemplate;
    private boolean hasConfigurationTemplate;
}
