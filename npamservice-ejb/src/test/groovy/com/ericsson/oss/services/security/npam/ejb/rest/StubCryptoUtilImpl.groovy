/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2016
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.services.security.npam.ejb.rest

import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.interfaces.CryptoUtilsOpenSSL

class StubCryptoUtilImpl implements CryptoUtilsOpenSSL{

    @Override
    public ByteArrayOutputStream encrypt(String password, InputStream targetsInfo, int size) throws NPAMRestErrorException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size + 50);

        outputStream.write("Salted__".getBytes());

        final byte[] buffer = new byte[64];

        int nRead;

        while ((nRead = targetsInfo.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, nRead);
        }


        return outputStream;
    }
}
