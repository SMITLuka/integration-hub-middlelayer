package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Row shape for the Mandators list screen.
 */
@Data
@AllArgsConstructor
public class MandatorSummaryDto
{
    private Long id;
    private UUID uuid;
    private String name;
    private String system;
    private String personalIdentificationNumber;
    private String externalMandatorId;
    private String hostUrl;
    private Integer port;
    private String country;
    private String locale;
    private int companyCount;
}
