package com.smit.integrationhubmiddlelayer.dto;

/**
 * Where a Company Configuration entry's effective value currently comes from,
 * in cascade priority order (OVERRIDE wins, TEMPLATE is the fallback default).
 */
public enum ConfigSourceLevel
{
    OVERRIDE,
    COMPANY,
    MANDATOR,
    TEMPLATE
}
