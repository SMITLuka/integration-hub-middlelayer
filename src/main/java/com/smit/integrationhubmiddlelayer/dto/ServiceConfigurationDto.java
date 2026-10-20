package com.smit.integrationhubmiddlelayer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Everything a calling service (e.g. MIMW) needs to run one Interface for one Company:
 * identity of the Mandator and Company, the effective configuration as plain key/value pairs
 * and the effective Additional Data (Company overrides Mandator).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceConfigurationDto
{
    private MandatorInfo mandator;
    private CompanyInfo company;
    private String interfaceName;
    private Map<String, String> config;
    private Map<String, String> additionalData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MandatorInfo
    {
        private String uuid;
        private String name;
        private String externalMandatorId;
        private String system;
        private String personalIdentificationNumber;
        private String hostUrl;
        private Integer port;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompanyInfo
    {
        private String uuid;
        private String name;
        private String dmsCompanyId;
        private String location;
        private String address;
    }
}
