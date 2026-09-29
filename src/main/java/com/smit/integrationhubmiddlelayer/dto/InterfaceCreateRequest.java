package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating an Interface.
 */
@Data
public class InterfaceCreateRequest
{
    @NotBlank
    private String name;

    @Size(max = 2000)
    private String description;
}
