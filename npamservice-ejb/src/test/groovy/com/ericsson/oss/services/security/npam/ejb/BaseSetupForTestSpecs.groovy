package com.ericsson.oss.services.security.npam.ejb

import com.ericsson.oss.services.security.npam.api.job.modelentities.Step
import com.ericsson.oss.services.topologyCollectionsService.dto.DetailedCollectionDTO
import com.ericsson.oss.services.topologyCollectionsService.dto.ManagedObjectDTO
import com.ericsson.oss.services.topology_collection.service.api.collections.search.CollectionSubType
import com.ericsson.oss.services.topology_search.service.api.SearchResponse
import com.ericsson.oss.services.topology_search.service.api.SearchResultItem

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.custom.node.ManagedObjectData
import com.ericsson.cds.cdi.support.rule.custom.node.NodeDataProvider
import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject
import com.ericsson.oss.services.security.npam.api.constants.ModelsConstants
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobProperty
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobResult
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobState
import com.ericsson.oss.services.security.npam.api.job.modelentities.JobType
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService
import com.ericsson.oss.services.topologyCollectionsService.dto.Category
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO
class BaseSetupForTestSpecs extends BaseSpecWithModels implements NodeDataProvider {
    @Inject
    TopologyCollectionsEjbService collectionService

    @Inject
    TopologyCollectionsEjbService savedSearchService

    final static String SUBNETWORK_NAME = "Sample"
    final static boolean REMOTE_MANAGEMENT_TRUE = true
    final static boolean REMOTE_MANAGEMENT_FALSE = false

    def setup() {
        runtimeDps.withTransactionBoundaries()
    }

    def addSubnetWork(final String subnetworkName) {
        runtimeDps.addManagedObject().withFdn("SubNetwork="+subnetworkName)
                .addAttribute("SubNetworkId", subnetworkName)
                .namespace("OSS_TOP")
                .version("3.0.0")
                .build()
    }

    def addNodeTree(final String subnetworkName, final String nodeName, final String syncStatus, final Boolean remoteManagementValue) {
        String managedElementFdn;

        if (subnetworkName != null) {
            managedElementFdn = "SubNetwork="+subnetworkName + ",ManagedElement=" + nodeName;
        } else {
            managedElementFdn = "ManagedElement=" + nodeName;
        }

        // create NetworkElement
        ManagedObject nodeMo = runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute('networkElementId', nodeName)
                .addAttribute('neType','RadioNode')
                .addAttribute('ossPrefix', managedElementFdn)
                .addAttribute('ossModelIdentity', '22.Q4-R60A24')
                .namespace("OSS_NE_DEF")
                .version("2.0.0")
                .type("NetworkElement")
                .build()

        // create CmFunction
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+',CmFunction=1')
                .addAttribute('CmFunctionId', "1")
                .addAttribute('syncStatus',syncStatus)
                .namespace("OSS_NE_CM_DEF")
                .version("1.0.1")
                .type("CmFunction")
                .build()

        // create CmFunction
        runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+',SecurityFunction=1')
                .addAttribute('securityFunctionId', "1")
                .namespace("OSS_NE_SEC_DEF")
                .version("1.0.0")
                .type("SecurityFunction")
                .build()

        // create ManagedElement
        ManagedObject managedElementMo = runtimeDps.addManagedObject().withFdn(managedElementFdn)
                .addAttribute('managedElementId', "nodeName")
                .namespace("ComTop")
                .version("10.23.0")
                .type("ManagedElement")
                .build()

        nodeMo.addAssociation("nodeRootRef",managedElementMo)

        // create SystemFunction
        runtimeDps.addManagedObject().withFdn(managedElementFdn + ",SystemFunctions=1")
                .addAttribute('systemFunctionsId', "1")
                .namespace("ComTop")
                .version("10.23.0")
                .type("SystemFunctions")
                .build()

        // create SecM
        runtimeDps.addManagedObject().withFdn(managedElementFdn + ",SystemFunctions=1,SecM=1")
                .addAttribute('secMId', "1")
                .namespace("RcsSecM")
                .version("12.3.2")
                .type("SecM")
                .build()

        // create UserManagement
        runtimeDps.addManagedObject().withFdn(managedElementFdn + ",SystemFunctions=1,SecM=1,UserManagement=1")
                .addAttribute('userManagementId', "1")
                .namespace("RcsSecM")
                .version("12.3.2")
                .type("UserManagement")
                .build()

        // create UserIdentity
        runtimeDps.addManagedObject().withFdn(managedElementFdn + ",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1")
                .addAttribute('userIdentityId', "1")
                .namespace("RcsUser")
                .version("6.2.2")
                .type("UserIdentity")
                .build()

        // create UserIdentity
        runtimeDps.addManagedObject().withFdn(managedElementFdn + ",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1,MaintenanceUserSecurity=1")
                .addAttribute('maintenanceUserSecurityId', "1")
                .addAttribute('remoteManagement', remoteManagementValue)
                .addAttribute('loginDelay', '5')
                .addAttribute('loginDelayPolicy', 'FIXED')
                .addAttribute('noOfFailedLoginAttempts', '2')
                .addAttribute('userLockoutPeriod', '3')
                .namespace("RcsUser")
                .version("6.2.2")
                .type("MaintenanceUserSecurity")
                .build()
    }

    def addMaintenanceUser(final String subnetworkName, final String nodeName, final int muid) {
        String parentFdn
        if (subnetworkName != null) {
            parentFdn = "SubNetwork="+subnetworkName + ",ManagedElement=" + nodeName+",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1";
        } else {
            parentFdn = "ManagedElement=" + nodeName+",SystemFunctions=1,SecM=1,UserManagement=1,UserIdentity=1";
        }
        //create MaintenanceUser
        runtimeDps.addManagedObject().withFdn(parentFdn + ",MaintenanceUser="+muid)
                .addAttribute('maintenanceUserId', muid)
                .addAttribute('subjectName', 'testSubjectName')
                .addAttribute('userName', 'testUserName')
                .addAttribute('password', ['cleartext':false, 'password':'testPassword'])
                .namespace("RcsUser")
                .version("6.2.2")
                .type("MaintenanceUser")
                .build()
    }

    def addNeAccount(final String nodeName, final int neAccountId, final String neAccountStatus) {
        def poObj = runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName+',SecurityFunction=1,NetworkElementAccount='+neAccountId)
                .addAttribute('networkElementAccountId', neAccountId+"")
                .addAttribute("updateStatus",neAccountStatus)
                .addAttribute("currentUsername","userNameEncrypted")
                .addAttribute("currentPassword","passwordEncrypted")
                .namespace("OSS_NE_SEC_DEF")
                .version("1.0.0")
                .type("NetworkElementAccount")
                .build()
            if (neAccountStatus.equals("FAILED")) {
                poObj.setAttribute("lastFailed", new Date())
                poObj.setAttribute("nextUsername","nextUserNameEncryptedFailed")
                poObj.setAttribute("nextPassword","nextPasswordEncryptedFailed")
            }
            if (neAccountStatus.equals("CONFIGURED")) {
                poObj.setAttribute("lastPasswordChange", new Date())
            }
        return poObj
    }

    def addNpamJobTemplate(final String name,
                           final String owner,
                           final JobType jobType,
                           final List<JobProperty> jobProperties,
                           final HashMap<String, Object> mainSchedule,
                           final List<String> nodes,
                           final List<String> collectionNames,
                           final List<String> savedSearchIds
    ) {
        Map<String,List<String>> neInfo = new HashMap<>(1)
        neInfo.put("neNames", nodes)
        neInfo.put("collectionNames", collectionNames)
        neInfo.put("savedSearchIds", savedSearchIds)
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_JOBTEMPLATE).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .addAttribute("name", name)
                .addAttribute("owner", owner)
                .addAttribute("creationTime", new Date())
                .addAttribute("description","some description")
                .addAttribute("jobType",jobType.name())
                .addAttribute("jobProperties",prepareJobProperties(jobProperties))
                .addAttribute("selectedNEs",neInfo)
                .addAttribute("mainSchedule", mainSchedule)
                .build()
    }

    def addNpamJobTemplateForHousekeeping(final String name,
                                          final String owner,
                                          final JobType jobType,
                                          final List<JobProperty> jobProperties,
                                          final HashMap<String, Object> mainSchedule,
                                          final List<String> nodes,
                                          final List<String> collectionNames,
                                          final List<String> savedSearchIds,
                                          final Date creationTime
    ) {
        Map<String,List<String>> neInfo = new HashMap<>(1)
        neInfo.put("neNames", nodes)
        neInfo.put("collectionNames", collectionNames)
        neInfo.put("savedSearchIds", savedSearchIds)
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_JOBTEMPLATE).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .addAttribute("name", name)
                .addAttribute("owner", owner)
                .addAttribute("creationTime", creationTime)
                .addAttribute("description","some description")
                .addAttribute("jobType", jobType.name())
                .addAttribute("jobProperties", prepareJobProperties(jobProperties))
                .addAttribute("selectedNEs", neInfo)
                .addAttribute("mainSchedule", mainSchedule)
                .build()
    }


    def addNpamJobWithNodes(final long templateJobId,
            final String owner,
            final JobType jobType,
            final JobState jobState,
            final List<JobProperty> jobProperties,
            final List<String> nodes,
            final List<String> collectionNames,
            final List<String> savedSearchIds
    ) {

        Map<String,List<String>> neInfo = new HashMap<>(1)
        neInfo.put("neNames",nodes)
        neInfo.put("collectionNames", collectionNames)
        neInfo.put("savedSearchIds", savedSearchIds)
        def scheduledTime = new Date()
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_JOB).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .addAttribute("state",jobState.name())
                .addAttribute("progressPercentage",0.0d)
                .addAttribute("result",null)
                .addAttribute("startTime",scheduledTime)
                .addAttribute("endTime",null)
                .addAttribute("creationTime",new Date())
                .addAttribute("jobType",jobType.name())
                .addAttribute("executionIndex",0)
                .addAttribute("selectedNEs",neInfo)
                .addAttribute("templateJobId",templateJobId)
                .addAttribute("numberOfNetworkElements",nodes.size())
                .addAttribute("scheduledTime", scheduledTime)
                .addAttribute("jobProperties", prepareJobProperties(jobProperties))
                .addAttribute("name", "jobName")
		        .addAttribute("owner", owner)
                .build()
    }

    def addNpamJobWithNodesForHousekeeping(final long templateJobId,
                                           final String owner,
                                           final JobType jobType,
                                           final JobState jobState,
                                           final List<JobProperty> jobProperties,
                                           final List<String> nodes,
                                           final List<String> collectionNames,
                                           final List<String> savedSearchIds,
                                           Date scheduledTime,
                                           final Date endTime,
                                           final String jobName
    ) {
        Map<String,List<String>> neInfo = new HashMap<>(1)
        neInfo.put("neNames",nodes)
        neInfo.put("collectionNames", collectionNames)
        neInfo.put("savedSearchIds", savedSearchIds)
        if (scheduledTime == null) {
            scheduledTime = new Date()
        }
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_JOB).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .addAttribute("state",jobState.name())
                .addAttribute("progressPercentage",0.0d)
                .addAttribute("result",null)
                .addAttribute("startTime",scheduledTime)
                .addAttribute("endTime",endTime)
                .addAttribute("creationTime", new Date())
                .addAttribute("jobType",jobType.name())
                .addAttribute("executionIndex",0)
                .addAttribute("selectedNEs",neInfo)
                .addAttribute("templateJobId",templateJobId)
                .addAttribute("numberOfNetworkElements",nodes.size())
                .addAttribute("scheduledTime", scheduledTime)
                .addAttribute("jobProperties", prepareJobProperties(jobProperties))
                .addAttribute("name", jobName)
                .addAttribute("owner", owner)
                .build()
    }

    def addNpamJobWithoutAttributes() {
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_JOB).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .build()
    }

    def addNpamNeJob(final long mainJobId, final JobState jobState, final JobResult jobResult, final String neName) {
        String jobResultString = null
        if (jobResult != null) {
            jobResultString = jobResult.name()
        }
        return runtimeDps.addPersistenceObject().type(ModelsConstants.NPAM_NEJOB).namespace(ModelsConstants.NAMESPACE)
                .version(ModelsConstants.VERSION)
                .addAttribute("state",jobState.name())
                .addAttribute("mainJobId",mainJobId)
                .addAttribute("neName",neName)
                .addAttribute("result", jobResultString)
                .addAttribute("startTime", new Date())
                .addAttribute("step", Step.NONE.name())
                .build()
    }

    def addNodeOnly(final String nodeName) {
        def managedElementFdn = "ManagedElement=" + nodeName;
        return runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute('networkElementId', nodeName)
                .addAttribute('neType','RadioNode')
                .addAttribute('ossPrefix', managedElementFdn)
                .addAttribute('ossModelIdentity', '22.Q4-R60A24')
                .namespace("OSS_NE_DEF")
                .version("2.0.0")
                .type("NetworkElement")
                .build()

    }

    def addNodeNoRadioNode(final String nodeName) {
        def managedElementFdn = "ManagedElement=" + nodeName;
        return runtimeDps.addManagedObject().withFdn("NetworkElement="+nodeName)
                .addAttribute('networkElementId', nodeName)
                .addAttribute('neType','NORadioNode')
                .addAttribute('ossPrefix', managedElementFdn)
                .addAttribute('ossModelIdentity', '22.Q4-R60A24')
                .namespace("OSS_NE_DEF")
                .version("2.0.0")
                .type("NetworkElement")
                .build()
    }
    
    private List<Map<String, String>> prepareJobProperties(List<JobProperty> jobPropertiesList) {
        final List<Map<String, String>> jobProperties = new ArrayList<>();
        for (final JobProperty jobProperty : jobPropertiesList) {
            jobProperties.add(prepareJobProperty(jobProperty.getKey(), jobProperty.getValue()));
        }
        return jobProperties;
    }

    private Map<String, String> prepareJobProperty(final String propertyName, final String propertyValue) {
        final Map<String, String> property = new HashMap<>();
        property.put(JobProperty.JOB_PROPERTY_KEY, propertyName);
        property.put(JobProperty.JOB_PROPERTY_VALUE, propertyValue);
        return property;
    }

    def createCollectionDto(final String collectionName, final String moType, final String nodeName1, final String nodeName2) {
        def collection = new DetailedCollectionDTO(name: collectionName,
        managedObjectsIDs: [nodeName1, nodeName2], category: "PUBLIC", owner: "administrator")
        collection.setSortable(false)
        collection.setSubType(CollectionSubType.LEAF)

        ManagedObjectDTO managedObjectDTONode1 = new ManagedObjectDTO()
        managedObjectDTONode1.setFdn(moType + "="+nodeName1)

        ManagedObjectDTO managedObjectDTONode2 = new ManagedObjectDTO()
        managedObjectDTONode2.setFdn(moType + "="+nodeName2)
        List<ManagedObjectDTO> elements = new ArrayList<>()
        elements.add(managedObjectDTONode1)
        elements.add(managedObjectDTONode2)
        collection.setElements(elements)
        collection.setId("1")

        return collection
    }

    def createSavedSearchDto (final String savedSearchName) {
        def savedSearch = new SavedSearchDTO(name: savedSearchName, owner: "administrator", category: Category.PUBLIC, query: "NetworkElement")
        return savedSearch
    }

    def createSearchResponse(final String moType, final String nodeName) {
        List<SearchResultItem> searchResultItemList = new ArrayList<>()
        SearchResultItem searchResultItem = new SearchResultItem(moType + "="+nodeName)
        searchResultItemList.add(searchResultItem)
        SearchResponse searchResponse = new SearchResponse()
        searchResponse.setResults(searchResultItemList)
        return searchResponse
    }

    @Override
    Map<String, Object> getAttributesForMo(final String moFdn) {
        def map = null
        map == null ? [:] : map
    }

    @Override
    List<ManagedObjectData> getAdditionalNodeManagedObjects() {
        []  //we use setupDbNodes to create new objects
    }
}
