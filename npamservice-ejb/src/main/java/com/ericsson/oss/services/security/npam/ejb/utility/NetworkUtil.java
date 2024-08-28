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
package com.ericsson.oss.services.security.npam.ejb.utility;

import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.CONFIGURATION_LIVE;
import static com.ericsson.oss.services.security.npam.api.constants.NodePamConstants.NETWORK_ELEMENT_MO;
import static com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorsMessageDetails.INTERNAL_SERVER_ERROR_USER_NOT_FOUND;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ericsson.oss.itpf.datalayer.dps.persistence.ManagedObject;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;
import com.ericsson.oss.itpf.sdk.security.accesscontrol.EAccessControl;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException;
import com.ericsson.oss.services.security.npam.api.exceptions.NPAMRestErrorException.NPamRestErrorMessage;
import com.ericsson.oss.services.security.npam.api.job.modelentities.NEInfo;
import com.ericsson.oss.services.security.npam.ejb.dao.DpsReadOperations;
import com.ericsson.oss.services.topologyCollectionsService.api.ReadCollectionOptions;
import com.ericsson.oss.services.topologyCollectionsService.api.TopologyCollectionsEjbService;
import com.ericsson.oss.services.topologyCollectionsService.dto.CollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.DetailedCollectionDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.ManagedObjectDTO;
import com.ericsson.oss.services.topologyCollectionsService.dto.SavedSearchDTO;
import com.ericsson.oss.services.topologyCollectionsService.service.sort.OrderDirection;
import com.ericsson.oss.services.topologyCollectionsService.service.sort.Sort;
import com.ericsson.oss.services.topology_collection.service.api.CollectionsService;
import com.ericsson.oss.services.topology_collection.service.api.collections.search.CollectionSubType;
import com.ericsson.oss.services.topology_search.service.SearchService;
import com.ericsson.oss.services.topology_search.service.api.SearchResponse;
import com.ericsson.oss.services.topology_search.service.api.SearchResultItem;


public class NetworkUtil {
    /**
     *
     */
    private static final String COLLECTION_WITH_NAME_MESSAGE = "Collection with name: ";
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);
    private static final String NETWORK_ELEMENT = "NetworkElement";

    private static final String EQUAL_SEPARATOR = "=";
    private static final String COMMA_SEPARATOR = ",";

    @EServiceRef
    private TopologyCollectionsEjbService topologyCollectionsEjbService;

    @EServiceRef
    private CollectionsService collectionsEjbService;

    @EServiceRef
    private SearchService searchExecutor;

    @Inject
    TbacEvaluation tbacEvaluation;

    @Inject
    EAccessControl eAccessControl;

    @Inject
    DpsReadOperations dpsReadOperations;


    public Set<String> getAllNetworkElementFromNeInfo(final NEInfo selectedNEInfo, final String ownerName, final boolean retrieveAllNeNames) {
        final Set<String> neNames = new HashSet<>();
        if (selectedNEInfo == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_INVALID_SELECTED_NES);
        }
        if (ownerName == null) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_USER_NOT_FOUND);
        }
        if (retrieveAllNeNames) {
            if (selectedNEInfo.getNeNames() != null) {
                neNames.addAll(selectedNEInfo.getNeNames());
            }
        }
        else {
            if (selectedNEInfo.getNeNames() != null) {
                neNames.addAll(validateNeNames(selectedNEInfo.getNeNames(), ownerName));
            }
        }
        final List<String> collectionIds = selectedNEInfo.getCollectionNames();
        final List<String> savedSearchIds = selectedNEInfo.getSavedSearchIds();
        if (collectionIds != null && !collectionIds.isEmpty()) {
            // Retrieve networkelement from Collections
            final List<String> neNamesFromCollections = populateNeNamesFromCollections(collectionIds, ownerName);
            neNames.addAll(neNamesFromCollections);
        }
        if (savedSearchIds != null && !savedSearchIds.isEmpty()) {
            // Retrieve networkelement from SavedSearch
            final List<String> neNamesFromSavedSearches = populateNeNamesFromSavedSearches(savedSearchIds, ownerName);
            neNames.addAll(neNamesFromSavedSearches);
        }
        return neNames;
    }

    public void validateNeInfo(final NEInfo selectedNEInfo, final String ownerName) {
        if (selectedNEInfo.getNeNames() != null) {
            validateNeNames(selectedNEInfo.getNeNames(), ownerName);
        }

        final List<String> collectionIds = selectedNEInfo.getCollectionNames();
        final List<String> savedSearchIds = selectedNEInfo.getSavedSearchIds();
        if (collectionIds != null && !collectionIds.isEmpty()) {
            // Evaluate collections if exist and check all objects contained are NetworkElement
            populateNeNamesFromCollections(collectionIds, ownerName);
        }
        if (savedSearchIds != null && !savedSearchIds.isEmpty()) {
            // Evaluate savedSearches if exist and check all objects contained are NetworkElement
            populateNeNamesFromSavedSearches(savedSearchIds, ownerName);
        }
    }

    public List<String> validateNeNames(final List<String> neNames, final String ownerName) {
        final List<String> invalidNames = new ArrayList<>();
        final List<String> validNames = new ArrayList<>();
        final Set<String> existingNeNames = dpsReadOperations.getAllNodeNames();
        for (final String neName : neNames) {
            if (!existingNeNames.contains(neName) || !tbacEvaluation.getNodePermission(ownerName, neName)) {
                LOGGER.error("Target {} doesn't exist", neName);
                invalidNames.add(neName);
                break;
            } else {
                validNames.add(neName);
            }
        }
        if (!invalidNames.isEmpty()) {
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_NOT_EXISTING_NETWORK_ELEMENT,
                    invalidNames.get(0));
        }
        return validNames;
    }

    public List<String> populateNeNamesFromCollections(final List<String> collectionNamesList, final String ownerName) {
        final Set<String> collectionNames = new LinkedHashSet<>(collectionNamesList);
        return getNodesReferenceListFromCollection(collectionNames, ownerName);
    }

    public List<String> populateNeNamesFromSavedSearches(final List<String> savedSearchNamesList, final String ownerName) {
        final Set<String> savedSearchNames = new LinkedHashSet<>(savedSearchNamesList);
        return getNodeReferenceListFromSavedSearches(savedSearchNames, ownerName);

    }

    private List<String> getNodesReferenceListFromCollection(final Set<String> collectionNames, final String ownerName) {
        LOGGER.info("Retrieving node names from the collections [{}]", collectionNames);
        final List<String> nodes = new ArrayList<>();
        eAccessControl.setAuthUserSubject(ownerName);
        final Collection<CollectionDTO> collectionDTOs = getCollectionDtos(ownerName, collectionNames);
        for (final CollectionDTO collectionDTO : collectionDTOs) {
            LOGGER.debug("Retrieved collection  name : {} category: {}", collectionDTO.getName(), collectionDTO.getCategory());
            prepareFdnsListAndInvalidCollectionNamesList(collectionDTO, ownerName, nodes);
        }

        return nodes;
    }

    private Collection<CollectionDTO> getCollectionDtos(final String userId, final Set<String> collectionNames)
    {
        LOGGER.info("Retrieving collection dtos for the collections {}", collectionNames);
        final Collection<CollectionDTO> collectionDtos = new ArrayList<>();
        final List<String> invalidNames = new ArrayList<>();
        for (final String collectionName : collectionNames) {
            final Collection<CollectionDTO> collectionDto = topologyCollectionsEjbService.getCollectionsByName(userId, collectionName);

            if (collectionDto == null || collectionDto.isEmpty()) {
                invalidNames.add(collectionName);
                break;
            }
            collectionDtos.addAll(collectionDto);
        }
        if (!invalidNames.isEmpty()) {
            LOGGER.error("Collection with names: {} don't exist or wrong permission capabilities.", invalidNames);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_NOT_FOUND, invalidNames.get(0));
        }

        return collectionDtos;
    }

    private void prepareFdnsListAndInvalidCollectionNamesList(final CollectionDTO collectionDTO, final String userId,
                                                              final List<String> neNames) {
        final ReadCollectionOptions readCollectionOptions = new ReadCollectionOptions();
        final Sort sort = new Sort("MoName", OrderDirection.ASC);
        readCollectionOptions.setSort(sort);
        readCollectionOptions.setUpdateDeleteObjects(false);
        readCollectionOptions.setAttributeMappings(null);

        final DetailedCollectionDTO collectionDTOByID = collectionsEjbService.getCollectionByID(collectionDTO.getId(), userId, readCollectionOptions);
        if ((collectionDTOByID == null) || collectionDTOByID.getSubType() == CollectionSubType.BRANCH) {
            LOGGER.error("Collection with name: {} is hybrid.", collectionDTO.getName());
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_HYBRID, collectionDTO.getName());
        }
        if (collectionDTOByID.getElements() == null || collectionDTOByID.getElements().isEmpty()) {
            LOGGER.error("{} {} is empty", COLLECTION_WITH_NAME_MESSAGE, collectionDTOByID.getName());
            return;
        }

        for (final ManagedObjectDTO managedObjectDTO : collectionDTOByID.getElements()) {

            final String fdn = managedObjectDTO.getFdn();
            if (fdn != null && !fdn.isEmpty()) {
                managedObjectDTO.getAttributes().forEach((k, v) -> LOGGER.debug("MO retrieved from getCollectionByID Key : {} Value: {}", k, v));
                final String name = getNodeName(fdn);
                if (name != null) {
                    neNames.add(name);
                } else {
                    LOGGER.error("{} {} doesn't contain only NetworkElement.", COLLECTION_WITH_NAME_MESSAGE, collectionDTO.getName());
                    throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_COLLECTION_MIXED_CONTENT, collectionDTO.getName());
                }
            }
        }
    }

    private List<String> getNodeReferenceListFromSavedSearches(final Set<String> savedSearchNames, final String ownerName) {
        LOGGER.info("Retrieving node names from the saved searches {}", savedSearchNames);
        final List<String> neNames = new ArrayList<>();
        eAccessControl.setAuthUserSubject(ownerName);
        final Collection<SavedSearchDTO> ssDtos = getSavedSearchDTOs(ownerName, savedSearchNames);
        for (final SavedSearchDTO ssDto : ssDtos) {

            final String searchQuery = ssDto.getQuery();
            final SearchResponse networkExplorerResponse = searchExecutor.search(searchQuery, null);
            if (networkExplorerResponse == null) {
                LOGGER.error("Saved search with name: {} is empty.", ssDto.getName());
            } else {
                final Set<String> fdnSet = getNodeNamesFromSearchResponse(networkExplorerResponse);
                if (fdnSet.isEmpty()) {
                    LOGGER.info("Saved search with name: {} is empty.", ssDto.getName());
                }
                for (final String fdnForSavedSearch : fdnSet) {
                    final String neNameForSavedSearch = getNodeName(fdnForSavedSearch);
                    if (neNameForSavedSearch != null) {
                        neNames.add(neNameForSavedSearch);
                    } else {
                        LOGGER.error("Saved search with name: {} doesn't contain only NetworkElement.", ssDto.getName());
                        throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_MIXED_CONTENT, ssDto.getName());
                    }
                }
            }
        }
        return neNames;
    }

    private Collection<SavedSearchDTO> getSavedSearchDTOs(final String userId, final Set<String> savedSearchNames) {
        LOGGER.info("Retrieving saved search dtos for the names {}", savedSearchNames);

        final List<String> invalidNames = new ArrayList<>();
        final Collection<SavedSearchDTO> ssDtoCollection = new ArrayList<>();

        for (final String savedSearchName : savedSearchNames) {
            final Collection<SavedSearchDTO> savedSearchDtos = topologyCollectionsEjbService.getSavedSearchesByName(userId, savedSearchName);
            if (savedSearchDtos == null || savedSearchDtos.isEmpty()) {
                invalidNames.add(savedSearchName);
            } else {
                ssDtoCollection.addAll(savedSearchDtos);
            }
        }
        if (!invalidNames.isEmpty()) {
            LOGGER.error("SavedSearch with names: {} don't exist or wrong permission capabilities", invalidNames);
            throw new NPAMRestErrorException(NPamRestErrorMessage.UNPROCESSABLE_ENTITY_SAVED_SEARCH_NOT_FOUND,
                    invalidNames.get(0));
        }
        return ssDtoCollection;
    }

    private Set<String> getNodeNamesFromSearchResponse(final SearchResponse networkExplorerResponse) {
        final List<SearchResultItem> cmObjects = new LinkedList<>();
        cmObjects.addAll(networkExplorerResponse.getResults());

        final Set<String> fdnsSet = new HashSet<>(cmObjects.size());
        for (final SearchResultItem cmObject : cmObjects) {
            final String fdn = cmObject.getFdn();
            fdnsSet.add(fdn);
        }
        return fdnsSet;
    }

    private static String getNodeName(final String fdn) {
        LOGGER.info("getNodeNameFrom FDn :: fdn={}", fdn);

        final String[] rdnsArray = fdn.split(COMMA_SEPARATOR);
        for (final String rdn : rdnsArray) {
            if (rdn.startsWith(NETWORK_ELEMENT)) {

                return rdn.split(EQUAL_SEPARATOR)[1].trim();
            }
        }
        return null;
    }

    public ManagedObject checkNodeDps(final String nodeName) {
        final String networkElementFdn = NETWORK_ELEMENT_MO + "=" + nodeName;
        final ManagedObject managedObject = dpsReadOperations.findManagedObject(networkElementFdn, dpsReadOperations.getDataBucket(CONFIGURATION_LIVE));
        if (managedObject == null) {
            LOGGER.error("validateNodeRootFdn:: not found networkElementFdn={}", networkElementFdn);
            return null;
        }
        return managedObject;
    }

}
