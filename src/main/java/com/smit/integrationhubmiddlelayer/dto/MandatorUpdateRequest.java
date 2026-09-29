package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for updating a Mandator's own fields (Additional Data is managed separately).
 */
@Data
public class MandatorUpdateRequest
{
    @NotBlank
    private String name;

    private String system;
    private String personalIdentificationNumber;
    private String externalMandatorId;
    private String hostUrl;

    @Min(1)
    @Max(65535)
    private Integer port;

    private String country;
    private String locale;
}
