package com.smit.integrationhubmiddlelayer.exception;

/**
 * Base type for exceptions that mean the request itself is invalid given the
 * current state of a referenced resource. Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public abstract class BadRequestException extends RuntimeException
{
    private final String errorCode;

    protected BadRequestException(String errorCode, String message)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode()
    {
        return errorCode;
    }
}
