/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.dao;

import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import eu.europa.ec.fisheries.mdr.entities.AcronymVersion;
import eu.europa.ec.fisheries.mdr.entities.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.entities.constants.AcronymListState;
import eu.europa.ec.fisheries.mdr.exception.AcronymNotFoundException;
import eu.europa.ec.fisheries.uvms.commons.date.DateUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

public class MdrStatusDaoTest extends BaseMdrDaoTest {

    private MdrStatusDao dao = new MdrStatusDao(em);

    @Before
    @SneakyThrows
    public void prepare(){
        Operation operation = sequenceOf(DELETE_ALL_MDR_CODELISTSTATUS, INSERT_MDR_CODELIST_STATUS, INSERT_ACRONYMS_VERSIONS);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(ds), operation);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    public void deleteAllCodeListStatuses(){
        Operation operation = sequenceOf(DELETE_ALL_MDR_CODELISTSTATUS);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(ds), operation);
        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    @SneakyThrows
    public void testGellAllAcronymStatuses() throws Exception {
        dbSetupTracker.skipNextLaunch();
        List<MdrCodeListStatus> allAcronymsStatuses = dao.getAllAcronymsStatuses();
        MdrCodeListStatus entity = allAcronymsStatuses.get(0);
        assertNotNull(entity);
    }

    @Test
    @SneakyThrows
    public void testFindStatusByAcronym(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym("ACTION_TYPE");
        assertNotNull(status);
    }

    @Test
    @SneakyThrows
    public void testFindStatusByAcronymNullReturn(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym(null);
        assertNull(status);
    }


    @Test
    @SneakyThrows
    public void testFindStatusAndRelatedVersionsByAcronym(){
        dbSetupTracker.skipNextLaunch();
        List<MdrCodeListStatus> actiontypeStatus = dao.findStatusAndVersionsForAcronym("ACTION_TYPE");
        Set<AcronymVersion> actiontypeVersions = actiontypeStatus.get(0).getVersions();
        assertEquals(2, actiontypeVersions.size());
        assertNotNull(actiontypeStatus.get(0));
    }

    @Test
    @SneakyThrows
    public void testFindAllUpdatableStatuses(){
        dbSetupTracker.skipNextLaunch();
        List<MdrCodeListStatus> status = dao.findAllUpdatableStatuses();
        assertEquals(2, status.size());
    }

    @Test
    @SneakyThrows
    public void testUpdateSchedulableForAcronym(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym("ACTION_TYPE");
        assertNotNull(status);
        dao.updateSchedulableForAcronym("ACTION_TYPE", false);
        MdrCodeListStatus status_1 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_1.getSchedulable().toString(), "false");
        dao.updateSchedulableForAcronym("ACTION_TYPE", true);
        MdrCodeListStatus status_2 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_2.getSchedulable().toString(), "true");
    }

    @Test
    @SneakyThrows
    public void testUpdateStatusAttemptForAcronym(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym("ACTION_TYPE");
        assertNotNull(status);
        dao.updateStatusForAcronym("ACTION_TYPE", AcronymListState.NEWENTRY, new Date(), "uuid");
        MdrCodeListStatus status_1 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_1.getLastStatus(), AcronymListState.NEWENTRY);
        dao.updateStatusForAcronym("ACTION_TYPE", AcronymListState.FAILED, new Date(), "uuid");
        MdrCodeListStatus status_2 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_2.getLastStatus(), AcronymListState.FAILED);
    }

    @Test
    @SneakyThrows
    public void testUpdateStatusFailedForAcronym(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym("ACTION_TYPE");
        assertNotNull(status);
        dao.updateStatusForAcronym("ACTION_TYPE", AcronymListState.FAILED);
        MdrCodeListStatus status_2 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_2.getLastStatus(), AcronymListState.FAILED);

    }

    @Test
    @SneakyThrows
    public void testUpdateStatusSuccessForAcronym(){
        dbSetupTracker.skipNextLaunch();
        MdrCodeListStatus status = dao.findStatusByAcronym("ACTION_TYPE");
        assertNotNull(status);
        dao.updateStatusSuccessForAcronym("ACTION_TYPE", AcronymListState.SUCCESS, DateUtils.nowUTC().toDate());
        MdrCodeListStatus status_2 = dao.findStatusByAcronym("ACTION_TYPE");
        assertEquals(status_2.getLastStatus(), AcronymListState.SUCCESS);

    }

    @Test
    @SneakyThrows
    public void testSaveAcronymsStatusList(){
        dao.saveAcronymsStatusList(mockMdrCodeListStatus());
        List<MdrCodeListStatus> allMdrStatuses = dao.findAllEntity(MdrCodeListStatus.class);
        assertEquals(8, allMdrStatuses.size());
        assertNotNull(dao.findStatusAndVersionsForAcronym("SomeAcronym1"));
    }

    @Test
    @SneakyThrows
    public void testFlushSaveEach20AcronymsStatusList(){
        dao.saveAcronymsStatusList(mock42MdrCodeListStatus());
        List<MdrCodeListStatus> allMdrStatuses = dao.findAllEntity(MdrCodeListStatus.class);
        assertEquals(45, allMdrStatuses.size());
        assertNotNull(dao.findStatusAndVersionsForAcronym("SomeAcronym1"));
    }

    @Test
    @SneakyThrows
    public void saveNotExistingAcronymValueFail(){
        try{
            dao.updateStatusSuccessForAcronym("BAD_ACRONYM", AcronymListState.SUCCESS, DateUtils.nowUTC().toDate());
            fail("It should have failed since the BAD_ACRONYM doesn't exist");
        } catch (AcronymNotFoundException ex){
            assertEquals("The acronym status BAD_ACRONYM you searched for is not present!", ex.getMessage());
        }
    }

    private List<MdrCodeListStatus> mockMdrCodeListStatus() {
        List<MdrCodeListStatus> statusesList = new ArrayList<>();
        for(int i=0; i<5; i++){
            MdrCodeListStatus mdrStatAct = new MdrCodeListStatus();
            mdrStatAct.setObjectAcronym("SomeAcronym"+i);
            mdrStatAct.setSchedulable(true);
            mdrStatAct.setLastStatus(AcronymListState.SUCCESS);
            mdrStatAct.setLastAttempt(DateUtils.nowUTC().toDate());
            mdrStatAct.setLastSuccess(DateUtils.nowUTC().toDate());
            statusesList.add(mdrStatAct);
        }
        return statusesList;
    }

    private List<MdrCodeListStatus> mock42MdrCodeListStatus() {
        List<MdrCodeListStatus> statusesList = new ArrayList<>();
        for(int i=0; i<42; i++){
            MdrCodeListStatus mdrStatAct = new MdrCodeListStatus();
            AcronymVersion version = new AcronymVersion();
            version.setMdrCodeListStatus(mdrStatAct);
            mdrStatAct.setObjectAcronym("SomeAcronym"+i);
            mdrStatAct.setSchedulable(true);
            mdrStatAct.setLastStatus(AcronymListState.SUCCESS);
            mdrStatAct.setLastAttempt(DateUtils.nowUTC().toDate());
            mdrStatAct.setLastSuccess(DateUtils.nowUTC().toDate());
            mdrStatAct.setVersions(new HashSet<>(Arrays.asList(version)));
            statusesList.add(mdrStatAct);
        }
        return statusesList;
    }


}
