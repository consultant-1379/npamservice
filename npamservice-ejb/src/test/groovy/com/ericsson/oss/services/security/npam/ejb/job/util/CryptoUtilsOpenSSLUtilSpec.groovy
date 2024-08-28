package com.ericsson.oss.services.security.npam.ejb.job.util


import java.lang.reflect.Constructor

import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage


class CryptoUtilsOpenSSLUtilSpec extends  CdiSpecification{



    def 'Test for encrypt method'() {
        given: 'a valid input'
        Constructor<CryptoUtilsOpenSSLImpl>  cryptoConstructor = CryptoUtilsOpenSSLImpl.class.getDeclaredConstructor();
        cryptoConstructor.setAccessible(true);
        CryptoUtilsOpenSSLImpl objUnderTest = new CryptoUtilsOpenSSLImpl()
        final byte[] strBytes = "test for encryption".getBytes();
        final InputStream targetStream = new ByteArrayInputStream(strBytes);
        when: 'call method decrypt'
        def result = objUnderTest.encrypt("myKey", targetStream,strBytes.length )
        then:
        result.toByteArray().length > 0
    }

    def 'Test for encrypt method exception thrown'() {
        given: 'a valid input'
        Constructor<CryptoUtilsOpenSSLImpl>  cryptoConstructor = CryptoUtilsOpenSSLImpl.class.getDeclaredConstructor();
        cryptoConstructor.setAccessible(true);
        CryptoUtilsOpenSSLImpl objUnderTest = new CryptoUtilsOpenSSLImpl()
        final byte[] strBytes = "test for encryption".getBytes();
        final InputStream targetStream = new ByteArrayInputStream(strBytes);
        when: 'call method decrypt'
        def result = objUnderTest.encrypt("myKey", targetStream,-60 )
        then:
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().contains(NPamRestErrorMessage.INTERNAL_SERVER_ERROR_ENCRYPT_FILE.getMessage())
    }
}
