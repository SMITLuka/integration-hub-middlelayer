package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A Mapping Template row's effective value for one Company Mapping, with its resolution source.
 */
@Data
@AllArgsConstructor
public class ResolvedMappingRowDto
{
    private Long templateRowId;
    private String descriptor;
    private String effectiveValue;
    private MappingSourceLevel sourceLevel;
    private Long valueId;
}
