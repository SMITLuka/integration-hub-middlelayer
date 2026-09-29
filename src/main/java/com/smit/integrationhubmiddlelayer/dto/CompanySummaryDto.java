package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Row shape for the Companies table nested inside a Mandator detail screen.
 */
@Data
@AllArgsConstructor
public class CompanySummaryDto
{
    private Long id;
    private UUID uuid;
    private String name;
    private String dmsCompanyId;
    private String location;
    private String countryCode;
}
