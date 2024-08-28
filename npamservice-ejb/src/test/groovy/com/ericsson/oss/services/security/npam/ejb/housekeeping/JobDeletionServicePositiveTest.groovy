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
package com.ericsson.oss.services.security.npam.ejb.housekeeping

import com.ericsson.cds.cdi.support.rule.ImplementationClasses
import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.cds.cdi.support.rule.ObjectUnderTest
import com.ericsson.oss.itpf.datalayer.dps.persistence.PersistenceObject
import com.ericsson.oss.services.security.npam.api.constants.NodePamConstantsGetter
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.security.npam.ejb.job.housekeeping.JobsHouseKeepingService
import com.ericsson.oss.services.security.npam.ejb.job.impl.JobConfigurationServiceImpl
import com.ericsson.oss.services.security.npam.ejb.pib.NpamPibParameters
import com.ericsson.oss.services.security.npam.ejb.testutil.DpsQueryUtil
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime
import spock.lang.Unroll

import java.util.concurrent.TimeUnit
import javax.inject.Inject

class JobDeletionServicePositiveTest extends BaseSetupForTestSpecs{

    @Inject
    DpsQueryUtil dpsQueryUtil
    
    @ObjectUnderTest
    private JobsHouseKeepingService objectUnderTest;

    @MockedImplementation
    NodePamConstantsGetter nodePamConstantsGetter

    @ImplementationClasses
    def classes = [JobConfigurationServiceImpl]

    def nodes = Arrays.asList('RadioNode01', 'RadioNode02')
    def owner = "owner"

    def schedule = new HashMap<String, Object>()

    long jobTemplatePoId = 0
    long jobMainPoId = 0
    long jobNEPoId1 = 0
    long jobNEPoId2 = 0

    String filepath = "/tmp/importFile.csv"
    String filesHKDir = "/tmp/housekeepingTestDir/"

    Date templateCreationDate = new DateTime().minusDays(200).toDate()
    Date mainJobEndDate = new DateTime().minusDays(190).toDate()
    Date mainJobScheduledDate = new DateTime().minusDays(185).toDate()
    JobType jobType_UpdatePassword = JobType.ROTATE_NE_ACCOUNT_CREDENTIALS;

    int maxHKDate = 180

    def setupForTest(JobState jobState) {
        List<JobProperty> jobPropertyList = new ArrayList<JobProperty>();
        jobPropertyList.add(new JobProperty("USERNAME", "username"))
        PersistenceObject jobTemplatePO = addNpamJobTemplateForHousekeeping(
                "jobTemplateName",
                "owner",
                jobType_UpdatePassword,
                jobPropertyList,
                schedule,
                nodes,
                new ArrayList<String>(),
                new ArrayList<>(),
                templateCreationDate)
        PersistenceObject mainJobPO = addNpamJobWithNodesForHousekeeping(
                jobTemplatePO.getPoId(),
                owner,
                jobType_UpdatePassword,
                jobState,
                jobPropertyList,
                nodes,
                new ArrayList<>(),
                new ArrayList<>(),
                mainJobScheduledDate,
                mainJobEndDate,
                "jobTemplateName"
        )

        jobTemplatePoId = jobTemplatePO.getPoId()
        jobMainPoId = mainJobPO.getPoId()
        if (jobState != JobState.USER_CANCELLED)
        {
            PersistenceObject npamNeJob1 = addNpamNeJob(jobMainPoId, JobState.COMPLETED, JobResult.SUCCESS, "RadioNode01")
            PersistenceObject npamNeJob2 = addNpamNeJob(jobMainPoId, JobState.COMPLETED, JobResult.SUCCESS, "RadioNode02")
            jobNEPoId1 = npamNeJob1.getPoId()
            jobNEPoId2 = npamNeJob2.getPoId()
        }
    }

    def setupForImportScenario(String filename) {
        List<JobProperty> jobPropertyList = new ArrayList<JobProperty>();
        jobPropertyList.add(new JobProperty("FILENAME", filename))
        PersistenceObject jobTemplatePO = addNpamJobTemplateForHousekeeping(
                "nameForImport",
                "owner",
                JobType.ROTATE_NE_ACCOUNT_CREDENTIALS_FROM_FILE,
                jobPropertyList,
                schedule,
                nodes,
                new ArrayList<String>(),
                new ArrayList<>(),
                templateCreationDate)
        PersistenceObject mainJobPO = addNpamJobWithNodesForHousekeeping(
                jobTemplatePO.getPoId(),
                owner,
                jobType_UpdatePassword,
                JobState.COMPLETED,
                jobPropertyList,
                nodes,
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                mainJobEndDate,
                "nameForImport"
        )

        jobTemplatePoId = jobTemplatePO.getPoId()
        jobMainPoId = mainJobPO.getPoId()
        PersistenceObject npamNeJob1 = addNpamNeJob(jobMainPoId, JobState.COMPLETED, JobResult.SUCCESS, "RadioNode01")
        PersistenceObject npamNeJob2 = addNpamNeJob(jobMainPoId, JobState.USER_CANCELLED, JobResult.SUCCESS, "RadioNode02")
        jobNEPoId1 = npamNeJob1.getPoId()
        jobNEPoId2 = npamNeJob2.getPoId()

        // create import file
        def importFile = new File(filepath)
        importFile.createNewFile()

        // mock for getting import file location
        nodePamConstantsGetter.getImportFolderJob() >> "/tmp/"
    }

    def setupForFileHousekeeping() {
        File housekeepingDir = new File(filesHKDir)
        if (housekeepingDir.exists()) {
            FileUtils.forceDelete(housekeepingDir)
        }
    }
  
    boolean endForFileHousekeeping() {
        File housekeepingDir = new File(filesHKDir)
        FileUtils.forceDelete(housekeepingDir)
        return true
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM job available in Database" () {
        given: "NPAM scheduled job is persisted as per import scenario"
        setupForTest(JobState.COMPLETED)
        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePoId) == null)
        assert(dpsQueryUtil.findPersistentObject(jobMainPoId) == null)
        assert(dpsQueryUtil.findPersistentObject(jobNEPoId1) == null)
        assert(dpsQueryUtil.findPersistentObject(jobNEPoId2) == null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM job in USER_CANCELLED state in Database" () {
        given: "a job in USER_CANCELLED state"
        setupForTest(JobState.USER_CANCELLED)
        when: "NPAM scheduled job is persisted as per multiple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobMainPoId) == null)
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePoId) == null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM job in SCHEDULED state in Database" () {
        given: "a job in SCHEDULED state"
        setupForTest(JobState.SCHEDULED)
        runtimeDps.stubbedDps.liveBucket.deletePo(dpsQueryUtil.findPersistentObject(jobNEPoId2))
        PersistenceObject npamNeJobscheduled = addNpamNeJob(jobMainPoId, JobState.RUNNING, JobResult.SUCCESS, "RadioNode02")
        when: "NPAM scheduled job is persisted as per mult++iple scenarios"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
        assert(dpsQueryUtil.findPersistentObject(jobTemplatePoId) != null)
        assert(dpsQueryUtil.findPersistentObject(jobMainPoId) != null)
        assert(dpsQueryUtil.findPersistentObject(jobNEPoId1) != null)
        assert(dpsQueryUtil.findPersistentObject(npamNeJobscheduled.getPoId()) != null)
    }

    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM job related to an import file" () {
        when: "NPAM scheduled job is persisted as per import scenario"
            setupForImportScenario("importFile.csv")
        and: "triggering housekeeping process"
            objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
            assert(dpsQueryUtil.findPersistentObject(jobTemplatePoId) == null)
            assert(dpsQueryUtil.findPersistentObject(jobMainPoId) == null)
            assert(dpsQueryUtil.findPersistentObject(jobNEPoId1) == null)
            assert(dpsQueryUtil.findPersistentObject(jobNEPoId2) == null)
        and: "import file should be successfully deleted"
            !new File(filepath).exists()
    }

    @Unroll
    def "Test triggerHouseKeepingOfJobsStartingFromTemplates() when there is NPAM job related to a wrong import file" (file) {
        given: "NPAM scheduled job is persisted as per import scenario"
        setupForImportScenario(file)
        when: "triggering housekeeping process"
        objectUnderTest.triggerHouseKeepingOfJobsStartingFromTemplates(maxHKDate);
        then:"All objects related to job should get deleted from database successfully"
            assert(dpsQueryUtil.findPersistentObject(jobTemplatePoId) == null)
            assert(dpsQueryUtil.findPersistentObject(jobMainPoId) == null)
            assert(dpsQueryUtil.findPersistentObject(jobNEPoId1) == null)
            assert(dpsQueryUtil.findPersistentObject(jobNEPoId2) == null)
        and: "import file should be successfully deleted"
        new File(filepath).exists()
        where:
        file                    |_
        "/tmp/importFile1.csv"  |_
        null                    |_
        ""                      |_
    }

    /** Housekeeping for Imported files */

    def "Test triggerHouseKeepingOfFiles where files are correctly deleted" () {
        setupForFileHousekeeping()
        given: "files exist in import directory"
            nodePamConstantsGetter.getImportFolder() >> filesHKDir
            def importFolderStr = nodePamConstantsGetter.getImportFolder()
            File importDir = new File(importFolderStr)
            importDir.mkdir()
            def filepath1 = importFolderStr + "importFile1.csv"
            def filepath2 = importFolderStr + "importFile2.txt"
            File file1 = new File(filepath1)
            File file2 = new File(filepath2)
            file1.createNewFile()
            file2.createNewFile()
        and: "files are older than npamPibParameters.getNpamHouseKeepingDays() days"
            long deletionDateInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxHKDate) - 1000
            // update last modified date in files
            file1.setLastModified(deletionDateInMillis)
            file2.setLastModified(deletionDateInMillis)
        when: "housekeeping for files is triggered"
            objectUnderTest.triggerHouseKeepingOfFiles(maxHKDate)
        then: "files have been deleted"
            !file1.exists()
            !file2.exists()
            endForFileHousekeeping()
    }

    def "Test triggerHouseKeepingOfFiles for coverage when import dir does not exist" () {
        setupForFileHousekeeping()
        given: "a not existent directory"
            String notExistentDir =  "/tmp/housekeepingNotExistentDir"
            nodePamConstantsGetter.getImportFolder() >> notExistentDir
            File dir = new File(notExistentDir)
            assert !dir.exists()
        when: "files exist in import directory"
            def importFolder = "/tmp/housekeepingTestDir"
            new File(importFolder).mkdir()
            def filepath1 = importFolder + "/importFile1.csv"
            def filepath2 = importFolder + "/importFile2.txt"
            File file1 = new File(filepath1)
            File file2 = new File(filepath2)
            file1.createNewFile()
            file2.createNewFile()
        and: "files are older than npamPibParameters.getNpamHouseKeepingDays() days"
            long deletionDateInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxHKDate) - 1000
            // update last modified date in files
            file1.setLastModified(deletionDateInMillis)
            file2.setLastModified(deletionDateInMillis)
        and: "housekeeping for files is triggered"
            objectUnderTest.triggerHouseKeepingOfFiles(maxHKDate)
        then: "import directory used by housekeeping service does not exist"
            nodePamConstantsGetter.getImportFolder() >> "/tmp/housekeepingWrongTestDir"
        then: "files are not deleted"
            file1.exists()
            file2.exists()
            endForFileHousekeeping()
    }

    def "Test triggerHouseKeepingOfFiles where files are not deletable" () {
        setupForFileHousekeeping()
        given: "files exist in import directory"
            nodePamConstantsGetter.getImportFolder() >> filesHKDir
            def importFolderStr = nodePamConstantsGetter.getImportFolder()
            File importDir = new File(importFolderStr)
            importDir.mkdir()
            def filepath1 = importFolderStr + "importFile1.csv"
            def filepath2 = importFolderStr + "importFile2.txt"
            File file1 = new File(filepath1)
            File file2 = new File(filepath2)
            file1.createNewFile()
            file2.createNewFile()
        and: "files are older than npamPibParameters.getNpamHouseKeepingDays() days"
            long deletionDateInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(maxHKDate) - 1000
            // update last modified date in files
            file1.setLastModified(deletionDateInMillis)
            file2.setLastModified(deletionDateInMillis)
        and: "a not empty directory exists"
            File dirInImportDir = new File("/tmp/housekeepingTestDir/OtherDir")
            dirInImportDir.mkdir()
            dirInImportDir.setLastModified(deletionDateInMillis)
            File fileInDirInImportDir = new File("/tmp/housekeepingTestDir/OtherDir/aFile")
            fileInDirInImportDir.createNewFile();
            fileInDirInImportDir.setLastModified(deletionDateInMillis)
        when: "housekeeping for files is triggered"
            objectUnderTest.triggerHouseKeepingOfFiles(maxHKDate)
        then: "files have been deleted"
        !file1.exists()
        !file2.exists()
        dirInImportDir.exists()
        endForFileHousekeeping()
    }

}