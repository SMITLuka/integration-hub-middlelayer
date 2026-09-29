package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for creating a Company under a Mandator.
 */
@Data
public class CompanyCreateRequest
{
    @NotBlank
    private String name;

    private String dmsCompanyId;
    private String location;
    private String address;
    private String countryCode;
    private String customerNumber;
    private String defaultLocale;
}
