package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Full detail shape for the Mandator "View Details" screen.
 */
@Data
@AllArgsConstructor
public class MandatorDetailDto
{
    private Long id;
    private String name;
    private String system;
    private String customer;
    private String externalMandatorId;
    private String country;
    private String locale;
    private List<AdditionalDataEntryDto> additionalData;
    private List<CompanySummaryDto> companies;
}
