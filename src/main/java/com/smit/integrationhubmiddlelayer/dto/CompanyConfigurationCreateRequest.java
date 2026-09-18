package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for instantiating an Interface's Configuration Template on a Company.
 */
@Data
public class CompanyConfigurationCreateRequest
{
    @NotNull
    private Long interfaceId;
}
