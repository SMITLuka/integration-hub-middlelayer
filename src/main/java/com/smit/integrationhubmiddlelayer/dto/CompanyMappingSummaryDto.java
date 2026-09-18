package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Row shape for the Mappings list on a Company detail screen.
 */
@Data
@AllArgsConstructor
public class CompanyMappingSummaryDto
{
    private Long id;
    private Long interfaceId;
    private String interfaceName;
    private Instant updatedAt;
}
