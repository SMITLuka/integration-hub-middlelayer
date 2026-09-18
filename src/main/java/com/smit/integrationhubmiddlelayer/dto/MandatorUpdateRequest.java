package com.smit.integrationhubmiddlelayer.dto;

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
    private String customer;
    private String externalMandatorId;
    private String country;
    private String locale;
}
