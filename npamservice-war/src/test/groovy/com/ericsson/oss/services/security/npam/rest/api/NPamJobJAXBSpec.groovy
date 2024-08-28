package com.ericsson.oss.services.security.npam.rest.api

import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult.SUCCESS
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobState.COMPLETED
import static com.ericsson.oss.services.security.npam.api.job.modelentities.JobType.DETACH_NE_ACCOUNT

import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.cds.cdi.support.spock.CdiSpecification
import com.ericsson.oss.services.security.npam.api.job.modelentities.NPamJob

class NPamJobJAXBSpec extends CdiSpecification {

    @ObjectUnderTest
    private NPAMJobJAXB objUnderTest


    def 'run getters and setters of NPamJobJAXB'() {
        when: 'execute'

        def npamJob = new NPamJob()
        Date now = new Date()
        def objUnderTest = new NPAMJobJAXB();

        objUnderTest.setName("jobName")
        objUnderTest.setState("COMPLETED")
        objUnderTest.setResult("SUCCESS")
        objUnderTest.setStartTime(now)
        objUnderTest.setEndTime(now)
        objUnderTest.setErrorDetails("errors")
        objUnderTest.setJobType(DETACH_NE_ACCOUNT)
        objUnderTest.setOwner("Administrator")
        objUnderTest.setNumberOfNetworkElements(2)
        objUnderTest.setProgressPercentage(50)
        objUnderTest.setJobInstanceId(10L)

        then : ' '
        objUnderTest.getName().equals("jobName")
        objUnderTest.getState().equals("COMPLETED")
        objUnderTest.getResult().equals("SUCCESS")
        objUnderTest.getStartTime().equals(now)
        objUnderTest.getEndTime().equals(now)
        objUnderTest.getErrorDetails().equals("errors")
        objUnderTest.getJobType().equals(DETACH_NE_ACCOUNT)
        objUnderTest.getOwner().equals("Administrator")
        objUnderTest.getNumberOfNetworkElements() == 2
        objUnderTest.getProgressPercentage() == 50
        objUnderTest.getJobInstanceId() == 10L
    }

    def 'run constructor of NPamJobJAXB'() {
        when: 'execute'
        Date now = new Date()
        def npamJob = new NPamJob()
        npamJob.setName("jobName")
        npamJob.setJobId(10L)
        npamJob.setState(COMPLETED)
        npamJob.setResult(SUCCESS)
        npamJob.setScheduledTime(now)
        npamJob.setEndTime(now)
        npamJob.setErrorDetails("errors")
        npamJob.setJobType(DETACH_NE_ACCOUNT)
        npamJob.setOwner("Administrator")
        npamJob.setNumberOfNetworkElements(2)
        npamJob.setProgressPercentage(50)

        def objUnderTest = new NPAMJobJAXB(npamJob);



        then : ' '
        objUnderTest.getName().equals("jobName")
        objUnderTest.getState().equals("COMPLETED")
        objUnderTest.getResult().equals("SUCCESS")
        objUnderTest.getEndTime().equals(now)
        objUnderTest.getErrorDetails().equals("errors")
        objUnderTest.getJobType().equals(DETACH_NE_ACCOUNT)
        objUnderTest.getOwner().equals("Administrator")
        objUnderTest.getNumberOfNetworkElements() == 2
        objUnderTest.getProgressPercentage() == 50
        objUnderTest.getJobInstanceId() == 10L
    }
}
