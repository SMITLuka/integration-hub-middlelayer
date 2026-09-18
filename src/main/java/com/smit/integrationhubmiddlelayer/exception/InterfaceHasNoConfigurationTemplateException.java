package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to instantiate a Configuration on a Company for an Interface that has no Configuration Template.
 */
public class InterfaceHasNoConfigurationTemplateException extends BadRequestException
{
    public InterfaceHasNoConfigurationTemplateException(Long interfaceId)
    {
        super("INTERFACE_HAS_NO_CONFIGURATION_TEMPLATE", "Interface has no configuration template defined: " + interfaceId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
