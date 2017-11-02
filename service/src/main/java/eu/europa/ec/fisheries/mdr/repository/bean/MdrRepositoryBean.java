/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.repository.bean;

import eu.europa.ec.fisheries.mdr.dao.CodeListStructureDao;
import eu.europa.ec.fisheries.mdr.dao.MasterDataRegistryDao;
import eu.europa.ec.fisheries.mdr.dao.MdrBulkOperationsDao;
import eu.europa.ec.fisheries.mdr.dao.MdrConfigurationDao;
import eu.europa.ec.fisheries.mdr.dao.MdrStatusDao;
import eu.europa.ec.fisheries.mdr.entities.CodeListStructure;
import eu.europa.ec.fisheries.mdr.entities.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.entities.MdrConfiguration;
import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.entities.constants.AcronymListState;
import eu.europa.ec.fisheries.mdr.mapper.MdrEntityMapper;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.service.bean.BaseMdrBean;
import eu.europa.ec.fisheries.uvms.commons.date.DateUtils;
import eu.europa.ec.fisheries.uvms.commons.service.exception.ServiceException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;
import un.unece.uncefact.data.standard.mdr.response.FLUXResponseDocumentType;
import un.unece.uncefact.data.standard.mdr.response.IDType;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRDataSetType;

@Stateless
@Slf4j
@Transactional
public class MdrRepositoryBean extends BaseMdrBean implements MdrRepository {

    private MdrBulkOperationsDao bulkOperationsDao;

    private MdrConfigurationDao mdrConfigDao;

    private MdrStatusDao statusDao;

    private MasterDataRegistryDao mdrDao;

    private CodeListStructureDao structDao;

    @PostConstruct
    public void init() {
        initEntityManager();
        bulkOperationsDao = new MdrBulkOperationsDao(getEntityManager());
        mdrDao = new MasterDataRegistryDao<>(getEntityManager());
        mdrConfigDao = new MdrConfigurationDao(getEntityManager());
        statusDao = new MdrStatusDao(getEntityManager());
        structDao = new CodeListStructureDao(getEntityManager());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends MasterDataRegistry> List<T> findAllForEntity(Class<T> mdr) throws ServiceException {
        return mdrDao.findAllEntity(mdr);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends MasterDataRegistry> List<T> findEntityByHqlQuery(Class<T> type, String hqlQuery, Map<Integer, String> parameters,
                                                                       int maxResultLimit) throws ServiceException {
        return mdrDao.findEntityByHqlQuery(type, hqlQuery, parameters, maxResultLimit);
    }


    /**
     * Saves the codeList received from this message. Only if the response is ok it will be saved, and the status will be changed to SUCCESS.
     * Otherwise the status for that codeList will be changed to failed.
     *
     * @param response
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void updateMdrEntity(FLUXMDRReturnMessage response) {
        final FLUXResponseDocumentType fluxResponseDocument = response.getFLUXResponseDocument();

        // Response is OK
        if (!"NOK".equals(fluxResponseDocument.getResponseCode().toString().toUpperCase())) {
            if (isBigList(response)) { // In case of big lists we need to save chunks of data at a time
                saveBigList(response);
            } else {
                saveSmallList(response, fluxResponseDocument);
            }
            // Response is NOT OK
        } else {
            log.error("Response was not ok (NOK) so nothing is going to be persisted.");
            statusFailedOrEmptyForAcronym(fluxResponseDocument, AcronymListState.FAILED);
        }
    }

    private void saveBigList(FLUXMDRReturnMessage response) {
        log.info("\n\n[START] Received BIG List. Going to chunk it ::::::::::::::::::::::::::::::");
        List<List<MasterDataRegistry>> chuncksList = getDataNodesAsChunks(response.getMDRDataSet().getContainedMDRDataNodes(), response.getMDRDataSet().getID().getValue(), 500);
        log.info("[END] Finished chunking BIG list ::::::::::::::::::::::::::::::\n\n");
        try {
            // Deletion phase
            deleteDataAndPurgeIndexes(chuncksList.get(0));

            // Insertion phase
            int counter = 0;
            for (List<MasterDataRegistry> chunkOfMdrEntityRows : chuncksList) {
                log.info("Inserting chunk : " + counter++);
                insertNewDataWithoutPurging(chunkOfMdrEntityRows);
            }
            statusDao.updateStatusSuccessForAcronym(response.getMDRDataSet(), AcronymListState.SUCCESS, DateUtils.nowUTC().toDate());
        } catch (ServiceException e) {
            statusDao.updateStatusForAcronym(chuncksList.get(0).get(0).getAcronym(), AcronymListState.FAILED);
            log.error("Transaction rolled back! Couldn't persist mdr Entity : ", e);
        }


    }

    private void saveSmallList(FLUXMDRReturnMessage response, FLUXResponseDocumentType fluxResponseDocument) {
        List<MasterDataRegistry> mdrEntityRows = MdrEntityMapper.mapJAXBObjectToMasterDataType(response);
        if (CollectionUtils.isNotEmpty(mdrEntityRows)) {
            log.info("START [STAT] : Received and going to save [ - " + mdrEntityRows.size() + " - ] Rows of the entity [ " + mdrEntityRows.get(0).getAcronym() + " ]");
            try {
                insertNewData(mdrEntityRows);
                statusDao.updateStatusSuccessForAcronym(response.getMDRDataSet(), AcronymListState.SUCCESS, DateUtils.nowUTC().toDate());
            } catch (ServiceException e) {
                statusDao.updateStatusForAcronym(mdrEntityRows.get(0).getAcronym(), AcronymListState.FAILED);
                log.error("Transaction rolled back! Couldn't persist mdr Entity : ", e);
            }
            log.info("END [STAT] : Succesfully saved [ - " + mdrEntityRows.size() + " - ] Rows of the entity [ " + mdrEntityRows.get(0).getAcronym() + " ]");
        } else { // List is empty
            log.error("[[ ERROR ]] Got Message from Flux related to MDR but, the list is empty! So, nothing is going to be persisted!");
            statusFailedOrEmptyForAcronym(fluxResponseDocument, AcronymListState.EMPTY);
        }
    }

    /**
     * Chunks the data in chunks of chunkSize size.
     *
     * @param containedMDRDataNodes
     * @param acronym
     * @param chunkSize
     * @return chunked data
     */
    public List<List<MasterDataRegistry>> getDataNodesAsChunks(List<MDRDataNodeType> containedMDRDataNodes, String acronym, int chunkSize) {
        log.info("The BIG lists size is : [[ " + containedMDRDataNodes.size() + " ]]");
        List<List<MasterDataRegistry>> chunks = new ArrayList<>();
        createChunks(chunks, containedMDRDataNodes, containedMDRDataNodes.size(), acronym, 0, chunkSize);
        log.info("Created : [[ " + chunks.size() + " ]] chunks of [[ " + chunkSize + " ]] dataNodes each.");
        return chunks;
    }

    private void createChunks(List<List<MasterDataRegistry>> chunks, List<MDRDataNodeType> dataNodes, int listSize, String acronym, int chunkStart, int chunkSize) {
        List<MDRDataNodeType> chunk = new ArrayList<>();
        int i;
        for (i = chunkStart; i < chunkStart + chunkSize && i < listSize; i++) {
            chunk.add(dataNodes.get(i));
        }
        chunks.add(MdrEntityMapper.mapJaxbToMDRType(chunk, acronym));
        if (i < listSize) {
            createChunks(chunks, dataNodes, listSize, acronym, i, chunkSize);
        }
    }

    /**
     * Checks if the list is too big to be saved all in one time.
     *
     * @param response
     * @return true if list is big (> 5000 entries), false otherwise
     */
    private boolean isBigList(FLUXMDRReturnMessage response) {
        MDRDataSetType mdrDataSet = response.getMDRDataSet();
        if (mdrDataSet == null || CollectionUtils.isEmpty(mdrDataSet.getContainedMDRDataNodes())) {
            return false;
        }
        return mdrDataSet.getContainedMDRDataNodes().size() > 3000;
    }

    /**
     * This method sets the status of the related Acronym (by uuid match) to failed.
     *
     * @param fluxResponseDocument
     */
    @Transactional(Transactional.TxType.REQUIRED)
    private void statusFailedOrEmptyForAcronym(FLUXResponseDocumentType fluxResponseDocument, AcronymListState state) {
        log.info("Going to set the status FAILED for the related acronym if referenceId will be found!");
        final IDType referencedID = fluxResponseDocument.getReferencedID();
        if (referencedID != null && StringUtils.isNotEmpty(referencedID.getValue())) {//, but has referenceID to relate which acronym failed
            log.info("Reference ID different then null");
            MdrCodeListStatus referencedStatus = statusDao.getStatusForUuid(referencedID.getValue());
            log.info("Found MdrCodeListStatus for related ref ID : " + referencedStatus.getObjectAcronym());
            if (referencedStatus != null) {
                log.info("Going to set the status FAILED for acronym : " + referencedStatus.getObjectAcronym());
                statusDao.updateStatusForAcronym(referencedStatus.getObjectAcronym(), state);
            } else {
                log.error("[[ERROR]] The MDR response received in MDR module was OK, but the referenceId couldn't be found in status table!");
            }
        } else {//, and doesn't have referenceID
            log.error("[[ERROR]] The MDR response received in MDR module was NOK and has no referenceId to link it to!");
        }
    }

    /**
     * Method for saving the new mdrData (MDR entity rows) without deleting previous data.
     *
     * @param mdrEntityRows
     * @throws ServiceException
     */
    @Override
    public void insertNewDataWithoutPurging(List<? extends MasterDataRegistry> mdrEntityRows) throws ServiceException {
        saveNewEntriesAndRefreshIndexes(mdrEntityRows, mdrEntityRows.get(0).getClass());
    }

    @Override
    public void deleteDataAndPurgeIndexes(List<? extends MasterDataRegistry> mdrEntityRows) throws ServiceException {
        Class mdrClass = mdrEntityRows.get(0).getClass();
        deleteFromDbAndPurgeIndexes(mdrClass, mdrClass.getSimpleName());
    }

    /**
     * Method for saving the new mdrData (MDR entity rows).
     * Done in 2 steps so that we have 2 different transactions.
     * One for deleting and purging lucene indexes.
     * One for saving data and refreshing the indexes.
     * Otherwise lucene will double the size of the indexes, and with that the returned result set also!
     *
     * @param mdrEntityRows
     * @throws ServiceException
     */
    @Override
    public void insertNewData(List<? extends MasterDataRegistry> mdrEntityRows) throws ServiceException {
        Class mdrClass = mdrEntityRows.get(0).getClass();
        String mdrEntityName = mdrClass.getSimpleName();
        // Deletion phase;
        deleteFromDbAndPurgeIndexes(mdrClass, mdrEntityName);
        // Insertion phase;
        saveNewEntriesAndRefreshIndexes(mdrEntityRows, mdrClass);
    }

    /*
     * MDR Configurations related methods.
     */
    @Override
    public List<MdrConfiguration> getAllConfigurations() throws ServiceException {
        return mdrConfigDao.findAllConfigurations();
    }

    @Override
    public MdrConfiguration getConfigurationByName(String configName) {
        return mdrConfigDao.findConfiguration(configName);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void changeMdrSchedulerConfiguration(String newCronExpression) throws ServiceException {
        mdrConfigDao.changeMdrSchedulerConfiguration(newCronExpression);
    }

    @Override
    public MdrConfiguration getMdrSchedulerConfiguration() {
        return mdrConfigDao.getMdrSchedulerConfiguration();
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void saveAcronymStructureMessage(String messageStr, String acronym) {
        structDao.saveStructureMessage(new CodeListStructure(acronym, new Date(), messageStr));
    }


    @Override
    public void updateMetaDataForAcronym(MDRDataSetType metaData) {
        statusDao.updateMetadataForAcronym(metaData);
    }

    /*
     * MDR Acronym's statuses.
     */
    @Override
    public List<MdrCodeListStatus> findAllStatuses() throws ServiceException {
        return statusDao.getAllAcronymsStatuses();
    }

    @Override
    public MdrCodeListStatus findStatusByAcronym(String acronym) {
        return statusDao.getStatusForAcronym(acronym);
    }

    private void saveNewEntriesAndRefreshIndexes(List<? extends MasterDataRegistry> entityRows, Class mdrClass) throws ServiceException {
        bulkOperationsDao.saveNewEntriesAndRefreshLuceneIndexes(mdrClass, entityRows);
    }

    private void deleteFromDbAndPurgeIndexes(Class mdrClass, String mdrEntityName) throws ServiceException {
        bulkOperationsDao.deleteFromDbAndPurgeAllFromIndex(mdrEntityName, mdrClass);
    }

}