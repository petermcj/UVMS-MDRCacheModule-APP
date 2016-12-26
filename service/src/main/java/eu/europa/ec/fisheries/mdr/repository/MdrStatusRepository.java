/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.repository;

import eu.europa.ec.fisheries.mdr.domain.MdrCodeListStatus;
import eu.europa.ec.fisheries.mdr.domain.constants.AcronymListState;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;

import javax.ejb.Local;
import java.util.Date;
import java.util.List;

/**
 * Created by kovian on 29/07/2016.
 */
@Local
public interface MdrStatusRepository {

    List<MdrCodeListStatus> getAllAcronymsStatuses();

    MdrCodeListStatus getStatusForAcronym(String acronym);

    void updateStatusAttemptForAcronym(String acronym, AcronymListState newStatus, Date lastAttempt);

    void updateStatusSuccessForAcronym(String acronym, AcronymListState newStatus, Date lastSuccess);

    void saveAcronymsStatusList(List<MdrCodeListStatus> diffList) throws ServiceException;

    List<MdrCodeListStatus> getAllUpdatableAcronymsStatuses();

    void updateStatusFailedForAcronym(String acronym);

    void updateSchedulableForAcronym(String acronym, boolean schedulable);
}