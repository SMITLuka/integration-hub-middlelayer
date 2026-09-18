package com.smit.integrationhubmiddlelayer.exception;

/**
 * Thrown when trying to revert a Configuration entry that has no override set at this Company Configuration.
 */
public class ConfigurationOverrideNotFoundException extends NotFoundException
{
    public ConfigurationOverrideNotFoundException(Long companyConfigurationId, Long templateEntryId)
    {
        super("CONFIGURATION_OVERRIDE_NOT_FOUND", //$NON-NLS-1$
                "No override set for entry " + templateEntryId + " on company configuration " + companyConfigurationId); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
