package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A single key-value Additional Data entry, with no inheritance (Mandator/Interface level).
 */
@Data
@AllArgsConstructor
public class AdditionalDataEntryDto
{
    private String key;
    private String value;
}
