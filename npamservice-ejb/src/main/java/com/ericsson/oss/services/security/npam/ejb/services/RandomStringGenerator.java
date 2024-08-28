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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RandomStringGenerator {
    private static final Logger logger = LoggerFactory.getLogger(RandomStringGenerator.class);
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private int minLower;
    private int minUpper;
    private int minDigits;
    private int minSpecialCharacter;
    private int stringLen;
    private String specialCharacters;
    private List<String> dictionaryForbidden;

    private RandomStringGenerator() {
        throw new UnsupportedOperationException("Empty constructor is not supported.");
    }

    private RandomStringGenerator(final RandomStringGeneratorBuilder builder) {
        this.minLower = builder.minLower;
        this.minUpper = builder.minUpper;
        this.minDigits = builder.minDigits;
        this.minSpecialCharacter = builder.minSpecialCharacter;
        this.stringLen = builder.stringLen;
        this.specialCharacters = builder.specialCharacters;
        this.dictionaryForbidden = builder.dictionaryForbidden;
    }

    public static class RandomStringGeneratorBuilder {
        private int minLower;
        private int minUpper;
        private int minDigits;
        private int minSpecialCharacter;
        private int stringLen;
        private String specialCharacters;
        private final List<String> dictionaryForbidden;

        public RandomStringGeneratorBuilder() {
            this.minLower = 0;
            this.minUpper = 0;
            this.minDigits = 0;
            this.minSpecialCharacter = 0;
            this.stringLen = 8;
            this.specialCharacters = "";
            this.dictionaryForbidden = new ArrayList<>();
        }

        public RandomStringGeneratorBuilder minUpper(final int minUpper) {
            if (minUpper >= 0) {
                this.minUpper = minUpper;
            } else {
                throw new IllegalArgumentException("minUpper value should be greater or equal to 0");
            }
            return this;
        }

        public RandomStringGeneratorBuilder minLower(final int minLower) {
            if (minLower >= 0) {
                this.minLower = minLower;
            } else {
                throw new IllegalArgumentException("minLower value should be greater or equal to 0");
            }
            return this;
        }

        public RandomStringGeneratorBuilder minDigits(final int minDigits) {
            if (minDigits >= 0) {
                this.minDigits = minDigits;
            } else {
                throw new IllegalArgumentException("minDigits value should be greater or equal to 0");
            }
            return this;
        }

        public RandomStringGeneratorBuilder minSpecialCharacter(final int minSpecialCharacter) {
            if (minSpecialCharacter >= 0) {
                this.minSpecialCharacter = minSpecialCharacter;
            } else {
                throw new IllegalArgumentException("minSpecialCharacter value should be greater or equal to 0");
            }
            return this;
        }

        public RandomStringGeneratorBuilder stringLen(final int stringLen) {
            if (stringLen > 0) {
                this.stringLen = stringLen;
            } else {
                throw new IllegalArgumentException("stringLen value should be greater than 0");
            }
            return this;
        }

        public RandomStringGeneratorBuilder specialCharacters(final String specialChar) {
            this.specialCharacters = specialChar;
            return this;
        }

        public RandomStringGeneratorBuilder forbiddenWords(final List<String> words) {
            if (words != null) {
                this.dictionaryForbidden.addAll(words);
            }
            return this;
        }

        public RandomStringGenerator build() {
            final int totMin = minLower + minUpper + minDigits + minSpecialCharacter;
            if (totMin > stringLen) {
                throw new UnsupportedOperationException("Insufficient configured lenght");
            }
            return new RandomStringGenerator(this);
        }
    }

    public String generateSecret() {
        String secretShuffled = "";
        do {

            final SecureRandom random = new SecureRandom();
            final StringBuilder secretGenerated = new StringBuilder();
            for (int i = 0; i < minLower; i++) {
                secretGenerated.append(LOWER.charAt(random.nextInt(LOWER.length())));
            }
            for (int i = 0; i < minUpper; i++) {
                secretGenerated.append(UPPER.charAt(random.nextInt(UPPER.length())));
            }
            for (int i = 0; i < minDigits; i++) {
                secretGenerated.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
            }
            for (int i = 0; i < minSpecialCharacter; i++) {
                secretGenerated.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));
            }

            final int diffLenght = (stringLen - secretGenerated.length());
            final String allRules = LOWER + UPPER + DIGITS + specialCharacters;
            for (int k = 0; k < diffLenght; k++) {
                secretGenerated.append(allRules.charAt(random.nextInt(allRules.length())));
            }
            secretShuffled = shuffle(random, secretGenerated.toString());

        } while (!checkValidation(secretShuffled));

        return secretShuffled;
    }

    private String shuffle(final Random random, final String secretGenerated) {
        final List<String> secretCharList = Arrays.asList(secretGenerated.split(""));
        Collections.shuffle(secretCharList, random);

        return String.join("", secretCharList);
    }

    private boolean checkValidation(final String secretShuffled) {
        for (final String el : dictionaryForbidden) {
            if (secretShuffled.contains(el)) {
                logger.info("secret with word forbidden");
                return false;
            }
        }
        return true;
    }

}
