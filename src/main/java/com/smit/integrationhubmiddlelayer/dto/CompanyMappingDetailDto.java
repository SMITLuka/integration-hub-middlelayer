package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full detail shape for one Company's Mapping instance of an Interface.
 */
@Data
@AllArgsConstructor
public class CompanyMappingDetailDto
{
    private Long id;
    private Long interfaceId;
    private String interfaceName;
    private List<CompanyMappingSectionDto> sections;
}
