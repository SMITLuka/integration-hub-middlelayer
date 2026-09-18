package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for creating an Interface.
 */
@Data
public class InterfaceCreateRequest
{
    @NotBlank
    private String name;

    private String dmsToMiddlewareUrl;
    private String oemToMiddlewareUrl;
    private String middlewareToOemUrl;
}
