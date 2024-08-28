/*
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 */
package com.ericsson.oss.services.security.npam.ejb.services;

import java.util.Arrays;

import javax.ejb.Stateless;

@Stateless
public class NodePamCredentialManagerImpl implements NodePamCredentialManager {
    // Default Credential Validation Rules
    private static int minLower = 3;
    private static int minUpper = 3;
    private static int minDigits = 2;
    private static int minSpecialCharacter = 1;
    private static int stringLen = 12;
    private static String specialCharacters = "@#$%&*()_+-.><'`^~:{},|!?=[]";

    @Override
    public String generateCredentialString(final String userName) {
        final RandomStringGenerator passwordGenerator = new RandomStringGenerator.RandomStringGeneratorBuilder().minLower(minLower)
                .minDigits(minDigits).minUpper(minUpper).minSpecialCharacter(minSpecialCharacter).stringLen(stringLen)
                .specialCharacters(specialCharacters).forbiddenWords(userName != null ? Arrays.asList(userName) : null).build();
        return passwordGenerator.generateSecret();
    }

    @Override
    public boolean validateCredentialString(final String credential, final String userName) {
        final StringValidator stringValidator = new StringValidator.StringValidatorBuilder().minLower(minLower).minDigits(minDigits)
                .minUpper(minUpper).minSpecialCharacter(minSpecialCharacter).stringLen(stringLen).specialCharacters(specialCharacters)
                .forbiddenWords(userName != null ? Arrays.asList(userName) : null).build();
        return stringValidator.validate(credential);
    }

    @Override
    public String generateUserName(final String nodeName) {
        return nodeName;
    }

    @Override
    public boolean validateKey(final String key) {
        final StringValidator stringValidator = new StringValidator.StringValidatorBuilder().stringLen(8).maxStringLen(32)
                .specialCharacters(specialCharacters).build();
        return stringValidator.validate(key);
    }
}
