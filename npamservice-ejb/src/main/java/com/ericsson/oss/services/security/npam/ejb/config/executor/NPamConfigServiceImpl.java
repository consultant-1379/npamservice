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
package com.ericsson.oss.services.security.npam.ejb.config.executor;

import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_NAME;
import static com.ericsson.oss.services.security.npam.api.constants.JobConfigurationConstants.PROPERTY_VALUE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_CBRS_DOMAIN;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_EMERGENCY_USER;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_NPAM;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_DISABLED;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PARAM_ENABLED;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_PROPERTIES;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_CONFIG_RESTRICT_MAINTENANCE_USER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.recording.SystemRecorder;
import com.ericsson.oss.services.security.npam.api.cal.CALConstants;
import com.ericsson.oss.services.security.npam.api.cal.CALDetailResultJSON;
import com.ericsson.oss.services.security.npam.api.cal.CALRecorderDTO;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfig;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigPropertiesEnum;
import com.ericsson.oss.services.security.npam.api.config.modelentities.NPamConfigProperty;
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.NPamConfigService;
import com.ericsson.oss.services.security.npam.api.message.NodePamDisableFeatureRequest;
import com.ericsson.oss.services.security.npam.ejb.config.dao.NPamConfigDpsReader;
import com.ericsson.oss.services.security.npam.ejb.config.dao.NPamConfigDpsWriter;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.listener.MembershipListenerInterface;
import com.ericsson.oss.services.security.npam.ejb.message.NodePamQueueMessageSender;

public class NPamConfigServiceImpl implements NPamConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NPamConfigServiceImpl.class);
    @Inject
    NPamConfigDpsReader configDpsReader;

    @Inject
    NPamConfigDpsWriter configDpsWriter;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Inject
    MembershipListenerInterface membershipListenerInterface;

    @Inject
    private NodePamQueueMessageSender nodePamQueueMessageSender;

    @Inject
    CALRecorderDTO cALRecorderDTO;

    @Inject
    SystemRecorder systemRecorder;

    @Override
    public List<NPamConfig> getNPamConfig() {
        LOGGER.debug("Retrieving NPam Configuration");

        final List<NPamConfig> nPamConfig = configDpsReader.getNPamConfigObject();
        if (nPamConfig.isEmpty()) {
            return Arrays.asList(new NPamConfig());
        }

        if (nPamConfig.size() > 1) {//should throw exception????
            LOGGER.error("Found more than one NPamConfig, restoring.");
        }

        return nPamConfig;
    }

    @Override
    public NPamConfig getNPamConfigCached() {
        LOGGER.debug("Retrieving NPam Configuration from cache.");
        final NPamConfig nPamConfig = new NPamConfig();

        final List<NPamConfigProperty> nPamConfigProperties = new ArrayList<>();
        nPamConfig.setnPamConfigProperties(nPamConfigProperties);

        NPamConfigProperty nPamConfigProperty = new NPamConfigProperty();
        nPamConfigProperty.setName(NPAM_CONFIG_NPAM);
        nPamConfigProperty.setValue(nodePamConfigStatus.isEnabled() ? NPAM_CONFIG_PARAM_ENABLED : NPAM_CONFIG_PARAM_DISABLED);
        nPamConfigProperties.add(nPamConfigProperty);

        nPamConfigProperty = new NPamConfigProperty();
        nPamConfigProperty.setName(NPAM_CONFIG_CBRS_DOMAIN);
        nPamConfigProperty.setValue(nodePamConfigStatus.isCbrsDomainEnabled() ? NPAM_CONFIG_PARAM_ENABLED : NPAM_CONFIG_PARAM_DISABLED);
        nPamConfigProperties.add(nPamConfigProperty);

        nPamConfigProperty = new NPamConfigProperty();
        nPamConfigProperty.setName(NPAM_CONFIG_EMERGENCY_USER);
        nPamConfigProperty.setValue(nodePamConfigStatus.isEmergencyUserEnabled() ? NPAM_CONFIG_PARAM_ENABLED : NPAM_CONFIG_PARAM_DISABLED);
        nPamConfigProperties.add(nPamConfigProperty);

        nPamConfigProperty = new NPamConfigProperty();
        nPamConfigProperty.setName(NPAM_CONFIG_RESTRICT_MAINTENANCE_USER);
        nPamConfigProperty.setValue(nodePamConfigStatus.isRestrictMaintenanceUserEnabled() ? NPAM_CONFIG_PARAM_ENABLED : NPAM_CONFIG_PARAM_DISABLED);
        nPamConfigProperties.add(nPamConfigProperty);

        return nPamConfig;
    }

    @Override
    public long createNPamConfig() {
        if (!membershipListenerInterface.isMaster()) {
            LOGGER.info("createNPamConfig:: isMaster=false so do nothing");
            return 0;
        }

        LOGGER.info("createNPamConfig:: isMaster=true");
        final List<NPamConfig> nPamConfig = getNPamConfig();
        List<NPamConfigProperty> toAddNpamConfigProperties;
        if (nPamConfig.size() > 1) {
            LOGGER.error("Too many NPamConfig ({}) in DPS. Reset to default", nPamConfig.size());

            final List<Long> ids = new ArrayList<>();
            for (final NPamConfig nPamConf : nPamConfig) {
                ids.add(nPamConf.getPoId());
            }
            configDpsWriter.deletePOList(ids);
        } else if (!nPamConfig.get(0).getnPamConfigProperties().isEmpty() && nPamConfig.get(0).getPoId() != -1) {

            final List<NPamConfigProperty> dpsNpamConfigProperties = nPamConfig.get(0).getnPamConfigProperties();
            toAddNpamConfigProperties = compareNPamConfigProperties(dpsNpamConfigProperties);
            if (!toAddNpamConfigProperties.isEmpty()) {
                LOGGER.warn("Npamconfig has too few attributes. Update NPamConfig with new attributes");

                final NPamConfig updatedNPamConfig = prepareUpdateNPamConfig(nPamConfig.get(0), toAddNpamConfigProperties);
                final Map<String, Object> attributes = new HashMap<>();
                attributes.put(NPAM_CONFIG_PROPERTIES, prepareNPamConfigProperties(updatedNPamConfig));

                configDpsWriter.update(nPamConfig.get(0).getPoId(), attributes);
                final String message = "Added NPamConfig : {} = {}";
                for (final NPamConfigProperty el : toAddNpamConfigProperties) {
                    LOGGER.info(message, el.getName(), el.getValue());
                }
                return nPamConfig.get(0).getPoId();
            } else {
                LOGGER.info("NPAM already exists in DPS.");
                return nPamConfig.get(0).getPoId();
            }
        }
        final Map<String, Object> attributes = new HashMap<>();
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>();
        for (final NPamConfigPropertiesEnum propertyEnum : NPamConfigPropertiesEnum.getconfigPropertiesMap().values()) {
            nPamConfigProperties.add(NPamConfigPropertiesEnum.getMapProperty(propertyEnum));
        }

        attributes.put(NPAM_CONFIG_PROPERTIES, nPamConfigProperties);
        LOGGER.info("Creating NPamConfig in DPS.");
        return configDpsWriter.createPO(ModelsConstants.NAMESPACE, ModelsConstants.NPAM_CONFIG, ModelsConstants.VERSION, attributes).getPoId();
    }

    private List<NPamConfigProperty> compareNPamConfigProperties(final List<NPamConfigProperty> dpsNpamConfigProperties) {
        final List<NPamConfigProperty> wantedNpamConfigProperties = NPamConfigPropertiesEnum.npamConfigPropertiesList();
        wantedNpamConfigProperties.removeAll(dpsNpamConfigProperties);
        return wantedNpamConfigProperties;
    }

    private NPamConfig prepareUpdateNPamConfig(final NPamConfig nPamConfig, final List<NPamConfigProperty> toAddNpamConfigProperties) {
        for (final NPamConfigProperty param : toAddNpamConfigProperties) {
            nPamConfig.getnPamConfigProperties().add(param);
        }
        return nPamConfig;
    }

    @Override
    public NPamConfig updateNPamConfig(final NPamConfig nPamConfig) {
        final int DEFAULT_PO_ID = -1;
        final NPamConfig dpsNpamConfig = getNPamConfig().get(0);
        final long poId = dpsNpamConfig.getPoId();
        if (poId == DEFAULT_PO_ID) {
            return dpsNpamConfig;
        }
        final List<NPamConfigProperty> dpsNpamConfigProperties = dpsNpamConfig.getnPamConfigProperties();
        final List<NPamConfigProperty> toUpdateProperties = nPamConfig.getnPamConfigProperties();
        if (systemRecorder.isCompactAuditEnabled()) {
            fillCALDetailResult(dpsNpamConfigProperties, toUpdateProperties);
        }

        final List<NPamConfigProperty> toUpdatePropertiesDistinct = toUpdateProperties.stream().distinct().collect(Collectors.toList());
        if (toUpdateProperties.size() != toUpdatePropertiesDistinct.size()) {
            return dpsNpamConfig;
        }
        if (!isNpamConfigUpdatePermitted(toUpdateProperties, dpsNpamConfigProperties) && isCbrsEnabling(toUpdateProperties)) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.FORBIDDEN_FEATURE_DISABLED);
        }
        final boolean npamDisable = isNpamUpdatingToDisable(toUpdateProperties);
        if (npamDisable) {
            for (final NPamConfigProperty prop : toUpdateProperties) {
                if (NPamConfigPropertiesEnum.getconfigPropertiesMap().get(prop.getName()) != null) {
                    prop.setValue((NPamConfigPropertiesEnum.getconfigPropertiesMap().get(prop.getName())).getPropertyDefaultValue());
                }
            }
        }
        mergeDpsNpamConfig(nPamConfig, dpsNpamConfigProperties, npamDisable);

        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(NPAM_CONFIG_PROPERTIES, prepareNPamConfigProperties(nPamConfig));

        configDpsWriter.update(poId, attributes);
        final String message = "Updated NPamConfig : {} = {}";
        for (final NPamConfigProperty el : nPamConfig.getnPamConfigProperties()) {
            LOGGER.info(message, el.getName(), el.getValue());
        }
        if (npamDisable) {
            final NodePamDisableFeatureRequest nodeDisableFeature = new NodePamDisableFeatureRequest();
            nodePamQueueMessageSender.sendDisableFeatureMessage(nodeDisableFeature);
        }
        return nPamConfig;
    }

    private void fillCALDetailResult(final List<NPamConfigProperty> dpsNpamConfigProperties, final List<NPamConfigProperty> toUpdateProperties) {
        final CALDetailResultJSON cALDetailResultJSON = new CALDetailResultJSON(CALConstants.UPDATE, CALConstants.NPAMCONFIG);

        final Map<String, Object> currentValues = new HashMap<>();
        for (final NPamConfigProperty newValue : toUpdateProperties) {
            currentValues.put(newValue.getName(), newValue.getValue());
        }
        cALDetailResultJSON.setCurrentValues(currentValues);

        final Map<String, Object> oldValues = new HashMap<>();
        for (final NPamConfigProperty old : dpsNpamConfigProperties) {
            oldValues.put(old.getName(), old.getValue());
        }
        cALDetailResultJSON.setOldValues(oldValues);
        cALRecorderDTO.setDetailResult(Arrays.asList(cALDetailResultJSON));
    }

    private void mergeDpsNpamConfig(final NPamConfig nPamConfig, final List<NPamConfigProperty> dpsNpamConfigProperties,
                                    final boolean npamDisable) {
        for (final NPamConfigProperty dpsProp : dpsNpamConfigProperties) {
            boolean foundFlag = false;
            foundFlag = isPropertyToUpdate(nPamConfig.getnPamConfigProperties(), dpsProp);
            final NPamConfigPropertiesEnum propertyEnum = NPamConfigPropertiesEnum.getconfigPropertiesMap().get(dpsProp.getName());

            if (!foundFlag) {
                if (npamDisable) {
                    if (propertyEnum != null) {
                        nPamConfig.getnPamConfigProperties().add(new NPamConfigProperty(dpsProp.getName(), propertyEnum.getPropertyDefaultValue()));
                    }
                } else {
                    if (propertyEnum != null) {
                        nPamConfig.getnPamConfigProperties().add(new NPamConfigProperty(dpsProp.getName(), dpsProp.getValue()));
                    }
                }
            }
        }
    }

    private boolean isPropertyToUpdate(final List<NPamConfigProperty> toUpdateProperties, final NPamConfigProperty dpsProp) {
        for (final NPamConfigProperty prop : toUpdateProperties) {
            if (dpsProp.getName().equals(prop.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isNpamUpdatingToDisable(final List<NPamConfigProperty> toUpdateProperties) {
        for (final NPamConfigProperty prop : toUpdateProperties) {
            if (NPamConfigPropertiesEnum.NPAM_CONFIG_NPAM.getPropertyName().equals(prop.getName())
                    && NPamConfigPropertiesEnum.NPAM_CONFIG_PARAM_DISABLED.equals(prop.getValue())) {
                LOGGER.warn("All properties will be disabled");
                return true;
            }
        }
        return false;
    }

    private boolean isNpamConfigUpdatePermitted(final List<NPamConfigProperty> toUpdateProperties, final List<NPamConfigProperty> dpsUpdateProperties) {
        if (isNpamFeatureEnabling(toUpdateProperties)) {
            return true;
        }
        return isNpamFeatureEnabled(dpsUpdateProperties);
    }

    private boolean isNpamFeatureEnabled(final List<NPamConfigProperty> dpsUpdateProperties) {
        for (final NPamConfigProperty prop : dpsUpdateProperties) {
            if (NPamConfigPropertiesEnum.NPAM_CONFIG_NPAM.getPropertyName().equals(prop.getName())
                    && NPamConfigPropertiesEnum.NPAM_CONFIG_PARAM_DISABLED.equals(prop.getValue())) {
                LOGGER.warn("Feature not enabled. Update not allowed");
                return false;
            }
        }
        return true;
    }

    private boolean isNpamFeatureEnabling(final List<NPamConfigProperty> toUpdateProperties) {
        for (final NPamConfigProperty prop : toUpdateProperties) {
            if (NPamConfigPropertiesEnum.NPAM_CONFIG_NPAM.getPropertyName().equals(prop.getName())
                    && NPamConfigPropertiesEnum.NPAM_CONFIG_PARAM_ENABLED.equals(prop.getValue())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCbrsEnabling(final List<NPamConfigProperty> toUpdateProperties) {
        for (final NPamConfigProperty prop : toUpdateProperties) {
            if (NPamConfigPropertiesEnum.NPAM_CONFIG_CBRS_DOMAIN.getPropertyName().equals(prop.getName())
                    && NPamConfigPropertiesEnum.NPAM_CONFIG_PARAM_ENABLED.equals(prop.getValue())) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, String>> prepareNPamConfigProperties(final NPamConfig nPamConfig) {
        final List<Map<String, String>> nPamConfigProperties = new ArrayList<>();
        for (final NPamConfigProperty nPamConfigProperty : nPamConfig.getnPamConfigProperties()) {
            nPamConfigProperties.add(prepareNPamConfigProperty(nPamConfigProperty.getName(), nPamConfigProperty.getValue()));
        }
        return nPamConfigProperties;
    }

    private Map<String, String> prepareNPamConfigProperty(final String propertyName, final String propertyValue) {
        final Map<String, String> property = new HashMap<>();
        property.put(PROPERTY_NAME, propertyName);
        property.put(PROPERTY_VALUE, propertyValue);
        return property;
    }

}
