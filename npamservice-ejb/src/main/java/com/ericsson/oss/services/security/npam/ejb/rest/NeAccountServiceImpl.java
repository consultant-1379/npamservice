/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.services.security.npam.ejb.rest;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.EXECUTE_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_EXPORT_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_PWD_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NPAM_NEACCOUNT_RESOURCE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.READ_ACTION;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_ONE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.MUID_TWO;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_DECRYPT_PASSWD;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_DECRYPT_USER;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_IP_RETRIEVAL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.ericsson.oss.services.security.npam.api.neaccount.modelentities.NetworkElementAccountUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.sdk.context.ContextService;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.annotation.Authorize;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.CryptoUtilsOpenSSL;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccountResponse;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus;
import com.ericsson.oss.services.security.npam.api.rest.NeAccountService;
import com.ericsson.oss.services.security.npam.ejb.database.availability.NodePamConfigStatus;
import com.ericsson.oss.services.security.npam.ejb.neaccount.executor.NeAccountGetServiceImpl;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamCredentialManager;
import com.ericsson.oss.services.security.npam.ejb.services.NodePamEncryptionManager;
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil;
import com.ericsson.oss.services.security.npam.ejb.utility.TbacEvaluation;

public class NeAccountServiceImpl implements NeAccountService {

    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NeAccountServiceImpl.class);
    private static final String SEPARATOR = ";";

    public static final String WARNING_LABEL = "Warning:";
    public static final String ERROR_LABEL = "Error:";

    public static final String CONFIG_PARAM_MSG = "status not aligned with NPAM configuration. Please run Update NE Accounts Configuration to resolve.";

    public static final String CBRS_PARAM_MSG = "CBRS status not aligned with NPAM configuration. Please run Update NE Accounts Configuration to resolve.";

    @Inject
    NeAccountGetServiceImpl neAccountGetServiceImpl;

    @Inject
    NodePamConfigStatus nodePamConfigStatus;

    @Inject
    ContextService ctx;

    @Inject
    NetworkUtil networkUtil;

    @Inject
    NodePamEncryptionManager nodePamEncryptionManager;

    @Inject
    CryptoUtilsOpenSSL cryptoUtilsOpenSSL;

    @Inject
    TbacEvaluation tbacEvaluation;

    @Inject
    NodePamCredentialManager nodePamCredentialManager;

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_RESOURCE, action = READ_ACTION)
    public List<NPamNEAccountResponse> getNEAccountStatusList(final NEInfo selectedNEInfo, final List<String> neIdList,
                                                              final List<NetworkElementStatus> neStatusList) {
        validateNeIdAccountParameter(neIdList);
        return getAllNEAccountStatusElements(selectedNEInfo, neIdList, neStatusList);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_RESOURCE, action = READ_ACTION)
    public List<NPamNEAccount> getNEAccounts(final String neName) {
        networkUtil.validateNeNames(Arrays.asList(neName), getUserFromContext());
        final List<NPamNEAccount> neAccountList = findNeAccountsByNeName(neName, null);
        if (neAccountList.isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NEACCOUNT_NOT_EXISTS);
        }
        return neAccountList;
    }

    @Override
    public String retrieveIpAddress(final String neName) {
        try {
            return neAccountGetServiceImpl.retrieveIp(neName);
        } catch (final Exception e) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DPS_RWISSUE, INTERNAL_SERVER_ERROR_IP_RETRIEVAL);
        }
    }

    private String getUserFromContext() {
        return ctx.getContextValue("X-Tor-UserID");
    }

    @Override
    public Set<String> getAllNes(final NEInfo selectedNEInfo) {
        return networkUtil.getAllNetworkElementFromNeInfo(selectedNEInfo, getUserFromContext(), false);
    }

    @Override
    @Authorize(resource = NPAM_NEACCOUNT_PWD_RESOURCE, action = READ_ACTION)
    public String getPwdInPlainText(final String credential) {
        return decryptPassword(credential);
    }

    @Override
    public String getPwdInPlainTextNoRBAC(final String credential) {
        return decryptPassword(credential);
    }

    @SuppressWarnings({"squid:S2093"})
    @Override
    @Authorize(resource = NPAM_NEACCOUNT_EXPORT_RESOURCE, action = EXECUTE_ACTION)
    public byte[] exportNeAccount(final NEInfo neInfo, final List<String> neAccountIdList, final List<NetworkElementStatus> neStatusList,
                                  final String key) {
        LOGGER.info("exportNeAccount: START neInfo neAccountIdList: {}, neStatusList: {} ", neAccountIdList, neStatusList);
        validateExportKey(key);

        final StringBuilder str = new StringBuilder();
        final List<NPamNEAccount> neAccountList = getNEAccountListForExport(neInfo, neAccountIdList, neStatusList);
        str.append("#NetworkElementName;UserName;Password\n");
        for (final NPamNEAccount neAccount : neAccountList) {
            str.append(neAccount.getNeName()).append(SEPARATOR).append(neAccount.getCurrentUser()).append(SEPARATOR)
            .append(decryptPassword(neAccount.getCurrentPswd())).append("\n");
        }

        try {
            final byte[] strBytes = str.toString().getBytes(StandardCharsets.UTF_8);
            if (key == null || key.isEmpty()) {
                return strBytes;
            }
            final int strLength = strBytes.length;
            final InputStream targetStream = new ByteArrayInputStream(strBytes);
            final ByteArrayOutputStream encryptedCode = cryptoUtilsOpenSSL.encrypt(key, targetStream, strLength);
            return encryptedCode.toByteArray();
        } catch (final Exception e) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_ENCRYPT_FILE);
        } finally {
            LOGGER.info("exportNeAccount: STOP");
        }
    }

    private void validateExportKey(final String key) {
        if (key == null || key.isEmpty()) {
            LOGGER.info("exportNeAccount: plaintext file");
        } else if (!nodePamCredentialManager.validateKey(key)) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_ENCRYPTION_KEY);
        } else {
            LOGGER.info("exportNeAccount: encrypted file");
        }
    }

    private List<NPamNEAccountResponse> getAllNEAccountStatusElements(final NEInfo selectedNEInfo, final List<String> neIdAccountList,
                                                                      final List<NetworkElementStatus> neStatusList) {
        final List<NPamNEAccountResponse> nPamNEAccountResponseList = new ArrayList<>();
        validateSelectNeInfoParameter(selectedNEInfo);

        final Set<String> neNames = networkUtil.getAllNetworkElementFromNeInfo(selectedNEInfo, getUserFromContext(), false);

        final Map<String, List<NPamNEAccount>> neAccountMap = neAccountGetServiceImpl.getMapAllNEAccountsById(neIdAccountList);
        final Map<String, Boolean> remoteManagementMap = neAccountGetServiceImpl.findAllRemoteManagementField();

        for (final String neName : neNames) {
            final NPamNEAccountResponse nPamNEAccountResponse = new NPamNEAccountResponse();
            nPamNEAccountResponse.setNeName(neName);
            if (neAccountMap.get(neName) != null) {
                final List<NPamNEAccount> neAccountPlainTextUserList = decryptAndUpdateUserInNeAccountList(neAccountMap.get(neName), remoteManagementMap.get(neName));
                nPamNEAccountResponse.setNeAccounts(neAccountPlainTextUserList);
            } else {
                nPamNEAccountResponse.setNeAccounts(new ArrayList<>());
            }
            Boolean remoteManagementField = null;
            remoteManagementField = remoteManagementMap.get(neName);
            if (calculateNeNpamStatus(neStatusList, nPamNEAccountResponse, remoteManagementField)) {
                nPamNEAccountResponseList.add(nPamNEAccountResponse);
            }
        }
        return nPamNEAccountResponseList;
    }

    private boolean calculateNeNpamStatus(final List<NetworkElementStatus> neStatusList, final NPamNEAccountResponse nPamNEAccountResponse,
                                          final Boolean remoteManagementField) {
        boolean toAdd = false;
        if (remoteManagementField == null && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.NOT_SUPPORTED))) {
            nPamNEAccountResponse.setNeNpamStatus(NetworkElementStatus.NOT_SUPPORTED);
            toAdd = true;
        } else if (Boolean.TRUE.equals(remoteManagementField)
                && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.MANAGED))) {
            nPamNEAccountResponse.setNeNpamStatus(NetworkElementStatus.MANAGED);
            toAdd = true;
        } else if (Boolean.FALSE.equals(remoteManagementField)
                && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.NOT_MANAGED))) {
            nPamNEAccountResponse.setNeNpamStatus(NetworkElementStatus.NOT_MANAGED);
            toAdd = true;
        }
        return toAdd;
    }

    private List<NPamNEAccount> decryptAndUpdateUserInNeAccountList(final List<NPamNEAccount> list, final Boolean remoteManagementField) {
        final List<NPamNEAccount> neAccountPlainTextUserList = new ArrayList<>();
        for (final NPamNEAccount neAccount : list) {
            decryptCurrentUser(neAccount);
            updateNeAccountDetailsIfNeeded(neAccount,remoteManagementField);
            neAccountPlainTextUserList.add(neAccount);
        }
        return neAccountPlainTextUserList;
    }


    private void updateNeAccountDetailsIfNeeded(final NPamNEAccount neAccount, final Boolean remoteManagementField) {
        String errorDetails = null;

        if (neAccount.getErrorDetails() != null) {
            errorDetails = ERROR_LABEL + " " + neAccount.getErrorDetails();
        } else {
            if (neAccount.getNetworkElementAccountId().equals(MUID_ONE) && nodePamConfigStatus.isEnabled()) {
                if ((remoteManagementField && NetworkElementAccountUpdateStatus.DETACHED.name().equals(neAccount.getStatus())) || (!remoteManagementField && NetworkElementAccountUpdateStatus.CONFIGURED.name().equals(neAccount.getStatus()))) {
                    errorDetails = WARNING_LABEL + " " + CONFIG_PARAM_MSG;
                }
            } else if (neAccount.getNetworkElementAccountId().equals(MUID_TWO) && nodePamConfigStatus.isCbrsDomainEnabled()) {
                if ((remoteManagementField && NetworkElementAccountUpdateStatus.DETACHED.name().equals(neAccount.getStatus())) || (!remoteManagementField && NetworkElementAccountUpdateStatus.CONFIGURED.name().equals(neAccount.getStatus()))) {
                    errorDetails = WARNING_LABEL + " " + CBRS_PARAM_MSG;
                }
            } else if ((neAccount.getNetworkElementAccountId().equals(MUID_TWO) && !nodePamConfigStatus.isCbrsDomainEnabled())) {
                errorDetails = WARNING_LABEL + " " + CBRS_PARAM_MSG;
            }
        }

        neAccount.setErrorDetails(errorDetails);
    }

    /*
     *   Export NetworkElementAccount(s)
     * */

    private List<NPamNEAccount> getNEAccountListForExport(final NEInfo neInfo, final List<String> neAccountIdList,
                                                          final List<NetworkElementStatus> neStatusList) {
        validateExportNeAccounts(neInfo);
        validateExportNeStatus(neStatusList);
        validateNeIdAccountParameter(neAccountIdList);

        if (neInfo == null) {
            // neAccountIdList and neStatusList are not empty
            return getAllNEAccountsByStatusAndId(neAccountIdList, neStatusList);
        }
        else {
            return getAllNEAccountsByStatusAndIdAndNEInfo(neInfo, neAccountIdList, neStatusList);
        }
    }

    private List<NPamNEAccount> getAllNEAccountsByStatusAndId(final List<String> neIdAccountList,
                                                              final List<NetworkElementStatus> neStatusList) {
        LOGGER.info("getAllNEAccountsByStatusAndId:: START");
        final List<NPamNEAccount> neAccountList = getAllNEAccountsById(neIdAccountList);
        final Map<String, Boolean> remoteManagementMap = neAccountGetServiceImpl.findAllRemoteManagementField();

        final List<NPamNEAccount> filteredNeAccountList = new ArrayList<>();
        for (final NPamNEAccount neAccount : neAccountList) {
            if (!tbacEvaluation.getNodePermission(getUserFromContext(), neAccount.getNeName())) {
                continue;
            }
            if (toBeAddedNEAccount(neStatusList, remoteManagementMap.get(neAccount.getNeName())) && isValidCurrentUserNameAndCurrentPassword(neAccount)) {
                decryptCurrentUser(neAccount);
                filteredNeAccountList.add(neAccount);
            }
        }
        LOGGER.info("getAllNEAccountsByStatusAndId:: filteredNeAccountList.size={}", filteredNeAccountList.size());
        LOGGER.info("getAllNEAccountsByStatusAndId:: STOP");
        return filteredNeAccountList;
    }


    private List<NPamNEAccount> getAllNEAccountsByStatusAndIdAndNEInfo(final NEInfo selectedNEInfo, final List<String> neIdAccountList,
                                                                       final List<NetworkElementStatus> neStatusList) {
        LOGGER.info("getAllNEAccountsByStatusAndIdAndNEInfo:: START");

        //Validate Node Names (as in original implementation if one node is invalid for TBAC an exception is raised)
        validateSelectNeInfoParameter(selectedNEInfo);
        final Set<String> neNames = networkUtil.getAllNetworkElementFromNeInfo(selectedNEInfo, getUserFromContext(), false);
        LOGGER.info("getAllNEAccountsByStatusAndIdAndNEInfo: neNames.size()={}", neNames.size());

        final List<NPamNEAccount> neAccountList = getAllNEAccountsById(neIdAccountList);
        final Map<String, Boolean> remoteManagementMap = neAccountGetServiceImpl.findAllRemoteManagementField();

        final List<NPamNEAccount> filteredNeAccountList = new ArrayList<>();
        for (final NPamNEAccount neAccount : neAccountList) {
            if (isSelectedNode(neAccount, neNames) && toBeAddedNEAccount(neStatusList, remoteManagementMap.get(neAccount.getNeName())) && isValidCurrentUserNameAndCurrentPassword(neAccount)) {
                decryptCurrentUser(neAccount);
                filteredNeAccountList.add(neAccount);
            }
        }
        LOGGER.info("getAllNEAccountsByStatusAndIdAndNEInfo:: filteredNeAccountList.size={}", filteredNeAccountList.size());
        LOGGER.info("getAllNEAccountsByStatusAndIdAndNEInfo:: STOP");
        return filteredNeAccountList;
    }

    private List<NPamNEAccount> getAllNEAccountsById(final List<String> neIdAccountList) {
        List<NPamNEAccount> neAccountList = new ArrayList<>();
        try {
            neAccountList = neAccountGetServiceImpl.getAllNEAccountsById(neIdAccountList);
            LOGGER.info("getAllNEAccountsByStatusAndIdAndNEInfo:: recovered neAccountList.size={}", neAccountList.size());
        } catch (final Exception e) { LOGGER.debug("getAllNEAccountsByStatusAndIdAndNEInfo:: exception ", e); throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DPS_RWISSUE);}
        return neAccountList;
    }

    private boolean isSelectedNode(final NPamNEAccount neAccount, final Set<String> neNames) {
        return neNames.contains(neAccount.getNeName());
    }

    @SuppressWarnings({"squid:S1871"})
    private boolean toBeAddedNEAccount(final List<NetworkElementStatus> neStatusList, final Boolean remoteManagementField) {
        boolean toAdd = false;
        if (remoteManagementField == null && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.NOT_SUPPORTED))) {
            toAdd = true;
        } else if (Boolean.TRUE.equals(remoteManagementField) && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.MANAGED))) {
            toAdd = true;
        } else if (Boolean.FALSE.equals(remoteManagementField) && (neStatusList.isEmpty() || neStatusList.contains(NetworkElementStatus.NOT_MANAGED))) {
            toAdd = true;
        }
        return toAdd;
    }

    private void decryptCurrentUser(final NPamNEAccount neAccount) {
        try {
            if (neAccount.getCurrentUser() != null) {
                neAccount.setCurrentUser(nodePamEncryptionManager.decryptPassword(neAccount.getCurrentUser()));
            }
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error("Error encrypting current user");
            LOGGER.debug("Error encrypting current user ", e);
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT, INTERNAL_SERVER_ERROR_DECRYPT_USER);
        }
    }

    private void validateNeIdAccountParameter(final List<String> neIdAccountList) {
        final List<String> results = new ArrayList<>(neIdAccountList);
        results.removeAll(new ArrayList<>(Arrays.asList("1", "2")));
        if (!results.isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_MUID);
        }
    }

    private void validateSelectNeInfoParameter(final NEInfo selectedNEInfo) {
        if (selectedNEInfo == null || (selectedNEInfo.getNeNames() == null ||
                selectedNEInfo.getCollectionNames() == null ||
                selectedNEInfo.getSavedSearchIds() == null )) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
        if (selectedNEInfo.getNeNames().isEmpty() && selectedNEInfo.getCollectionNames().isEmpty() && selectedNEInfo.getSavedSearchIds().isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
    }

    private List<NPamNEAccount> findNeAccountsByNeName(final String neName, final List<String> neIdAccountList) {
        final List<NPamNEAccount> neAccountList = new ArrayList<>();
        List<NPamNEAccount> neAccounts = null;
        try {
            neAccounts = neAccountGetServiceImpl.findNEAccountObject(neName, neIdAccountList);
        } catch (final Exception e) {
            LOGGER.error("Error Retrieve object in DPS ");
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DPS_RWISSUE);
        }
        for (final NPamNEAccount elem : neAccounts) {
            try {
                if (elem.getCurrentUser() != null) {
                    elem.setCurrentUser(nodePamEncryptionManager.decryptPassword(elem.getCurrentUser()));
                }
                neAccountList.add(elem);
            } catch (final UnsupportedEncodingException e) {
                LOGGER.error("Error encrypting current user");
                LOGGER.debug("Error encrypting current user ", e);
                throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT, INTERNAL_SERVER_ERROR_DECRYPT_USER);
            }
        }
        return neAccountList;
    }

    private void validateExportNeAccounts(final NEInfo neInfo) {
        if (neInfo != null && (neInfo.getNeNames() == null || neInfo.getCollectionNames() == null || neInfo.getSavedSearchIds() == null)) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
        if (neInfo != null && (neInfo.getNeNames().isEmpty() && neInfo.getCollectionNames().isEmpty() && neInfo.getSavedSearchIds().isEmpty())) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
    }

    private String decryptPassword(final String credential) {
        try {
            return nodePamEncryptionManager.decryptPassword(credential);
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error("Error decrypting current password.");
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_DECRYPT, INTERNAL_SERVER_ERROR_DECRYPT_PASSWD);
        }
    }

    private void validateExportNeStatus(final List<NetworkElementStatus> neStatusList) {
        for (final NetworkElementStatus status : neStatusList) {
            if (status == NetworkElementStatus.NOT_SUPPORTED) {
                throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_NE_NPAM_STATUS);
            }
        }
    }

    private boolean isValidCurrentUserNameAndCurrentPassword(final NPamNEAccount neAccount) {
        return ((neAccount.getCurrentUser() != null && !neAccount.getCurrentUser().isEmpty())
                && (neAccount.getCurrentPswd() != null && !neAccount.getCurrentPswd().isEmpty()));
    }
}
