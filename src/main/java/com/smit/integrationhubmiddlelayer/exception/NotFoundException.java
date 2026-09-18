package com.smit.integrationhubmiddlelayer.exception;

/**
 * Base type for exceptions that mean a requested resource does not exist.
 * Mapped to HTTP 404 by GlobalExceptionHandler.
 */
public abstract class NotFoundException extends RuntimeException
{
    private final String errorCode;

    protected NotFoundException(String errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return errorCode;
    }
}
