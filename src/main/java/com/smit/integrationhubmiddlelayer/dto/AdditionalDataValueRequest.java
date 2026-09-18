package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for setting a single Additional Data key's value.
 */
@Data
public class AdditionalDataValueRequest
{
    @NotBlank
    private String value;
}
