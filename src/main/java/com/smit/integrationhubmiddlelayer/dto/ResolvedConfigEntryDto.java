package com.smit.integrationhubmiddlelayer.dto;

import com.smit.integrationhubmiddlelayer.entity.ConfigValueType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A Configuration Template entry's effective value for one Company Configuration,
 * with its resolution source in the Template/Mandator/Company/Override cascade.
 */
@Data
@AllArgsConstructor
public class ResolvedConfigEntryDto
{
    private Long templateEntryId;
    private String key;
    private ConfigValueType type;
    private String description;
    private String effectiveValue;
    private ConfigSourceLevel sourceLevel;
    private Long overrideId;
}
