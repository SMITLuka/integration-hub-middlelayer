package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Row shape for the Mandators list screen.
 */
@Data
@AllArgsConstructor
public class MandatorSummaryDto
{
    private Long id;
    private String name;
    private String system;
    private String customer;
    private String externalMandatorId;
    private String country;
    private String locale;
    private int companyCount;
}
