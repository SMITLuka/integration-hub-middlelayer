package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A single resolved Company Additional Data entry, indicating whether the effective
 * value is the Company's own override or inherited from its Mandator.
 */
@Data
@AllArgsConstructor
public class ResolvedAdditionalDataEntryDto
{
    private String key;
    private String value;
    private AdditionalDataSourceLevel sourceLevel;
}
