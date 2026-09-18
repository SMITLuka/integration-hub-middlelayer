package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A single field-level validation failure, embedded in an ErrorDetail's fieldErrors list.
 */
@Data
@AllArgsConstructor
public class FieldErrorDetail
{
    private String field;
    private String message;
}
