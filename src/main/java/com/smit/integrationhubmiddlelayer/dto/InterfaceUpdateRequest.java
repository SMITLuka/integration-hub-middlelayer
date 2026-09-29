package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for updating an Interface.
 */
@Data
public class InterfaceUpdateRequest
{
    @NotBlank
    private String name;

    @Size(max = 2000)
    private String description;
}
