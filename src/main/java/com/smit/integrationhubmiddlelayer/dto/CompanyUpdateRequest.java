package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for updating a Company's own fields (Additional Data is managed separately).
 */
@Data
public class CompanyUpdateRequest
{
    @NotBlank
    private String name;

    private String dmsCompanyId;
    private String location;
    private String countryCode;
    private String customerNumber;
    private String defaultLocale;
}
