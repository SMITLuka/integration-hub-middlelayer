package com.smit.integrationhubmiddlelayer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for updating an Interface.
 */
@Data
public class InterfaceUpdateRequest
{
    @NotBlank
    private String name;

    private String dmsToMiddlewareUrl;
    private String oemToMiddlewareUrl;
    private String middlewareToOemUrl;
}
