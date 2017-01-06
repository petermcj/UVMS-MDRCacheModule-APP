/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.dao;

import eu.europa.ec.fisheries.mdr.domain.MdrConfiguration;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import eu.europa.ec.fisheries.uvms.service.AbstractDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Created by kovian on 17/08/2016.
 */
@Slf4j
public class MdrConfigurationDao extends AbstractDAO<MdrConfiguration> {

    private EntityManager em;
    
    private static final String SELECT_FROM_MDRCONFIG_WHERE_NAME_EQ = "from MdrConfiguration where configName = ";
    private static final String SCHEDULER_CONFIG_NAME               = "MDR_SCHED_CONFIG_NAME";

    public MdrConfigurationDao(EntityManager em) {
        this.em = em;
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }
    
    public List<MdrConfiguration> findAllConfigurations() throws ServiceException {
        return findAllEntity(MdrConfiguration.class);
    }

    public MdrConfiguration findConfiguration(String configName){
    	MdrConfiguration configEntry      = null;
        List<MdrConfiguration> configList = null;
        try {
            configList = findEntityByHqlQuery(MdrConfiguration.class, SELECT_FROM_MDRCONFIG_WHERE_NAME_EQ + "'"+configName+"'");
            if(CollectionUtils.isNotEmpty(configList)){
                configEntry =  configList.get(0);
            } else {
                log.error("No configuration found in the db regarding {} ", configName);
            }
        } catch (ServiceException | NullPointerException e) {
            log.error("Error while trying to get Configuration for configName : ", configName, e);
        }
        return configEntry;
    }
    
    public MdrConfiguration getMdrSchedulerConfiguration(){
    	return findConfiguration(SCHEDULER_CONFIG_NAME);
    }


    /**
     * Changes the mdr scheduler configuration with a new one.
     *
     * @param newCronExpression
     * @throws ServiceException
     */
    public void changeMdrSchedulerConfiguration(String newCronExpression) throws ServiceException {
        if(StringUtils.isEmpty(newCronExpression)){
            throw new ServiceException("Cron expression cannot be empty!");
        }
    	MdrConfiguration newConfig = getMdrSchedulerConfiguration();
        if(newConfig != null){
            newConfig.setConfigValue(newCronExpression);
        } else {
            MdrConfiguration newToSaveConfig = new MdrConfiguration(SCHEDULER_CONFIG_NAME, newCronExpression);
            saveOrUpdateEntity(newToSaveConfig);
        }
    }

}