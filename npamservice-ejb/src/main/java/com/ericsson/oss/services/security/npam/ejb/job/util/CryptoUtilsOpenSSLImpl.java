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
package com.ericsson.oss.services.security.npam.ejb.job.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.interfaces.CryptoUtilsOpenSSL;

/**
 * This class performs encryption of a content in to a file.
 */
public class CryptoUtilsOpenSSLImpl implements CryptoUtilsOpenSSL{


    private static final int INDEX_KEY = 0;
    private static final int INDEX_IV = 1;
    private static final int ITERATIONS = 1;
    private static final int SALT_SIZE = 8;
    private static final int KEY_SIZE_BITS = 256;
    private static final Charset ASCII = StandardCharsets.US_ASCII;

    /**
     * private constructor.
     */
    private CryptoUtilsOpenSSLImpl() {
        super();
    }

    /**
     * @param key_len
     *            the length of the key.
     * @param iv_len
     *            the length of the initialization vector.
     * @param md
     *            the message digest.
     * @param salt
     *            the salt.
     * @param data
     *            the secret.
     * @param count
     *            the iterations number.
     * @return a double byte array containing key and iv.
     */
    public static byte[][] EVP_BytesToKey(final int key_len, final int iv_len, final MessageDigest md, final byte[] salt, final byte[] data,
                                          final int count) {
        final byte[][] both = new byte[2][];
        final byte[] key = new byte[key_len];
        int keyIx = 0;
        final byte[] iv = new byte[iv_len];
        int ivIx = 0;
        both[0] = key;
        both[1] = iv;
        byte[] mdBuf = null;
        int nkey = key_len;
        int niv = iv_len;
        int i = 0;
        if (data == null) {
            return both;
        }
        int addmd = 0;
        for (;;) {
            md.reset();
            if (addmd++ > 0) {
                md.update(mdBuf);
            }
            md.update(data);
            if (null != salt) {
                md.update(salt, 0, 8);
            }
            mdBuf = md.digest();
            for (i = 1; i < count; i++) {
                md.reset();
                md.update(mdBuf);
                mdBuf = md.digest();
            }
            i = 0;
            if (nkey > 0) {
                for (;;) {
                    if (nkey == 0 || i == mdBuf.length) {
                        break;
                    }
                    key[keyIx++] = mdBuf[i];
                    nkey--;
                    i++;
                }
            }
            if (niv > 0 && i != mdBuf.length) {
                for (;;) {
                    if (niv == 0 || i == mdBuf.length) {
                        break;
                    }
                    iv[ivIx++] = mdBuf[i];
                    niv--;
                    i++;
                }
            }
            if (nkey == 0 && niv == 0) {
                break;
            }
        }
        for (i = 0; i < mdBuf.length; i++) {
            mdBuf[i] = 0;
        }
        return both;
    }

    @Override
    public ByteArrayOutputStream encrypt(final String password, final InputStream targetsInfo, final int size) throws NPAMRestErrorException {
        return doCrypto(password, targetsInfo, size);
    }

    private ByteArrayOutputStream doCrypto(final String password, final InputStream targetStream, final int size) throws NPAMRestErrorException {

        final SecureRandom rand = new SecureRandom();
        final byte[] salt = new byte[SALT_SIZE];
        rand.nextBytes(salt);

        try {
            final Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final MessageDigest md5 = MessageDigest.getInstance("MD5");

            // --- create key and IV  ---

            // the IV is useless, OpenSSL might as well have use zero's
            final byte[][] keyAndIV = EVP_BytesToKey(KEY_SIZE_BITS / Byte.SIZE, aesCBC.getBlockSize(), md5, salt, password.getBytes(ASCII),
                    ITERATIONS);

            final SecretKeySpec key = new SecretKeySpec(keyAndIV[INDEX_KEY], "AES");
            final IvParameterSpec iv = new IvParameterSpec(keyAndIV[INDEX_IV]);

            // --- initialize cipher instance and encrypt ---
            aesCBC.init(Cipher.ENCRYPT_MODE, key, iv);

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size + 50);

            outputStream.write("Salted__".getBytes());
            outputStream.write(salt); // salt size = 8

            final byte[] buffer = new byte[64];
            int bytesRead;
            while ((bytesRead = targetStream.read(buffer)) != -1) {
                final byte[] output = aesCBC.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }

            final byte[] outputBytes = aesCBC.doFinal();
            if (outputBytes != null) {
                outputStream.write(outputBytes);
            }
            return outputStream;
        } catch (final IOException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
                | InvalidKeyException | InvalidAlgorithmParameterException | IllegalArgumentException e) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_ENCRYPT_FILE);
        }
    }

}
