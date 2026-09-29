package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full detail shape for the Company "View Details" screen.
 */
@Data
@AllArgsConstructor
public class CompanyDetailDto
{
    private Long id;
    private Long mandatorId;
    private String mandatorName;
    private String name;
    private String dmsCompanyId;
    private String location;
    private String address;
    private String countryCode;
    private String defaultLocale;
    private List<CompanyMappingSummaryDto> mappings;
    private List<CompanyConfigurationSummaryDto> configurations;
}
