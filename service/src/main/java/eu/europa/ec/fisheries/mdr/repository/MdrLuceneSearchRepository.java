/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.repository;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import java.util.List;
import javax.ejb.Local;

/**
 * Created by kovian on 15/12/2016.
 */
@Local
public interface MdrLuceneSearchRepository {

    int countCodeListItemsByAcronymAndFilter(String acronym, String filter, String... searchAttribute) throws ServiceException;

    List<? extends MasterDataRegistry> findCodeListItemsByAcronymAndFilter(String acronym, Integer offset, Integer pageSize, String sortBy, Boolean isReversed, String filter, String... searchAttributes) throws ServiceException;

    void massiveUpdateFullTextIndex() throws InterruptedException;
}
