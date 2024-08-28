/*
 * ------------------------------------------------------------------------------
 * *******************************************************************************
 *  COPYRIGHT Ericsson  2023
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 * ******************************************************************************
 * ----------------------------------------------------------------------------
 */

package com.ericsson.oss.services.security.npam.ejb.database.availability;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_EMERGENCY_USER;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESTRICT_MAINTENANCE_USER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty;
import com.ericsson.oss.services.security.npam.ejb.config.dao.NPamConfigDpsReader;

/**
 * <p>
 * Holds the NPamConfig status.
 * </p>
 * <p>
 * This singleton bean ensures one single instance in the server with the appropriate locks on the read and write methods
 * </p>
 */
@Singleton
public class NodePamConfigStatus {

    private final Map<String, Boolean> npamConfigCache = new HashMap<>();

    @Inject
    private Logger logger;

    @Inject
    private NPamConfigDpsReader configDpsReader;

    /*
    *  Initial Setup
    *
    * */
    @PostConstruct
    public void settingInitialStatus() {
        try {
            clearAndSetDefaultValuesInCache();
            final NPamConfig nPamConfig = getNPamConfig();
            if ((nPamConfig.getPoId() != -1) && (containsNpamFlag(nPamConfig))) {
                logger.info("settingInitialStatus:: found 1 nPamConfig={}", nPamConfig);
                setPropertiesInCache(nPamConfig);
            } else {
                logger.info("setConfig:: INVALID nPamConfig={}", nPamConfig);
            }
        } catch (final Exception e) {
            logger.error("settingInitialStatus:: Exception searching NPamConfig object ={}, caching false values", e.getMessage());
            clearAndSetDefaultValuesInCache();
        } finally {
            logger.info("settingInitialStatus:: npamConfigCache={}", npamConfigCache);
        }
    }

    private NPamConfig getNPamConfig() {
        logger.debug("Retrieving NPam Configuration");
        final List<NPamConfig> nPamConfig = configDpsReader.getNPamConfigObject();
        if (nPamConfig.isEmpty()) {
            return new NPamConfig();
        }

        if (nPamConfig.size() > 1) { //should throw exception????
            logger.error("Issue in retrieving NPamConfig, found more than one object");
        }
        return nPamConfig.get(0); //we suppose to have only 1 nPamConfig
    }

    private boolean containsNpamFlag(final NPamConfig nPamConfig) {
        for (final NPamConfigProperty npamConfigProperty : nPamConfig.getnPamConfigProperties()) {
            if (NPAM_CONFIG_NPAM.equals(npamConfigProperty.getName())) {
                return true;
            }
        }
        return false;
    }

    private void setPropertiesInCache(final NPamConfig nPamConfig) {
        for (final NPamConfigProperty npamConfigProperty : nPamConfig.getnPamConfigProperties()) {
            if (NPAM_CONFIG_NPAM.equals(npamConfigProperty.getName())) {
                setNpamConfigValueInCache(npamConfigProperty.getValue());
            }
            if (NPAM_CONFIG_CBRS_DOMAIN.equals(npamConfigProperty.getName())) {
                setCbrsDomainConfigValueInCache(npamConfigProperty.getValue());
            }
            if (NPAM_CONFIG_EMERGENCY_USER.equals(npamConfigProperty.getName())
                    && NPamConfigPropertiesEnum.isKnownPropertyName(NPAM_CONFIG_EMERGENCY_USER)) {
                setEmergencyUserConfigValueInCache(npamConfigProperty.getValue());
            }
            if (NPAM_CONFIG_RESTRICT_MAINTENANCE_USER.equals(npamConfigProperty.getName())
                    && NPamConfigPropertiesEnum.isKnownPropertyName(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)) {
                setRestrictMaintenanceUserConfigValueInCache(npamConfigProperty.getValue());
            }
        }
    }

    /*
     *  API methods
     *
     * */

    /**
     * Retrieves the npam current status. Any thread can read as long as there's no write thread running
     *
     * @return npam current status
     */
    @Lock(LockType.READ)
    public boolean isEnabled() {
        logger.debug("NPAM Configuration: isEnabled: {}", npamConfigCache.get(NPAM_CONFIG_NPAM));
        return npamConfigCache.get(NPAM_CONFIG_NPAM);
    }

    /**
     * Retrieves info about cbrs_domain
     *
     * @return cbrs_domain enabled/disabled
     */
    @Lock(LockType.READ)
    public boolean isCbrsDomainEnabled() {
        logger.debug("NPAM Configuration: isCbrsDomainEnabled: {}", npamConfigCache.get(NPAM_CONFIG_CBRS_DOMAIN));
        return npamConfigCache.get(NPAM_CONFIG_CBRS_DOMAIN);
    }

    /**
     * Retrieves info about emergencyuser
     *
     * @return emergencyuser enabled/disabled
     */
    @Lock(LockType.READ)
    public boolean isEmergencyUserEnabled() {
        logger.debug("NPAM Configuration: isEmergencyUserEnabled: {}", npamConfigCache.get(NPAM_CONFIG_EMERGENCY_USER));
        return npamConfigCache.get(NPAM_CONFIG_EMERGENCY_USER);
    }

    /**
     * Retrieves info about restrictmaintenanceuser
     *
     * @return restrictmaintenanceuser enabled/disabled
     */
    @Lock(LockType.READ)
    public boolean isRestrictMaintenanceUserEnabled() {
        logger.debug("NPAM Configuration: isRestrictMaintenanceUserEnabled: {}", npamConfigCache.get(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER));
        return npamConfigCache.get(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
    }

    /**
     * Updates the npamconfig status. Concurrent access to this method is blocked.
     *
     * @param npamConfigProperties
     *            new status (enabled = true; disabled = false)
     */
    @Lock(LockType.WRITE)
    public void setConfig(final List<Map<String, String>> npamConfigProperties) {
        try {
            logger.info("setConfig:: rewrite values");
            clearAndSetDefaultValuesInCache();

            if (containsNpamFlag(npamConfigProperties)) {
                setPropertiesInCache(npamConfigProperties);
            } else {
                logger.info("setConfig:: INVALID npamConfigProperties={}", npamConfigProperties);
            }
        } finally {
            logger.info("setConfig:: npamConfigCache={}", npamConfigCache);
        }
    }

    private boolean containsNpamFlag(final List<Map<String, String>> npamConfigProperties) {
        for (final Map<String, String> npamConfigProperty : npamConfigProperties) {
            if (NPAM_CONFIG_NPAM.equals(npamConfigProperty.get(PROPERTY_NAME))) {
                return true;
            }
        }
        return false;
    }

    private void setPropertiesInCache(final List<Map<String, String>> npamConfigProperties) {
        for (final Map<String, String> npamConfigProperty : npamConfigProperties) {
            if (NPAM_CONFIG_NPAM.equals(npamConfigProperty.get(PROPERTY_NAME))) {
                setNpamConfigValueInCache(npamConfigProperty.get(PROPERTY_VALUE));
                logger.info("setConfig:: written1 {}={}", NPAM_CONFIG_NPAM, npamConfigCache.get(NPAM_CONFIG_NPAM));
            }
            if (NPAM_CONFIG_CBRS_DOMAIN.equals(npamConfigProperty.get(PROPERTY_NAME))) {
                setCbrsDomainConfigValueInCache(npamConfigProperty.get(PROPERTY_VALUE));
                logger.info("setConfig:: written2 {}={}", NPAM_CONFIG_CBRS_DOMAIN, npamConfigCache.get(NPAM_CONFIG_CBRS_DOMAIN));
            }
            if (NPAM_CONFIG_EMERGENCY_USER.equals(npamConfigProperty.get(PROPERTY_NAME))
                    && NPamConfigPropertiesEnum.isKnownPropertyName(NPAM_CONFIG_EMERGENCY_USER)) {
                setEmergencyUserConfigValueInCache(npamConfigProperty.get(PROPERTY_VALUE));
                logger.info("setConfig:: written3 {}={}", NPAM_CONFIG_EMERGENCY_USER, npamConfigCache.get(NPAM_CONFIG_EMERGENCY_USER));
            }
            if (NPAM_CONFIG_RESTRICT_MAINTENANCE_USER.equals(npamConfigProperty.get(PROPERTY_NAME))
                    && NPamConfigPropertiesEnum.isKnownPropertyName(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER)) {
                setRestrictMaintenanceUserConfigValueInCache(npamConfigProperty.get(PROPERTY_VALUE));
                logger.info("setConfig:: written4 {}={}", NPAM_CONFIG_RESTRICT_MAINTENANCE_USER, npamConfigCache.get(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER));
            }
        }
    }

    /*
     *  Common methods
     *
     * */

    private void clearAndSetDefaultValuesInCache() {
        npamConfigCache.clear();
        npamConfigCache.put(NPAM_CONFIG_NPAM, false);
        npamConfigCache.put(NPAM_CONFIG_CBRS_DOMAIN, false);
        npamConfigCache.put(NPAM_CONFIG_EMERGENCY_USER, false);
        npamConfigCache.put(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER, false);
    }

    private void setNpamConfigValueInCache(final String value) {
        if (NPAM_CONFIG_PARAM_ENABLED.equals(value)) {
            logger.info("setNpamConfigValueInCache:: setting {}=true", NPAM_CONFIG_NPAM);
            npamConfigCache.put(NPAM_CONFIG_NPAM, true);
        } else {
            logger.info("setNpamConfigValueInCache:: setting {}=false", NPAM_CONFIG_NPAM);
            npamConfigCache.put(NPAM_CONFIG_NPAM, false);
        }
    }

    private void setCbrsDomainConfigValueInCache(final String value) {
        if (NPAM_CONFIG_PARAM_ENABLED.equals(value)) {
            logger.info("setCbrsDomainConfigValueInCache:: setting {}=true", NPAM_CONFIG_CBRS_DOMAIN);
            npamConfigCache.put(NPAM_CONFIG_CBRS_DOMAIN, true);
        } else {
            logger.info("setCbrsDomainConfigValueInCache:: setting {}=false", NPAM_CONFIG_CBRS_DOMAIN);
            npamConfigCache.put(NPAM_CONFIG_CBRS_DOMAIN, false);
        }
    }

    private void setEmergencyUserConfigValueInCache(final String value) {
        if (NPAM_CONFIG_PARAM_ENABLED.equals(value)) {
            logger.info("setEmergencyUserConfigValueInCache:: setting {}=true", NPAM_CONFIG_EMERGENCY_USER);
            npamConfigCache.put(NPAM_CONFIG_EMERGENCY_USER, true);
        } else {
            logger.info("setEmergencyUserConfigValueInCache:: setting {}=false", NPAM_CONFIG_EMERGENCY_USER);
            npamConfigCache.put(NPAM_CONFIG_EMERGENCY_USER, false);
        }
    }

    private void setRestrictMaintenanceUserConfigValueInCache(final String value) {
        if (NPAM_CONFIG_PARAM_ENABLED.equals(value)) {
            logger.info("setEmergencyUserConfigValueInCache:: setting {}=true", NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
            npamConfigCache.put(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER, true);
        } else {
            logger.info("setEmergencyUserConfigValueInCache:: setting {}=false", NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
            npamConfigCache.put(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER, false);
        }
    }
}
