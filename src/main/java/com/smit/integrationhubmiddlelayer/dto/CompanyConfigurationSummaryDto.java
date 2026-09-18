package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Row shape for the Configurations list on a Company detail screen.
 */
@Data
@AllArgsConstructor
public class CompanyConfigurationSummaryDto
{
    private Long id;
    private Long interfaceId;
    private String interfaceName;
    private Instant updatedAt;
}
