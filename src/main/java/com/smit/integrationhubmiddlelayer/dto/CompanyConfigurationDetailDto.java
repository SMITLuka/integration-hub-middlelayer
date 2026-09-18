package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full detail shape for one Company's Configuration instance of an Interface.
 */
@Data
@AllArgsConstructor
public class CompanyConfigurationDetailDto
{
    private Long id;
    private Long interfaceId;
    private String interfaceName;
    private List<ResolvedConfigEntryDto> entries;
}
