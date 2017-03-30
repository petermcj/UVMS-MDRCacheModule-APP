/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.repository.bean;

import eu.europa.ec.fisheries.mdr.dao.MasterDataRegistryDao;
import eu.europa.ec.fisheries.mdr.dao.MdrBulkOperationsDao;
import eu.europa.ec.fisheries.mdr.dao.MdrConfigurationDao;
import eu.europa.ec.fisheries.mdr.dao.MdrStatusDao;
import eu.europa.ec.fisheries.mdr.domain.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.domain.MdrConfiguration;
import eu.europa.ec.fisheries.mdr.domain.codelists.base.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.domain.constants.AcronymListState;
import eu.europa.ec.fisheries.mdr.mapper.MdrEntityMapper;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.uvms.common.DateUtils;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import un.unece.uncefact.data.standard.mdr.response.FLUXMDRReturnMessage;
import un.unece.uncefact.data.standard.mdr.response.FLUXResponseDocumentType;
import un.unece.uncefact.data.standard.mdr.response.IDType;
import un.unece.uncefact.data.standard.mdr.response.MDRDataSetType;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;

@Stateless
@Slf4j
@Transactional
public class MdrRepositoryBean implements MdrRepository {
	
	@PersistenceContext(unitName = "mdrPU")
    private EntityManager em;

	private MdrBulkOperationsDao bulkOperationsDao;
	
	private MdrConfigurationDao mdrConfigDao;

	private MdrStatusDao statusDao;

	private MasterDataRegistryDao mdrDao;

    @PostConstruct
    public void init() {
    	bulkOperationsDao = new MdrBulkOperationsDao(em);
    	mdrDao 			  = new MasterDataRegistryDao<>(em);
    	mdrConfigDao      = new MdrConfigurationDao(em);
		statusDao         = new MdrStatusDao(em);
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
	public void updateMdrEntity(FLUXMDRReturnMessage response){
		// Response is OK
		final FLUXResponseDocumentType fluxResponseDocument = response.getFLUXResponseDocument();
		if(!fluxResponseDocument.getResponseCode().toString().toUpperCase().equalsIgnoreCase("NOK")) {
			List<MasterDataRegistry> mdrEntityRows = MdrEntityMapper.mapJAXBObjectToMasterDataType(response);
			final MDRDataSetType mdrDataSet = response.getMDRDataSet();
			if (CollectionUtils.isNotEmpty(mdrEntityRows)) {
				try {
					insertNewData(mdrEntityRows);
					statusDao.updateStatusSuccessForAcronym(mdrDataSet, AcronymListState.SUCCESS, DateUtils.nowUTC().toDate());
				} catch (ServiceException e) {
					statusDao.updateStatusFailedForAcronym(mdrEntityRows.get(0).getAcronym());
					log.error("Transaction rolled back! Couldn't persist mdr Entity : ", e);
				}
			} else {
				log.error("Got Message from Flux related to MDR but, the list is empty! So, nothing is going to be persisted!");
			}
		// Response is NOT OK
		} else {
			final IDType referencedID = fluxResponseDocument.getReferencedID();
			if(referencedID != null && StringUtils.isNotEmpty(referencedID.getValue())){//, but has referenceID
				MdrCodeListStatus referencedStatus = statusDao.getStatusForUuid(referencedID.getValue());
				if(referencedStatus != null){
					statusDao.updateStatusFailedForAcronym(referencedStatus.getObjectAcronym());
				} else {
					log.error("[[ERROR]] The MDR response received in MDR module was OK, but the referenceId couldn't be found in status table!");
				}
			} else {//, and doesn't have referenceID
                log.error("[[ERROR]] The MDR response received in MDR module was NOK and has no referenceId to link it to!");
            }
		}
	}

	/**
	 * 	Method for saving the new mdrData (MDR entity rows).
	 *  Done in 2 steps so that we have 2 different transactions.
	 *  One for deleting and purging lucene indexes.
	 *  One for saving data and refreshing the indexes.
	 *  Otherwise lucene will double the size of the indexes, and with that the returned result set also!
	 *
	 * @param  mdrEntityRows
	 * @throws ServiceException
	 */
	@Override
	public void insertNewData(List<? extends MasterDataRegistry> mdrEntityRows) throws ServiceException {
		Class mdrClass       = mdrEntityRows.get(0).getClass();
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
	public List<MdrConfiguration> getAllConfigurations() throws ServiceException{
		return mdrConfigDao.findAllConfigurations();
	}
	
	@Override
	public MdrConfiguration getConfigurationByName(String configName) {
		return mdrConfigDao.findConfiguration(configName);
	}
	
	@Override
	@Transactional(Transactional.TxType.REQUIRED)
    public void changeMdrSchedulerConfiguration(String newCronExpression) throws ServiceException{
    	mdrConfigDao.changeMdrSchedulerConfiguration(newCronExpression);
    }
	
	@Override
	public MdrConfiguration getMdrSchedulerConfiguration(){
		return mdrConfigDao.getMdrSchedulerConfiguration();
	}
	
	/*
	 * MDR Acronym's statuses.
	 */
	@Override
    public List<MdrCodeListStatus> findAllStatuses() throws ServiceException {
        return statusDao.getAllAcronymsStatuses();
    }

	@Override
    public MdrCodeListStatus findStatusByAcronym(String acronym){
    	return statusDao.getStatusForAcronym(acronym);
    }

	private void saveNewEntriesAndRefreshIndexes(List<? extends MasterDataRegistry> entityRows, Class mdrClass) throws ServiceException {
		bulkOperationsDao.saveNewEntriesAndRefreshLuceneIndexes(mdrClass, entityRows);
	}

	private void deleteFromDbAndPurgeIndexes(Class mdrClass, String mdrEntityName) throws ServiceException {
		bulkOperationsDao.deleteFromDbAndPurgeAllFromIndex(mdrEntityName, mdrClass);
	}

}