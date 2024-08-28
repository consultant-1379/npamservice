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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringValidator {
    private static final Logger logger = LoggerFactory.getLogger(StringValidator.class);
    private int minLower;
    private int minUpper;
    private int minDigits;
    private int minSpecialCharacter;
    private int stringLen;
    private int maxStringLen;
    private String specialCharacters;
    private List<String> dictionaryForbidden;

    private StringValidator() {
        throw new UnsupportedOperationException("Empty constructor is not supported.");
    }

    private StringValidator(final StringValidatorBuilder builder) {
        this.minLower = builder.minLower;
        this.minUpper = builder.minUpper;
        this.minDigits = builder.minDigits;
        this.minSpecialCharacter = builder.minSpecialCharacter;
        this.stringLen = builder.stringLen;
        this.maxStringLen = builder.maxStringLen;
        this.specialCharacters = builder.specialCharacters;
        this.dictionaryForbidden = builder.dictionaryForbidden;
    }

    public static class StringValidatorBuilder {
        private int minLower;
        private int minUpper;
        private int minDigits;
        private int minSpecialCharacter;
        private int stringLen;
        private int maxStringLen;
        private String specialCharacters;
        private final List<String> dictionaryForbidden;
        public StringValidatorBuilder() {
            this.minLower = 0;
            this.minUpper = 0;
            this.minDigits = 0;
            this.minSpecialCharacter = 0;
            this.stringLen = 8;
            this.maxStringLen = 0;
            this.specialCharacters = "";
            this.dictionaryForbidden = new ArrayList<>();
        }

        public StringValidatorBuilder minUpper(final int minUpper) {
            if (minUpper >= 0) {
                this.minUpper = minUpper;
            } else {
                throw new IllegalArgumentException("minUpper value should be greater or equal to 0");
            }
            return this;
        }

        public StringValidatorBuilder minLower(final int minLower) {
            if (minLower >= 0) {
                this.minLower = minLower;
            } else {
                throw new IllegalArgumentException("minLower value should be greater or equal to 0");
            }
            return this;
        }

        public StringValidatorBuilder minDigits(final int minDigits) {
            if (minDigits >= 0) {
                this.minDigits = minDigits;
            } else {
                throw new IllegalArgumentException("minDigits value should be greater or equal to 0");
            }
            return this;
        }

        public StringValidatorBuilder minSpecialCharacter(final int minSpecialCharacter) {
            if (minSpecialCharacter >= 0) {
                this.minSpecialCharacter = minSpecialCharacter;
            } else {
                throw new IllegalArgumentException("minSpecialCharacter value should be greater or equal to 0");
            }
            return this;
        }

        public StringValidatorBuilder stringLen(final int stringLen) {
            if (stringLen > 0) {
                this.stringLen = stringLen;
            } else {
                throw new IllegalArgumentException("stringLen value should be greater than 0");
            }
            return this;
        }

        public StringValidatorBuilder maxStringLen(final int maxStringLen) {
            if (maxStringLen > 0) {
                this.maxStringLen = maxStringLen;
            } else {
                throw new IllegalArgumentException("maxStringLen value should be greater than 0");
            }
            return this;
        }

        public StringValidatorBuilder specialCharacters(final String specialChar) {
            this.specialCharacters = specialChar;
            return this;
        }

        public StringValidatorBuilder forbiddenWords(final List<String> words) {
            if (words != null) {
                this.dictionaryForbidden.addAll(words);
            }
            return this;
        }

        public StringValidator build() {
            return new StringValidator(this);
        }
    }

    boolean validate(final String secret) {
        int countDigits = 0;
        int countUpper = 0;
        int countLower = 0;
        int countSpecialChar = 0;

        if (!validateStringLength(secret)) {
            return false;
        }
        for (final char c : secret.toCharArray()) {
            if (Character.isDigit(c)) {
                countDigits++;
            } else if (Character.isUpperCase(c)) {
                countUpper++;
            } else if ((specialCharacters).indexOf(c) != -1) {
                countSpecialChar++;
            } else if (Character.isLowerCase(c)) {
                countLower++;
            } else {
                logger.info("Special Character not supported {}", c);
                return false;
            }
        }
        for (final String el : dictionaryForbidden) {
            if (secret.contains(el) || secret.contains(new StringBuffer(el).reverse().toString())) {
                logger.info("The validated string contains forbidden word = {} or its reverse", el);
                return false;
            }
        }

        if ((countDigits >= minDigits) && (countUpper >= minUpper) && (countSpecialChar >= minSpecialCharacter) && (countLower >= minLower)) {
            logger.info("String is valid");
            return true;
        }
        logErrorDetails(countDigits, countUpper, countLower, countSpecialChar);
        logger.info("Validation Rule not satisfied ");
        return false;
    }

    private void logErrorDetails(int countDigits, int countUpper, int countLower, int countSpecialChar) {
        if (countDigits < minDigits) {
            logger.info("Digit number {} less than minimum {}", countDigits, minDigits);
        }
        if (countUpper < minUpper) {
            logger.info("Upper number {} less than minimum {}", countUpper, minUpper);
        }
        if (countSpecialChar < minSpecialCharacter) {
            logger.info("Special Character number {} less than minimum {}", countSpecialChar, minSpecialCharacter);
        }
        if (countLower < minLower) {
            logger.info("Lower number {} less than minimum {}", countLower, minLower);
        }
    }

    private boolean validateStringLength(final String secret) {
        if (secret.length() < stringLen) {
            logger.info("Wrong string size");
            return false;
        }
        if (maxStringLen > 0 && secret.length() > maxStringLen) {
            logger.info("Wrong string size");
            return false;
        }
        return true;
    }
}
