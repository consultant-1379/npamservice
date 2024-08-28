package com.ericsson.oss.services.security.npam.ejb.listener

import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_FILENAME
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_PASSWORD
import static com.ericsson.oss.services.security.npam.api.constants.JobPropertyConstants.PK_NEXT_USERNAME
import static com.ericsson.oss.services.security.npam.ejb.testutil.JMSHelper.createEventForObject

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.SpyImplementation
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.itpf.sdk.eventbus.Channel
import com.ericsson.oss.itpf.sdk.eventbus.Event
import com.ericsson.oss.services.security.npam.api.exceptions.JobError
import com.ericsson.oss.services.security.npam.api.exceptions.JobExceptionFactory
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.api.message.*
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.util.FileResource
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import com.ericsson.oss.services.security.npam.ejb.utility.NetworkUtil

import spock.lang.Unroll

class NodePamQueueListenerForJobExecutionSpec extends BaseSetupForTestSpecs {

    @Inject
    NodePamQueueListener objUnderTest

    @Inject
    private Channel mockedNodePamRequestQueueChannel


    @Inject
    DpsQueryUtil dpsQueryUtil

    def mockedEvent = Mock(Event)

    @SpyImplementation    //this annotation allow to spy real object exection
    JobExceptionFactory jobExceptionFactory

    @MockedImplementation
    NetworkUtil networkUtilMock;

    @MockedImplementation
    FileResource fileResourceMock;

    @ImplementationClasses
    def classes = []

    def userId = 'userId'
    def requestId = 'requestId'
    def nodes = ['RadioNode1', 'RadioNode2']
    def collections = ['collection1']
    def savedSearches = ['savedsearch1']


    PersistenceObject mainJobPO
    PersistenceObject jobTemplatePO

    def templateName = "name"
    def owner = "owner"
    def NO_SCHEDULE = null

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    def 'receive NodePamSubmitMainJobRequest with invalid jobId=-1 do nothing (mark job result FAILED)'() {
        given: "invalid jobId"
        def jobId= -1
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(jobId)
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NOT_FOUND_JOB_ID,_)
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        and: 'NPamJob PO'
        dpsQueryUtil.findPersistentObject(jobId) == null
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest with job with invalid JobState=#jobState do nothing (mark job result FAILED)'(jobState) {
        given: 'NPamJob configured'
        mainJobPO = addNpamJobWithNodes(0, owner, JobType.CREATE_NE_ACCOUNT, jobState, null, nodes, collections, savedSearches)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_INVALID_STATE_FOR_JOB, _)
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        where:
        jobState         || _
        JobState.CREATED || _
        JobState.SCHEDULED || _
        JobState.RUNNING || _
        JobState.COMPLETED || _
        JobState.USER_CANCELLED || _
    }

    /*
     Section Job CREATE_NE_ACCOUNT
     * */

    def 'receive NodePamSubmitMainJobRequest(CREATE_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo returns EMPTY list of nodes do nothing (mark job result FAILED)'() {
        given: 'NPamJobTemplate and NPamJob configured with empty list of nodes'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.CREATE_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.CREATE_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns EMPTY list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>()
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(CREATE_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo throw exception do nothing (mark job result FAILED)'(exception) {
        given: 'NPamJobTemplate and NPamJob configured with invalid collection'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.CREATE_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.CREATE_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo throws Exception'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> {throw exception}
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("NetworkElement not found")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        where:
        exception                                     || _
        new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,"message")         || _
        new Exception("NetworkElement not found")                      || _
    }


    def 'receive NodePamSubmitMainJobRequest(CREATE_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo returns valid list of nodes send NodePamEnableRemoteManagementRequest(s) to queue'() {
        given: 'NPamJobTemplate and NPamJob configured'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.CREATE_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.CREATE_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamNEJob PO created/modified'
        def npamNeJobList = dpsQueryUtil.getNeJobsConnectedToMainJob(mainJobPO.getPoId())
        npamNeJobList.size() == 2

        npamNeJobList.get(0).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(0).allAttributes.get("result") == null
        npamNeJobList.get(0).allAttributes.get("startTime") == null
        npamNeJobList.get(0).allAttributes.get("endTime") == null
        npamNeJobList.get(0).allAttributes.get("creationTime") != null
        npamNeJobList.get(0).allAttributes.get("neName") == "RadioNode2"
        npamNeJobList.get(0).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(0).allAttributes.get("errorDetails") == null

        npamNeJobList.get(1).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(1).allAttributes.get("result") == null
        npamNeJobList.get(1).allAttributes.get("startTime") == null
        npamNeJobList.get(1).allAttributes.get("endTime") == null
        npamNeJobList.get(1).allAttributes.get("creationTime") != null
        npamNeJobList.get(1).allAttributes.get("neName") == "RadioNode1"
        npamNeJobList.get(1).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(1).allAttributes.get("errorDetails") == null

        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("numberOfNetworkElements") == 2
        po.allAttributes.get("errorDetails") == null
        and: 'events are sent to queue'
        2 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamEnableRemoteManagementRequest request = (NodePamEnableRemoteManagementRequest)it
            assert (request.nodeName == "RadioNode1" || request.nodeName == "RadioNode2")
            assert request.neJobId != 0
            assert request.retryNumber == 0
            return true
        }, _) >> mockedEvent
        2 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }


    /*
     Section Job DETACH_NE_ACCOUNT
     * */

    def 'receive NodePamSubmitMainJobRequest(DETACH_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo returns EMPTY list of nodes do nothing (mark job result FAILED)'() {
        given: 'NPamJobTemplate and NPamJob configured with empty list of nodes'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.DETACH_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.DETACH_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns EMPTY list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>()
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(DETACH_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo throw exception do nothing (mark job result FAILED)'(exception) {
        given: 'NPamJobTemplate and NPamJob configured with invalid collection'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.DETACH_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.DETACH_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo throws Exception'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> {throw exception}
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("NetworkElement not found")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        where:
        exception                                     || _
        new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,"message")         || _
        new Exception("NetworkElement not found")                      || _
    }

    def 'receive NodePamSubmitMainJobRequest(DETACH_NE_ACCOUNT) job when getAllNetworkElementFromNeInfo returns valid list of nodes send NodePamEnableRemoteManagementRequest(s) to queue'() {
        given: 'NPamJobTemplate and NPamJob configured'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.DETACH_NE_ACCOUNT,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.DETACH_NE_ACCOUNT, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamNEJob PO created/modified'
        def npamNeJobList = dpsQueryUtil.getNeJobsConnectedToMainJob(mainJobPO.getPoId())
        npamNeJobList.size() == 2

        npamNeJobList.get(0).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(0).allAttributes.get("result") == null
        npamNeJobList.get(0).allAttributes.get("startTime") == null
        npamNeJobList.get(0).allAttributes.get("endTime") == null
        npamNeJobList.get(0).allAttributes.get("creationTime") != null
        npamNeJobList.get(0).allAttributes.get("neName") == "RadioNode2"
        npamNeJobList.get(0).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(0).allAttributes.get("errorDetails") == null

        npamNeJobList.get(1).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(1).allAttributes.get("result") == null
        npamNeJobList.get(1).allAttributes.get("startTime") == null
        npamNeJobList.get(1).allAttributes.get("endTime") == null
        npamNeJobList.get(1).allAttributes.get("creationTime") != null
        npamNeJobList.get(1).allAttributes.get("neName") == "RadioNode1"
        npamNeJobList.get(1).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(1).allAttributes.get("errorDetails") == null

        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("numberOfNetworkElements") == 2
        po.allAttributes.get("errorDetails") == null
        and: 'events are sent to queue'
        2 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamDisableRemoteManagementRequest request = (NodePamDisableRemoteManagementRequest)it
            assert (request.nodeName == "RadioNode1" || request.nodeName == "RadioNode2")
            assert request.neJobId != 0
            assert request.retryNumber == 0
            return true
        }, _) >> mockedEvent
        2 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    /*
     Section Job ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED
     * */
    //THIS UC WILL BE DEVELOPED UFFICIALLY IN FUTURE SPRINT BUT WE ADD ALL VALID TESTS
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED) job when getAllNetworkElementFromNeInfo returns EMPTY list of nodes do nothing (mark job result FAILED)'() {
        given: 'NPamJobTemplate and NPamJob configured with empty list of nodes'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns EMPTY list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>()
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED) job when getAllNetworkElementFromNeInfo throw exception do nothing (mark job result FAILED)'(exception) {
        given: 'NPamJobTemplate and NPamJob configured with invalid collection'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo throws Exception'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> {throw exception}
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("NetworkElement not found")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        where:
        exception                                     || _
        new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,"message")         || _
        new Exception("NetworkElement not found")                      || _
    }

    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED) job when getAllNetworkElementFromNeInfo returns valid list of nodes send NodePamEnableRemoteManagementRequest(s) to queue'() {
        given: 'NPamJobTemplate and NPamJob configured'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_AUTOGENERATED, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamNEJob PO created/modified'
        def npamNeJobList = dpsQueryUtil.getNeJobsConnectedToMainJob(mainJobPO.getPoId())
        npamNeJobList.size() == 2

        npamNeJobList.get(0).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(0).allAttributes.get("result") == null
        npamNeJobList.get(0).allAttributes.get("startTime") == null
        npamNeJobList.get(0).allAttributes.get("endTime") == null
        npamNeJobList.get(0).allAttributes.get("creationTime") != null
        npamNeJobList.get(0).allAttributes.get("neName") == "RadioNode2"
        npamNeJobList.get(0).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(0).allAttributes.get("errorDetails") == null

        npamNeJobList.get(1).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(1).allAttributes.get("result") == null
        npamNeJobList.get(1).allAttributes.get("startTime") == null
        npamNeJobList.get(1).allAttributes.get("endTime") == null
        npamNeJobList.get(1).allAttributes.get("creationTime") != null
        npamNeJobList.get(1).allAttributes.get("neName") == "RadioNode1"
        npamNeJobList.get(1).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(1).allAttributes.get("errorDetails") == null

        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("numberOfNetworkElements") == 2
        po.allAttributes.get("errorDetails") == null
        and: 'events are sent to queue'
        2 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamAutogeneratePasswordRequest request = (NodePamAutogeneratePasswordRequest)it
            assert (request.nodeName == "RadioNode1" || request.nodeName == "RadioNode2")
            assert request.neJobId != 0
            assert request.retryNumber == 0
            return true
        }, _) >> mockedEvent
        2 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    /*
     Section Job ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE
     * */
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) job when getAllNetworkElementFromNeInfo returns EMPTY list of nodes do nothing (mark job result FAILED)'() {
        given: 'valid credentials for RadioNode1'
        fileResourceMock.getFilenameAfterJob(_) >> "filename1"
        fileResourceMock.readCredentialsFromFile("filename1") >> createCredentialForNode("RadioNode1")
        and: 'NPamJobTemplate and NPamJob configured with empty list of nodes'
        JobProperty jobProperty = new JobProperty(PK_FILENAME, "filename1")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(jobProperty)
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE,jobProperties, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, JobState.SUBMITTED, jobProperties, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns EMPTY list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>()
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) job when getAllNetworkElementFromNeInfo throw exception do nothing (mark job result FAILED)'(exception) {
        given: 'valid credentials for RadioNode1'
        fileResourceMock.getFilenameAfterJob(_) >> "filename1"
        fileResourceMock.readCredentialsFromFile("filename1") >> createCredentialForNode("RadioNode1")
        and: 'NPamJobTemplate and NPamJob configured with invalid collection'
        JobProperty jobProperty = new JobProperty(PK_FILENAME, "filename1")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(jobProperty)
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE,jobProperties, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, JobState.SUBMITTED, jobProperties, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo throws Exception'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> {throw exception}
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("NetworkElement not found")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        where:
        exception                                     || _
        new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,"message")         || _
        new Exception("NetworkElement not found")                      || _
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) job when readCredentialsFromFile return empty list do nothing (mark job result FAILED)'() {
        given: 'empty credentials for nodes'
        fileResourceMock.getFilenameAfterJob(_) >> "filename1"
        fileResourceMock.readCredentialsFromFile("filename1") >> new HashMap<String, List<String>>()
        and: 'NPamJobTemplate and NPamJob configured'
        JobProperty jobProperty = new JobProperty(PK_FILENAME, "filename1")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(jobProperty)
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE,jobProperties, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, JobState.SUBMITTED, jobProperties, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_CREDENTIALS_FILE_EMPTY, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("credentials")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE) job when getAllNetworkElementFromNeInfo returns valid list of nodes send NodePamEnableRemoteManagementRequest(s) to queue'() {
        given: 'valid credentials for RadioNode1'
        fileResourceMock.getFilenameAfterJob(_) >> "filename1"
        fileResourceMock.readCredentialsFromFile("filename1") >> createCredentialForNode("RadioNode1")
        and: 'NPamJobTemplate and NPamJob configured'
        JobProperty jobProperty = new JobProperty(PK_FILENAME, "filename1")
        List<JobProperty> jobProperties = new ArrayList<>()
        jobProperties.add(jobProperty)
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, jobProperties, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE, JobState.SUBMITTED, jobProperties, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamNEJob PO created/modified'
        def npamNeJobList = dpsQueryUtil.getNeJobsConnectedToMainJob(mainJobPO.getPoId())
        npamNeJobList.size() == 2

        npamNeJobList.get(0).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(0).allAttributes.get("result") == null
        npamNeJobList.get(0).allAttributes.get("startTime") == null
        npamNeJobList.get(0).allAttributes.get("endTime") == null
        npamNeJobList.get(0).allAttributes.get("creationTime") != null
        npamNeJobList.get(0).allAttributes.get("neName") == "RadioNode2"
        npamNeJobList.get(0).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(0).allAttributes.get("errorDetails") == null

        npamNeJobList.get(1).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(1).allAttributes.get("result") == null
        npamNeJobList.get(1).allAttributes.get("startTime") == null
        npamNeJobList.get(1).allAttributes.get("endTime") == null
        npamNeJobList.get(1).allAttributes.get("creationTime") != null
        npamNeJobList.get(1).allAttributes.get("neName") == "RadioNode1"
        npamNeJobList.get(1).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(1).allAttributes.get("errorDetails") == null

        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("numberOfNetworkElements") == 2
        po.allAttributes.get("errorDetails") == null
        and: 'events are sent to queue'
        2 * mockedNodePamRequestQueueChannel.createEvent({
            NodePamUpdatePasswordRequest request = (NodePamUpdatePasswordRequest)it
            assert (request.nodeName == "RadioNode1" || request.nodeName == "RadioNode2")
            assert request.neJobId != 0
            assert request.retryNumber == 0
            if (request.nodeName == "RadioNode1") {
                assert request.nextUser != null
                assert request.nextPasswd != null
            }
            if (request.nodeName == "RadioNode2") {
                assert request.nextUser == null
                assert request.nextPasswd == null
            }
            return true
        }, _) >> mockedEvent
        2 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }

    /*
     Section Job ROTATE_NE_ACCOUNT_CREDENTIALS
     * */
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS) job when getAllNetworkElementFromNeInfo returns EMPTY list of nodes do nothing (mark job result FAILED)'() {
        given: 'NPamJobTemplate and NPamJob configured with empty list of nodes'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns EMPTY list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>()
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'internally raised exception'
        1 * jobExceptionFactory.createJobConfigurationException(JobError.CONFIGURATION_ERROR_NO_NODES, _)
        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails") != null
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
    }

    @Unroll
    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS) job when getAllNetworkElementFromNeInfo throw exception do nothing (mark job result FAILED)'(exception) {
        given: 'NPamJobTemplate and NPamJob configured with invalid collection'
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS,null, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED, null, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo throws Exception'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> {throw exception}
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.COMPLETED.getJobStateName()
        po.allAttributes.get("result") == JobResult.FAILED.getJobResult()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("endTime") != null
        po.allAttributes.get("progressPercentage") == 100.0
        po.allAttributes.get("errorDetails").toString().contains("NetworkElement not found")
        and:
        0 * mockedNodePamRequestQueueChannel.createEvent(*_)
        0 * mockedNodePamRequestQueueChannel.send(*_)
        where:
        exception                                     || _
        new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,"message")         || _
        new Exception("NetworkElement not found")                      || _
    }

    def 'receive NodePamSubmitMainJobRequest(ROTATE_NE_ACCOUNT_CREDENTIALS) job when getAllNetworkElementFromNeInfo returns valid list of nodes send NodePamEnableRemoteManagementRequest(s) to queue'() {
        given: 'NPamJobTemplate and NPamJob configured'
        List<JobProperty> jobProperties = new ArrayList<>()
        JobProperty jobProperty1 = new JobProperty(PK_NEXT_USERNAME, "nextUserName")
        JobProperty jobProperty2 = new JobProperty(PK_NEXT_PASSWORD, "nextPassword")
        jobProperties.add(jobProperty1)
        jobProperties.add(jobProperty2)
        jobTemplatePO= addNpamJobTemplate(templateName,  owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, jobProperties, NO_SCHEDULE, nodes, collections, savedSearches)
        mainJobPO = addNpamJobWithNodes(jobTemplatePO.getPoId(), owner, JobType.ROTATE_NE_ACCOUNT_CREDENTIALS, JobState.SUBMITTED, jobProperties, nodes, collections, savedSearches)
        and: 'getAllNetworkElementFromNeInfo returns list of nodes'
        networkUtilMock.getAllNetworkElementFromNeInfo(_, owner, true) >> new HashSet<String>(nodes)
        and: 'the event is created'
        NodePamSubmitMainJobRequest nodePamSubmitMainJobRequest = new NodePamSubmitMainJobRequest(mainJobPO.getPoId())
        when: 'the event is consumed'
        objUnderTest.receiveSubmitMainJobRequest(createEventForObject(requestId, nodePamSubmitMainJobRequest))
        then: 'NPamNEJob PO created/modified'
        def npamNeJobList = dpsQueryUtil.getNeJobsConnectedToMainJob(mainJobPO.getPoId())
        npamNeJobList.size() == 2

        npamNeJobList.get(0).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(0).allAttributes.get("result") == null
        npamNeJobList.get(0).allAttributes.get("startTime") == null
        npamNeJobList.get(0).allAttributes.get("endTime") == null
        npamNeJobList.get(0).allAttributes.get("creationTime") != null
        npamNeJobList.get(0).allAttributes.get("neName") == "RadioNode2"
        npamNeJobList.get(0).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(0).allAttributes.get("errorDetails") == null

        npamNeJobList.get(1).allAttributes.get("state") == JobState.CREATED.getJobStateName()
        npamNeJobList.get(1).allAttributes.get("result") == null
        npamNeJobList.get(1).allAttributes.get("startTime") == null
        npamNeJobList.get(1).allAttributes.get("endTime") == null
        npamNeJobList.get(1).allAttributes.get("creationTime") != null
        npamNeJobList.get(1).allAttributes.get("neName") == "RadioNode1"
        npamNeJobList.get(1).allAttributes.get("mainJobId") == mainJobPO.getPoId()
        npamNeJobList.get(1).allAttributes.get("errorDetails") == null

        and: 'NPamJob PO modified'
        def po = dpsQueryUtil.findPersistentObject(mainJobPO.getPoId())
        po.allAttributes.get("state") == JobState.RUNNING.getJobStateName()
        po.allAttributes.get("startTime") != null
        po.allAttributes.get("numberOfNetworkElements") == 2
        po.allAttributes.get("errorDetails") == null
        and: 'events are sent to queue'
        2 * mockedNodePamRequestQueueChannel.createEvent({

            NodePamUpdatePasswordRequest request = (NodePamUpdatePasswordRequest)it
            assert (request.nodeName == "RadioNode1" || request.nodeName == "RadioNode2")
            assert request.neJobId != 0
            assert request.retryNumber == 0
            assert request.nextUser != null
            assert request.nextPasswd != null
            return true
        }, _) >> mockedEvent
        2 * mockedNodePamRequestQueueChannel.send(mockedEvent, _)
    }


    HashMap<String, List<String>> createCredentialForNode(String nodeName1) {
        HashMap<String, List<String>> credentialsFromFile = new HashMap()
        final List<String> credentials1 = new ArrayList<>()
        credentials1.add("userName")
        credentials1.add("passWord")
        credentialsFromFile.put(nodeName1, credentials1)
        return credentialsFromFile
    }
}
