package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to delete a Configuration Template that still has Company Configurations instantiated from it.
 */
public class ConfigurationTemplateInUseException extends ConflictException
{
    public ConfigurationTemplateInUseException(Long interfaceId)
    {
        super("CONFIGURATION_TEMPLATE_IN_USE", "Configuration template is still in use by one or more companies: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
