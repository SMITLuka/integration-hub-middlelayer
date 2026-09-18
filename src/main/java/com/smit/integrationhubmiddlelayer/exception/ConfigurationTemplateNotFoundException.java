package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when an Interface has no Configuration Template defined.
 */
public class ConfigurationTemplateNotFoundException extends NotFoundException
{
    public ConfigurationTemplateNotFoundException(Long interfaceId)
    {
        super("CONFIGURATION_TEMPLATE_NOT_FOUND", "No configuration template defined for interface: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
