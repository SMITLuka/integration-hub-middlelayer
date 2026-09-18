package com.smit.integrationhubmiddlelayer.exception;

/**
 * Base type for exceptions that mean a request conflicts with the current state
 * of a resource (duplicates, in-use dependents). Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public abstract class ConflictException extends RuntimeException
{
    private final String errorCode;

    protected ConflictException(String errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return errorCode;
    }
}
