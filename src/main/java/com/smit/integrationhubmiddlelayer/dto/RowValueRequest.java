package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for overriding a single Mapping row or Configuration entry's value.
 */
@Data
public class RowValueRequest
{
    @NotNull
    private String value;
}
