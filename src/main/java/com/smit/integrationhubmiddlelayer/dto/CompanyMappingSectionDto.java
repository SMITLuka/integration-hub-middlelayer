package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * A Mapping Template section's rows, resolved to their effective values for one Company Mapping.
 */
@Data
@AllArgsConstructor
public class CompanyMappingSectionDto
{
    private Long sectionId;
    private String name;
    private List<ResolvedMappingRowDto> rows;
}
