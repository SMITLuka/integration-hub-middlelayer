package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Error detail embedded inside an ErrorResponse.
 */
@Data
@AllArgsConstructor
public class ErrorDetail
{
    private String code;
    private String description;
    private List<FieldErrorDetail> fieldErrors;

    public ErrorDetail(String code, String description)
    {
        this(code, description, null);
    }
}
