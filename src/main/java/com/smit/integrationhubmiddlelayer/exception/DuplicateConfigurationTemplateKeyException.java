package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when a Configuration Template upsert request contains the same key more than once.
 */
public class DuplicateConfigurationTemplateKeyException extends BadRequestException
{
    public DuplicateConfigurationTemplateKeyException(String key)
    {
        super("DUPLICATE_CONFIGURATION_TEMPLATE_KEY", "Configuration template key used more than once: " + key); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
