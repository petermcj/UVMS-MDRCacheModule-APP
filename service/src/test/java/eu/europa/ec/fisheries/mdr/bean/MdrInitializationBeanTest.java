/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.bean;

import eu.europa.ec.fisheries.mdr.dao.BaseMdrDaoTest;
import eu.europa.ec.fisheries.mdr.repository.MdrStatusRepository;
import eu.europa.ec.fisheries.mdr.repository.bean.MdrLuceneSearchRepositoryBean;
import eu.europa.ec.fisheries.mdr.repository.bean.MdrRepositoryBean;
import eu.europa.ec.fisheries.mdr.repository.bean.MdrStatusRepositoryBean;
import eu.europa.ec.fisheries.mdr.service.MdrSchedulerService;
import eu.europa.ec.fisheries.mdr.service.MdrSynchronizationService;
import eu.europa.ec.fisheries.mdr.service.bean.BaseMdrBean;
import eu.europa.ec.fisheries.mdr.service.bean.MdrInitializationBean;
import eu.europa.ec.fisheries.mdr.service.bean.MdrSchedulerServiceBean;
import eu.europa.ec.fisheries.mdr.service.bean.MdrSynchronizationServiceBean;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.IMdrMessageProducer;
import eu.europa.ec.fisheries.uvms.mdr.message.producer.MdrMessageProducerBean;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;

import javax.ejb.TimerService;

/**
 * Created by kovian on 06/01/2017.
 */
public class MdrInitializationBeanTest extends BaseMdrDaoTest {

    private MdrInitializationBean initBean = new MdrInitializationBean();

    private MdrSynchronizationService synchBean = new MdrSynchronizationServiceBean();
    private MdrStatusRepository statusRepository = new MdrStatusRepositoryBean();
    private IMdrMessageProducer producer = new MdrMessageProducerBean();
    private MdrLuceneSearchRepositoryBean mdrSearchRepository = new MdrLuceneSearchRepositoryBean();
    private MdrSchedulerService schedulerBean = new MdrSchedulerServiceBean();

    @Mock
    private TimerService timerServ;


    private MdrStatusRepositoryBean mdrStatusRepository = new MdrStatusRepositoryBean();
    private MdrRepositoryBean mdrRepository = new MdrRepositoryBean();

    private BaseMdrBean baseBean;


    @Before
    @SneakyThrows
    public void prepare() {
        // SyncBean internal state
        Whitebox.setInternalState(synchBean, "mdrRepository", mdrRepository);
        Whitebox.setInternalState(synchBean, "statusRepository", statusRepository);
        Whitebox.setInternalState(synchBean, "producer", producer);

        // SchedulerBean internal state
        Whitebox.setInternalState(schedulerBean, "mdrRepository", mdrRepository);
        Whitebox.setInternalState(schedulerBean, "synchBean", synchBean);
        Whitebox.setInternalState(schedulerBean, "timerServ", timerServ);

        Whitebox.setInternalState(mdrStatusRepository, "postgres", em);
        Whitebox.setInternalState(mdrRepository, "postgres", em);
        Whitebox.setInternalState(mdrSearchRepository, "postgres", em);

        // initBean internal state
        Whitebox.setInternalState(initBean, "synchBean", synchBean);
        Whitebox.setInternalState(initBean, "schedulerBean", schedulerBean);
        Whitebox.setInternalState(initBean, "mdrStatusRepository", mdrStatusRepository);
        Whitebox.setInternalState(initBean, "mdrRepository", mdrRepository);
        Whitebox.setInternalState(initBean, "mdrSearchRepository", mdrSearchRepository);

        mdrStatusRepository.init();
        mdrRepository.init();
        mdrSearchRepository.init();
    }

    @Test
    @SneakyThrows
    public void testStartUpMdrInitializationProcess() {
            initBean.startUpMdrInitializationProcess();
    }

}
