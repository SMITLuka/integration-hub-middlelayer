package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Counts shown on the dashboard's summary cards.
 */
@Data
@AllArgsConstructor
public class DashboardSummaryDto
{
    private long configuredMandators;
    private long configuredInterfaces;
    private long configuredMappingTemplates;
}
