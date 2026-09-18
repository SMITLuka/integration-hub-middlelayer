package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for creating a Mandator.
 */
@Data
public class MandatorCreateRequest
{
    @NotBlank
    private String name;

    private String system;
    private String customer;
    private String externalMandatorId;
    private String country;
    private String locale;
}
