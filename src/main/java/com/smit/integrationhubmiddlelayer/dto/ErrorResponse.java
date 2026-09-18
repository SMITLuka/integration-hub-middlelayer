package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Standard error response envelope returned by this middlelayer: {"error": {"code": ..., "description": ...}}.
 */
@Data
@AllArgsConstructor
public class ErrorResponse
{
    private ErrorDetail error;

    /**
     * Factory method for building an ErrorResponse from a code and description.
     *
     * @param code        short error code (e.g. MANDATOR_NOT_FOUND)
     * @param description human-readable error description
     * @return constructed ErrorResponse
     */
    public static ErrorResponse of(String code, String description)
    {
        return new ErrorResponse(new ErrorDetail(code, description));
    }

    /**
     * Factory method for building a validation ErrorResponse with per-field failures.
     *
     * @param code        short error code
     * @param description human-readable error description
     * @param fieldErrors per-field validation failures
     * @return constructed ErrorResponse
     */
    public static ErrorResponse of(String code, String description, List<FieldErrorDetail> fieldErrors)
    {
        return new ErrorResponse(new ErrorDetail(code, description, fieldErrors));
    }
}
