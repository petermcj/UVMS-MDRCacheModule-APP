/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.
This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.mdr.service.bean;

import eu.europa.ec.fisheries.mdr.entities.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.entities.constants.AcronymListState;
import eu.europa.ec.fisheries.mdr.exception.MdrCacheInitException;
import eu.europa.ec.fisheries.mdr.exception.MdrMappingException;
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
import eu.europa.ec.fisheries.mdr.mapper.MdrRequestMapper;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.repository.MdrStatusRepository;
import eu.europa.ec.fisheries.mdr.service.MdrSynchronizationService;
import eu.europa.ec.fisheries.mdr.util.GenericOperationOutcome;
import eu.europa.ec.fisheries.mdr.util.OperationOutcome;
import eu.europa.ec.fisheries.uvms.commons.date.DateUtils;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.IMdrMessageProducer;
import eu.europa.ec.fisheries.uvms.commons.message.api.MessageException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * @author kovian
 *         <p>
 *         EJB that provides the MDR Synchronization Functionality.
 *         1. Methods for synchronizing the MDR lists
 *         2. Method for getting the actual state of the MDR codeLists
 */
@Slf4j
@Stateless
@Transactional
public class MdrSynchronizationServiceBean implements MdrSynchronizationService {

    @EJB
    private MdrRepository mdrRepository;

    @EJB
    private MdrStatusRepository statusRepository;

    @EJB
    private IMdrMessageProducer producer;

    private static final String OBJ_DATA_ALL = "OBJ_DATA_ALL";
    private static final String OBJ_DESC     = "OBJ_DESC";
    private static final String INDEX        = "INDEX";
    private static final String ERROR_WHILE_TRYING_TO_MAP_MDRQUERY_TYPE_FOR_ACRONYM = "Error while trying to map MDRQueryType for acronym {}";
    private static final String MDR_EXCLUSION_LIST = "mdr.exclusion.list";

    private List<String> exclusionList;


    @PostConstruct
    public void loadExclusionList() throws IOException {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("exclusionList.properties");
        Properties props = new Properties();
        props.load(resourceAsStream);
        List<String> propertyStr = Arrays.asList((props.getProperty(MDR_EXCLUSION_LIST)));
        exclusionList = CollectionUtils.isNotEmpty(propertyStr) ? Arrays.asList(propertyStr.get(0).split(",")) : new ArrayList<String>();
    }

    /**
     * Manually startable Job for the MDR Entities synchronising.
     * It will check if the acronym is schedulable before sending a request.
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public GenericOperationOutcome manualStartMdrSynchronization() {
        return extractAcronymsAndUpdateMdr();
    }

    /**
     * Extracts all the available acronyms and for each of those that are updatable
     * sends an update request message to the next module (that will propagate it to - other modules which will propagate it until the - flux node).
     *
     * @return errorContainer
     */
    @Override
    public GenericOperationOutcome extractAcronymsAndUpdateMdr() {
        log.info("\n\t\t[START] Starting sending code-lists synchronization requests.\n");
        List<String> updatableAcronyms = extractUpdatableAcronyms(getAvailableMdrAcronyms());
        GenericOperationOutcome errorContainer = updateMdrEntities(updatableAcronyms);
        log.info("\n\n\t\t[END] Sending of synchronization requests finished! (Sent : [ "+updatableAcronyms.size()+" ] code-lists synch requests in total!) \n\n");
        return errorContainer;
    }


    /**
     * Extract a sublist containing all the acronyms that are updatable by the scheduler (schedulable);
     *
     * @param availableAcronyms
     * @return matchList
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private List<String> extractUpdatableAcronyms(List<String> availableAcronyms) {
        List<String> statusListFromDb = extractAcronymsListFromAcronymStatusList(statusRepository.getAllUpdatableAcronymsStatuses());
        List<String> matchList = new ArrayList<>();
        for (String actualCacheAcronym : availableAcronyms) {
            if (statusListFromDb.contains(actualCacheAcronym)) {
                matchList.add(actualCacheAcronym);
            }
        }
        return matchList;
    }


    /**
     * Extracts the objectAcronyms from the list of MdrCodeListStatus objects;
     *
     * @param allUpdatableAcronymsStatuses
     * @return acronymsStrList
     */
    private List<String> extractAcronymsListFromAcronymStatusList(List<MdrCodeListStatus> allUpdatableAcronymsStatuses) {
        List<String> acronymsStrList = new ArrayList<>();
        for (MdrCodeListStatus actStatus : allUpdatableAcronymsStatuses) {
            acronymsStrList.add(actStatus.getObjectAcronym());
        }
        return acronymsStrList;
    }

    /**
     * Updates the given list of mdr entities.
     * The list given as input contains the acronyms.
     * For each acronym a request to flux will be sent and the status (Status Table)
     * will be set to RUNNING or FAILED depending on the outcome of the operation.
     *
     * @param acronymsList
     * @return errorContainer
     */
    @Override
    public GenericOperationOutcome updateMdrEntities(List<String> acronymsList) {
        // For each Acronym send a request object towards Exchange module.
        GenericOperationOutcome errorContainer = new GenericOperationOutcome();
        List<String> existingAcronymsList;
        try {
            existingAcronymsList = MasterDataRegistryEntityCacheFactory.getAcronymsList();
        } catch (MdrCacheInitException e) {
            log.error("Error while trying to get acronymsList from cache", e);
            return new GenericOperationOutcome(OperationOutcome.NOK, "Error while trying to get acronymsList from cache");
        }
        // 1. Send update request (data)
        for (String actualAcronym : acronymsList) {
            log.info("Preparing Request Object for " + actualAcronym + " and sending message to Rules queue.");

            // Create request object and send message to exchange module
            if (existingAcronymsList.contains(actualAcronym) && !acronymIsInExclusionList(actualAcronym)) {// Acronym exists
                String strReqObj;
                String uuid = java.util.UUID.randomUUID().toString();
                try {
                    strReqObj   = MdrRequestMapper.mapMdrQueryTypeToString(actualAcronym, OBJ_DATA_ALL, uuid);
                    producer.sendRulesModuleMessage(strReqObj);
                    statusRepository.updateStatusAttemptForAcronym(actualAcronym, AcronymListState.RUNNING, DateUtils.nowUTC().toDate(), uuid);
                    log.info("Synchronization Request Sent for Entity : " + actualAcronym);
                } catch (MdrMappingException e) {
                    log.error(ERROR_WHILE_TRYING_TO_MAP_MDRQUERY_TYPE_FOR_ACRONYM, actualAcronym, e);
                    errorContainer.addMessage("Error while trying to map MDRQueryType for acronym {} " + actualAcronym);
                    statusRepository.updateStatusAttemptForAcronym(actualAcronym, AcronymListState.FAILED, DateUtils.nowUTC().toDate(), uuid);
                } catch (MessageException e) {
                    log.error("Error while trying to send message from MDR module to Rules module.", e);
                    errorContainer.addMessage("Error while trying to send message from MDR module to Rules module for acronym {} " + actualAcronym);
                    statusRepository.updateStatusAttemptForAcronym(actualAcronym, AcronymListState.FAILED, DateUtils.nowUTC().toDate(), uuid);
                }
                errorContainer.setIncludedObject(statusRepository.getAllAcronymsStatuses());
            } else {// Acronym does not exist
                log.debug("Couldn't find the acronym \" " + actualAcronym + " \" (or the acronym is in Exclusion List) in the cachedAcronymsList! Request for said acronym won't be sent to flux!");
                errorContainer.addMessage("The following acronym doesn't exist (or is excluded) in the cacheFactory : " + actualAcronym);
            }
        }

        return errorContainer;
    }

    private boolean acronymIsInExclusionList(String acronym) {
        if(exclusionList.contains(acronym)){
            return true;
        }
        return false;
    }


    @Override
    public void sendRequestForMdrCodelistsStructures(Collection<String> acronymsList) {
        try {
            for(String actAcron : acronymsList){
                sendRequestForSingleMdrCodelistsStructure(actAcron);
            }
        } catch (MdrMappingException e) {
            log.error(ERROR_WHILE_TRYING_TO_MAP_MDRQUERY_TYPE_FOR_ACRONYM, acronymsList, e);
        } catch (MessageException e) {
            log.error("Error while trying to send OBJ_DESC message from MDR module to Rules module.", e);
        }
    }

    @Override
    public void sendRequestForSingleMdrCodelistsStructure(String actAcron) throws MdrMappingException, MessageException {
        String strReqObj = MdrRequestMapper.mapMdrQueryTypeToString(actAcron, OBJ_DESC, java.util.UUID.randomUUID().toString());
        producer.sendRulesModuleMessage(strReqObj);
    }

    @Override
    public void sendRequestForMdrCodelistsIndex() {
        try {
            String strReqObj = MdrRequestMapper.mapMdrQueryTypeToStringForINDEXServiceType(INDEX);
            producer.sendRulesModuleMessage(strReqObj);
            log.info("Synchronization Request Sent for INDEX ServiceType");
        } catch (MdrMappingException e) {
            log.error(ERROR_WHILE_TRYING_TO_MAP_MDRQUERY_TYPE_FOR_ACRONYM, e);
        } catch (MessageException e) {
            log.error("Error while trying to send message from MDR module to Rules module.", e);
        }
    }

    /**
     * Method that extracts all the available acronyms.
     *
     * @return acronymsList
     */
    @Override
    public List<String> getAvailableMdrAcronyms() {
        List<String> acronymsList = new ArrayList<>();
        try {
            acronymsList = MasterDataRegistryEntityCacheFactory.getAcronymsList();
            if (!CollectionUtils.isEmpty(acronymsList)) {
                log.info("Acronyms exctracted. \nThere were found [ " + acronymsList.size() + " ] acronyms in the MDR entities package.");
            }
            log.info("\n---> Exctracted : " + acronymsList.size() + " acronyms!\n");
        } catch (MdrCacheInitException exC) {
            log.error("Couldn't extract Entity Acronyms. The following Exception was thrown : \n", exC);
        }
        return acronymsList;
    }

}