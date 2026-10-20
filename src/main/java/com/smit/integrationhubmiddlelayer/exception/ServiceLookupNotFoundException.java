package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown by the service API when no Mandator, Company, Interface or Company Configuration
 * matches the lookup values a calling service sent. Mapped to HTTP 404.
 */
public class ServiceLookupNotFoundException extends NotFoundException
{
    public ServiceLookupNotFoundException(String errorCode, String message)
    {
        super(errorCode, message);
    }
}
