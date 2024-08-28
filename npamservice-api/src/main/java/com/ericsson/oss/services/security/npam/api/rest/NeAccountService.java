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

package com.ericsson.oss.services.security.npam.api.rest;

import java.util.List;
import java.util.Set;

import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccount;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamNEAccountResponse;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NetworkElementStatus;

public interface NeAccountService {
    List<NPamNEAccountResponse> getNEAccountStatusList(NEInfo selectedNEInfo, List<String> neIdList, List<NetworkElementStatus> neStatusList);

    List<NPamNEAccount> getNEAccounts(String neName);

    String retrieveIpAddress(String neName);

    Set<String> getAllNes(NEInfo selectedNEInfo);

    String getPwdInPlainText(String credential);

    String getPwdInPlainTextNoRBAC(String credential);

    /*
     * export a crypted file with targeName, user, password semicolon separated
     *
     * @param neInfo 
     *     selected nes to export data for.
     * @param neAccountIdList
     *      neAccount required.
     * @param neStatusList
     *      ne status required.
     * @param string
     *      key for encryption.
     * @return byte exported.
     */
    byte[] exportNeAccount(NEInfo neInfo, List<String> neAccountIdList, List<NetworkElementStatus> neStatusList, String string);
}
