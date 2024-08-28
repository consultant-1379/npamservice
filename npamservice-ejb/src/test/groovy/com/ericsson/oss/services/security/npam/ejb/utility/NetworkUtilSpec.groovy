package com.ericsson.oss.services.security.npam.ejb.utility

import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_USER_NOT_FOUND

import javax.inject.Inject

import com.ericsson.cds.cdi.support.rule.MockedImplementation
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo
import com.ericsson.oss.services.security.npam.ejb.BaseSetupForTestSpecs
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService
import com.ericsson.oss.services.topologyCollectionsService.dto.DetailedCollectionDTO
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO
import com.ericsson.oss.services.topology_collection.service.api.CollectionsService
import com.ericsson.oss.services.topology_collection.service.api.collections.search.CollectionSubType
import com.ericsson.oss.services.topology_search.service.SearchService
import com.ericsson.oss.services.topology_search.service.api.SearchResponse
import com.ericsson.oss.services.topology_search.service.api.SearchResultItem

class NetworkUtilSpec extends BaseSetupForTestSpecs {

    @Inject
    NetworkUtil objUnderTest

    @Inject
    TopologyCollectionsEjbService topologyCollectionsEjbServiceMock

    @Inject
    CollectionsService collectionsEjbServiceMock

    @Inject
    SearchService searchServiceMock

    @MockedImplementation
    TbacEvaluation tbacEvaluationMock;

    final String userId = 'administrator'

    final static String COLLECTION_NAME = "my-collection"
    final static String SAVED_SEARCH_NAME = "my-saved-search"
    final static String INVALID_NAME = "invalid-name"

    final List<String> emptyList = new ArrayList<>();
    final List<String> invalidList = new ArrayList<>(1)
    final List<String> validNeNamesList = new ArrayList<>(1)
    final List<String> validCollectionList = new ArrayList<>(1)
    final List<String> validSavedSearchList = new ArrayList<>(1)

    def nodeName1 = "node01"
    def nodeName2 = "node02"

    def setup() {
        invalidList.add(INVALID_NAME)

        addNodeOnly(nodeName1) //useful for Ne Names tests
        addNodeOnly(nodeName2) //useful for Ne Names tests

        validNeNamesList.add(nodeName1)
        validNeNamesList.add(nodeName2)

        validCollectionList.add(COLLECTION_NAME)
        validSavedSearchList.add(SAVED_SEARCH_NAME)


        SavedSearchDTO savedSearchDTO = createSavedSearchDto(SAVED_SEARCH_NAME)
        List<SearchResultItem> searchResultItemList = new ArrayList<>()
        SearchResultItem searchResultItem = new SearchResultItem("NetworkElement="+nodeName1)
        searchResultItemList.add(searchResultItem)
        SearchResponse searchResponse = new SearchResponse()
        searchResponse.setResults(searchResultItemList)
    }

    /*
     *
     *       SECTION getAllNetworkElementFromNeInfo
     *
     */
    def 'Call getAllNetworkElementFromNeInfo with bad parameters -> throws exception'() {
        given: 'Fill empty neInfo'
        def neInfo = createNeInfo(emptyList, emptyList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, null, false)

        then: 'an exception about bad parameters'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.INTERNAL_SERVER_ERROR.message)
        ex.getInternalCode().getErrorDetails().equals(INTERNAL_SERVER_ERROR_USER_NOT_FOUND)
    }

    /*
     *   Ne Names section
     * */
    def 'Call getAllNetworkElementFromNeInfo with valid ne names list -> recover NE names'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> true
        and: ''
        def neInfo = createNeInfo(validNeNamesList, emptyList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        def neNameList = objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'all NE names are retrieved'
        neNameList.contains(nodeName1)
        neNameList.contains(nodeName2)
    }

    def 'Call getAllNetworkElementFromNeInfo with invalid ne names list (for TBAC) -> throws exception'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> false
        and:
        def neInfo = createNeInfo(validNeNamesList, emptyList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT.message)
    }

    def 'Call getAllNetworkElementFromNeInfo with invalid ne names list  (for TBAC) but retrieveAllNeNames=true -> recover NE names'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> false
        and: ''
        def neInfo = createNeInfo(validNeNamesList, emptyList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        def neNameList = objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, true)

        then: 'all NE names are retrieved'
        neNameList.contains(nodeName1)
        neNameList.contains(nodeName2)
    }

    def 'Call getAllNetworkElementFromNeInfo with invalid ne names list -> throws exception'() {
        given: 'invalid node'
        final List<String> inValidNeNamesList = new ArrayList<>(1)
        inValidNeNamesList.add("invalid_node")
        def neInfo = createNeInfo(inValidNeNamesList, emptyList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT.message)
    }

    /*
     *   Collection section
     * */
    def 'Call getAllNetworkElementFromNeInfo with valid collection of NetworkElement(s) -> recover NE names'() {
        given: 'Collection is created'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        def neNameList = objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'all NE names are retrieved'
        neNameList.contains(nodeName1)
        neNameList.contains(nodeName2)
    }

    def 'Call getAllNetworkElementFromNeInfo with empty collection -> recover emtpy NE names'() {
        given: 'Collection is created with no elements'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        collectionOfNetworkElementDto.elements = new ArrayList<>() //empty elements
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        def neNameList= objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'no NE names are retrieved'
        neNameList.isEmpty()
    }

    def 'Call getAllNetworkElementFromNeInfo with unknown collection -> throws exception'() {
        given: 'Collection not exist'
        def neInfo = createNeInfo(emptyList, invalidList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_NOT_FOUND.message)
    }

    def 'Call getAllNetworkElementFromNeInfo with hybrid collection of NetworkElement(s) -> throws exception'() {
        given: 'Collection is created with unsupported subType=BRANCH'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        collectionOfNetworkElementDto.subType = CollectionSubType.BRANCH  //hybrid
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception about is hybrid'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_HYBRID.message)
    }

    def 'Call getAllNetworkElementFromNeInfo with collection of ManagedElement(s) -> throws exception'() {
        given: 'Collection is created'
        DetailedCollectionDTO collectionOfManagedElementDto = createCollectionDto(COLLECTION_NAME, "ManagedElement", nodeName1, nodeName2)
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfManagedElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfManagedElementDto.getId(), userId, _) >> collectionOfManagedElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_MIXED_CONTENT.message)
    }


    /*
     *   Saved Search section
     * */
    def 'Call getAllNetworkElementFromNeInfo with valid saved search of NetworkElement(s) -> recover NE names'() {
        given: 'SavedSearch is created'
        SavedSearchDTO savedSearchDTO = createSavedSearchDto(SAVED_SEARCH_NAME)
        topologyCollectionsEjbServiceMock.getSavedSearchesByName(userId, SAVED_SEARCH_NAME)  >> [savedSearchDTO]
        searchServiceMock.search(savedSearchDTO.getQuery(), null) >> createSearchResponse("NetworkElement", nodeName1)
        and:
        def neInfo = createNeInfo(emptyList, emptyList, validSavedSearchList)

        when: 'getAllNetworkElementFromNeInfo'
        def neNameList = objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'all NE names are retrieved'
        neNameList.contains(nodeName1)
    }

    def 'Call getAllNetworkElementFromNeInfo with saved search of ManagedElement(s) -> throws exception'() {
        given: 'SavedSearch is created'
        SavedSearchDTO savedSearchDTO = createSavedSearchDto(SAVED_SEARCH_NAME)
        topologyCollectionsEjbServiceMock.getSavedSearchesByName(userId, SAVED_SEARCH_NAME)  >> [savedSearchDTO]
        searchServiceMock.search(savedSearchDTO.getQuery(), null) >> createSearchResponse("ManagedElement", nodeName1)
        and:
        def neInfo = createNeInfo(emptyList, emptyList, validSavedSearchList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_MIXED_CONTENT.message)
    }

    def 'Call getAllNetworkElementFromNeInfo with unknown saved search -> throws exception'() {
        given: 'SavedSearch not exist'
        def neInfo = createNeInfo(emptyList, emptyList, invalidList)

        when: 'getAllNetworkElementFromNeInfo'
        objUnderTest.getAllNetworkElementFromNeInfo(neInfo, userId, false)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_NOT_FOUND.message)
    }


    /*
     *
     *       SECTION validateNeInfo
     *
     */


    /*
     *   Ne Names section
     * */
    def 'Call validateNeInfo with valid ne names list -> not throws exception'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> true
        and: ''
        def neInfo = createNeInfo(validNeNamesList, emptyList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then:
        notThrown(NPAMRestErrorException)
    }

    def 'Call validateNeInfo with invalid ne names list (for TBAC) -> throws exception'() {
        given: 'valid nodes for TBAC'
        tbacEvaluationMock.getNodePermission(*_) >> false
        and:
        def neInfo = createNeInfo(validNeNamesList, emptyList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT.message)
    }

    def 'Call validateNeInfo with invalid ne names list -> throws exception'() {
        given: 'invalid node'
        final List<String> inValidNeNamesList = new ArrayList<>(1)
        inValidNeNamesList.add("invalid_node")
        def neInfo = createNeInfo(inValidNeNamesList, emptyList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT.message)
    }

    /*
     *   Collection section
     * */
    def 'Call validateNeInfo with valid collection of NetworkElement(s) -> not throws exception'() {
        given: 'Collection is created'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then:
        notThrown(NPAMRestErrorException)
    }

    def 'Call validateNeInfo with empty collection -> not throws exception'() {
        given: 'Collection is created with no elements'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        collectionOfNetworkElementDto.elements = new ArrayList<>() //empty elements
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then:
        notThrown(NPAMRestErrorException)
    }

    def 'Call validateNeInfo with unknown collection -> throws exception'() {
        given: 'Collection not exist'
        def neInfo = createNeInfo(emptyList, invalidList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_NOT_FOUND.message)
    }

    def 'Call validateNeInfo with hybrid collection of NetworkElement(s) -> throws exception'() {
        given: 'Collection is created with unsupported subType=BRANCH'
        DetailedCollectionDTO collectionOfNetworkElementDto = createCollectionDto(COLLECTION_NAME, "NetworkElement", nodeName1, nodeName2)
        collectionOfNetworkElementDto.subType = CollectionSubType.BRANCH  //hybrid
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfNetworkElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfNetworkElementDto.getId(), userId, _) >> collectionOfNetworkElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception about is hybrid'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_HYBRID.message)
    }

    def 'Call validateNeInfo with collection of ManagedElement(s) -> throws exception'() {
        given: 'Collection is created'
        DetailedCollectionDTO collectionOfManagedElementDto = createCollectionDto(COLLECTION_NAME, "ManagedElement", nodeName1, nodeName2)
        topologyCollectionsEjbServiceMock.getCollectionsByName(userId, COLLECTION_NAME) >> [collectionOfManagedElementDto]
        collectionsEjbServiceMock.getCollectionByID(collectionOfManagedElementDto.getId(), userId, _) >> collectionOfManagedElementDto
        and:
        def neInfo = createNeInfo(emptyList, validCollectionList, emptyList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_MIXED_CONTENT.message)
    }

    /*
     *   Saved Search section
     * */
    def 'Call validateNeInfo with valid saved search of NetworkElement(s) -> not throws exception'() {
        given: 'SavedSearch is created'
        SavedSearchDTO savedSearchDTO = createSavedSearchDto(SAVED_SEARCH_NAME)
        topologyCollectionsEjbServiceMock.getSavedSearchesByName(userId, SAVED_SEARCH_NAME)  >> [savedSearchDTO]
        searchServiceMock.search(savedSearchDTO.getQuery(), null) >> createSearchResponse("NetworkElement", nodeName1)
        and:
        def neInfo = createNeInfo(emptyList, emptyList, validSavedSearchList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then:
        notThrown(NPAMRestErrorException)
    }

    def 'Call validateNeInfo with saved search of ManagedElement(s) -> throws exception'() {
        given: 'SavedSearch is created'
        SavedSearchDTO savedSearchDTO = createSavedSearchDto(SAVED_SEARCH_NAME)
        topologyCollectionsEjbServiceMock.getSavedSearchesByName(userId, SAVED_SEARCH_NAME)  >> [savedSearchDTO]
        searchServiceMock.search(savedSearchDTO.getQuery(), null) >> createSearchResponse("ManagedElement", nodeName1)
        and:
        def neInfo = createNeInfo(emptyList, emptyList, validSavedSearchList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_MIXED_CONTENT.message)
    }

    def 'Call validateNeInfo with unknown saved search -> throws exception'() {
        given: 'SavedSearch not exist'
        def neInfo = createNeInfo(emptyList, emptyList, invalidList)

        when: 'validateNeInfo'
        objUnderTest.validateNeInfo(neInfo, userId)

        then: 'an exception about not exist'
        NPAMRestErrorException ex = thrown()
        ex.getInternalCode().getMessage().equals(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_NOT_FOUND.message)
    }


    /*
     *   private methods
     * */

    private NEInfo createNeInfo(List<String> names, List<String> collections, List<String> savedSearches) {
        NEInfo neInfo = new NEInfo();
        neInfo.setNeNames(names)
        neInfo.setSavedSearchIds(savedSearches)
        neInfo.setCollectionNames(collections)

        return neInfo;
    }
}
