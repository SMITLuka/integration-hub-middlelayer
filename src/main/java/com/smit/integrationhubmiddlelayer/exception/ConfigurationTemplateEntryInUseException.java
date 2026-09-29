package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when saving a Configuration Template would remove an entry that a Company Configuration still overrides.
 */
public class ConfigurationTemplateEntryInUseException extends ConflictException
{
    public ConfigurationTemplateEntryInUseException(String key)
    {
        super("CONFIGURATION_TEMPLATE_ENTRY_IN_USE", //$NON-NLS-1$
                "Configuration entry '" + key + "' cannot be removed: it is still overridden by one or more companies"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
