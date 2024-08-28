package com.ericsson.oss.services.security.npam.ejb.utility

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs

class NetworkUtilWithoutNESpec extends BaseSetupForTestSpecs {

    @Inject
    NetworkUtil objUnderTest

    @MockedImplementation
    TbacEvaluation tbacEvaluationMock;

    @MockedImplementation
    DataPersistenceService dataPersistenceServiceMock

    final String userId = 'administrator'

    final List<String> emptyList = new ArrayList<>();
    final List<String> validNeNamesList = new ArrayList<>(1)

    /*
     *   Ne Names section
     * */
    def 'Call getAllNetworkElementFromNeInfo with valid ne names list -> recover NE names'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> true
        dataPersistenceServiceMock.getQueryBuilder() >> { throw new Exception("Error") }

        and: ''
        validNeNamesList.add("node1")

        NEInfo neInfo = new NEInfo();
        neInfo.setNeNames(validNeNamesList)
        neInfo.setSavedSearchIds(emptyList)
        neInfo.setCollectionNames(emptyList)
        when: 'getAllNetworkElementFromNeInfo'
        def neNameList = objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception'
        NPAMRestErrorException ex = thrown()
    }
}
