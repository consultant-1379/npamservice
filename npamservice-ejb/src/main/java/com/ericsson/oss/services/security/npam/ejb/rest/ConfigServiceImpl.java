/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.rest;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.READ_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.UPDATE_ACTION;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.UNPROCESSABLE_DUPLICATED_PROPERTIES;

import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.NPamConfigService;
import com.ericsson.oss.services.security.npam.api.rest.ConfigService;

public class ConfigServiceImpl implements ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigServiceImpl.class);

    @Inject
    NPamConfigService nPamConfigService;

    @Override
    @Authorize(resource = NPAM_CONFIG_RESOURCE, action = READ_ACTION)
    public NPamConfig getNPamConfig() {
        LOGGER.debug("Authorize for Get NPamConfig");
        if (nPamConfigService.getNPamConfig().size() > 1) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DUPLICATED_NPAMCONFIG);

        }
        return nPamConfigService.getNPamConfig().get(0);
    }

    @Override
    @Authorize(resource = NPAM_CONFIG_RESOURCE, action = UPDATE_ACTION)
    public NPamConfig updateNPamConfig(final NPamConfig nPamConfig) {
        LOGGER.debug("Authorize for Update NpamConfig");
        if (!isValidPropertyList(nPamConfig.getnPamConfigProperties())) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES,
                    UNPROCESSABLE_DUPLICATED_PROPERTIES);

        }
        for (final NPamConfigProperty el : nPamConfig.getnPamConfigProperties()) {
            //we check values for known parameters
            if (!NPamConfigPropertiesEnum.isKnownPropertyName(el.getName())) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES, el.getName());
            }
            if (!isValidPropertyValue(el.getName(), el.getValue())) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NPAMCONFIG_PROPERTIES, el.getValue());
            }
        }
        return nPamConfigService.updateNPamConfig(nPamConfig);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_RESOURCE, action = READ_ACTION)
    public NPamConfig getNPamConfigCached() {
        LOGGER.debug("Authorize for Get NpamConfig from GUI");
        return nPamConfigService.getNPamConfigCached();
    }

    private boolean isValidPropertyValue(final String name, final String value) {
        if (NPamConfigPropertiesEnum.getAllowedvalues().get(name) == null) {
            return true;
        }
        return NPamConfigPropertiesEnum.getAllowedvalues().get(name).contains(value);
    }

    private boolean isValidPropertyList(final List<NPamConfigProperty> nPamConfigProperties) {
        final List<NPamConfigProperty> toUpdateProperties = nPamConfigProperties.stream().distinct().collect(Collectors.toList());
        return toUpdateProperties.size() == nPamConfigProperties.size();
    }
}
