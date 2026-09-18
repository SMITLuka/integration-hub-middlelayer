package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for instantiating an Interface's Mapping Template on a Company.
 */
@Data
public class CompanyMappingCreateRequest
{
    @NotNull
    private Long interfaceId;
}
