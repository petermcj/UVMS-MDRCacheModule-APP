/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.service.bean;

import static javax.ejb.ConcurrencyManagementType.BEAN;

import eu.europa.ec.fisheries.mdr.entities.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.entities.MdrConfiguration;
import eu.europa.ec.fisheries.mdr.entities.constants.AcronymListState;
import eu.europa.ec.fisheries.mdr.exception.MdrCacheInitException;
import eu.europa.ec.fisheries.mdr.exception.MdrStatusTableException;
import eu.europa.ec.fisheries.mdr.mapper.MasterDataRegistryEntityCacheFactory;
import eu.europa.ec.fisheries.mdr.repository.MdrLuceneSearchRepository;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.repository.MdrStatusRepository;
import eu.europa.ec.fisheries.mdr.service.MdrSchedulerService;
import eu.europa.ec.fisheries.mdr.service.MdrSynchronizationService;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

/**
 * Created by kovian on 29/07/2016.
 */
@Slf4j
@Singleton
@Startup
@ConcurrencyManagement(BEAN)
@DependsOn(value = {"MdrSynchronizationServiceBean", "MdrStatusRepositoryBean", "MdrRepositoryBean", "MdrSchedulerServiceBean"})
public class MdrInitializationBean {

    private static final String FIXED_SCHED_CONFIGURATION = "0 1 20 * *";

    @EJB
    private MdrSynchronizationService synchBean;

    @EJB
    private MdrSchedulerService schedulerBean;

    @EJB
    private MdrStatusRepository mdrStatusRepository;

    @EJB
    private MdrRepository mdrRepository;

    @EJB
    MdrLuceneSearchRepository mdrSearchRepository;

    /**
     * Method for start up Jobs of MDR module Module (Deploy phase)
     *
     *  1. Initializing the acronyms cache.
     *  2. Setting up the scheduler timer for MDR synchronization at start up.
     *  3. Updating the acronyms status table (with eventually new added entities (acronyms)).
     *
     */
    @PostConstruct
    public void startUpMdrInitializationProcess() throws InterruptedException {

        long startTime  = System.currentTimeMillis();

        log.info("[START] Starting up MDR moduleModule Initialization..");
        log.info("Going to : \n\t\t1. Initailize acronymsCache..\n\t\t2. Update MDR status table..\n\t\t3. Set up MDR scheduler..  ");

        // 1. Initializing the acronyms cache;
        log.info("\n\n\t\t1. Initailizing acronymsCache..\n");
        MasterDataRegistryEntityCacheFactory.initialize();

        // 2. Updating the acronyms status table (with eventually new added entities (acronyms, aka. codeLists)).
        log.info("\n\n\t\t2. Updating MDR status table..\n");
        try {
            updateMdrStatusTable();
        } catch (MdrStatusTableException e) {
            log.error("Exception occured while attempting to update the Status table at MDR module module deploy phase!", e);
        }

        // 3. Setting up the scheduler timer for MDR synchronization at start up.
        log.info("\n\n\t\t3. Starting up MDR Synchronization Scheduler Initialization..\n");
        // Get the scheduler config from DB;
        MdrConfiguration storedMdrSchedulerConfig = mdrRepository.getMdrSchedulerConfiguration();
        try {
            if(storedMdrSchedulerConfig != null){
                log.info("\n\n\t\t3.1. Creating scheduler from cached expression.\n");
                schedulerBean.setUpScheduler(storedMdrSchedulerConfig.getConfigValue());
            } else {
                log.info("\n\n\t\t3.2. Creating scheduler from fixed expression, because there was no config stored in DB..\n");
                schedulerBean.reconfigureScheduler(FIXED_SCHED_CONFIGURATION);
            }
        } catch(Exception ex){
            log.debug("\n\n\t\t Creating scheduler threw the following error : \n", ex);
        }

        // 4. Reindex lucene (this is particularly useful when the data comes from a back-up)
        try {
            log.info("\n\n\t\t4. Reindexing Lucene indexes.\n");
            mdrSearchRepository.massiveUpdateFullTextIndex();
        } catch (InterruptedException e) {
            log.error("An error occured while calli [ MdrLuceneSearchRepository.massiveUpdateFullTextIndex ]", e);
            throw  e;
        }

        log.info("[END] Finished Starting up MDR moduleModule Initialization.");
        log.debug("\n\n -- It Took " + (System.currentTimeMillis() - startTime) + " milliseconds for startUp activities to finish.. -- \n\n");
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    private void updateMdrStatusTable() throws MdrStatusTableException {
        List<String> acronymsFromCache;
        List<String> statusListFromDb;
        List<MdrCodeListStatus> diffList = new ArrayList<>();

        try {
            acronymsFromCache = MasterDataRegistryEntityCacheFactory.getAcronymsList();
            statusListFromDb  = extractStringListFromAcronymStatusList(mdrStatusRepository.getAllAcronymsStatuses());
        } catch (MdrCacheInitException e) {
            log.error("Error occurred in updateMdrStatusTable() while attempting to get AcronymsList.",e);
            throw new MdrStatusTableException(e.getMessage(), e.getCause());
        }

        for (String actualCacheAcronym : acronymsFromCache) {
            if (!statusListFromDb.contains(actualCacheAcronym)) {
                diffList.add(createNewAcronymSatus(actualCacheAcronym));
            }
        }

        if (CollectionUtils.isNotEmpty(diffList)) {
            try {
                mdrStatusRepository.saveAcronymsStatusList(diffList);
            } catch (ServiceException e) {
                log.error("Error occurred while attempting to save the AcronymsStatusList.",e);
            }
        } else {
            log.info("No new Acronyms were found for insertion into the MdrCodeListStatus Table..");
        }
    }

    private MdrCodeListStatus createNewAcronymSatus(String actualCacheAcronym) {
        return new MdrCodeListStatus(actualCacheAcronym, "", null, null, AcronymListState.NEWENTRY, true);
    }

    private List<String> extractStringListFromAcronymStatusList(List<MdrCodeListStatus> allAcronymsStatuses) {
        List<String> extractedList = new ArrayList<>();
        for(MdrCodeListStatus status : allAcronymsStatuses){
            extractedList.add(status.getObjectAcronym());
        }
        return extractedList;
    }
}
