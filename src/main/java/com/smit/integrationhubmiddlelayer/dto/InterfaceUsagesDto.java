package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * The Mandator/Company pairs currently using an Interface, split by Mapping vs Configuration usage.
 */
@Data
@AllArgsConstructor
public class InterfaceUsagesDto
{
    private List<InterfaceUsageDto> mappingUsages;
    private List<InterfaceUsageDto> configurationUsages;
}
