/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.dao;

import javax.persistence.EntityManager;
import java.util.HashMap;

import eu.europa.ec.fisheries.mdr.entities.CodeListStructure;
import eu.europa.ec.fisheries.uvms.commons.service.dao.AbstractDAO;
import eu.europa.ec.fisheries.uvms.commons.service.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by kovian on 06/10/2017.
 */
@Slf4j
public class CodeListStructureDao extends AbstractDAO<CodeListStructure> {

    private static final String HQL_DELETE_FROM = "DELETE FROM CodeListStructure WHERE acronym=:acronym";

    private EntityManager em;

    public CodeListStructureDao() {
        super();
    }

    public CodeListStructureDao(EntityManager entityManager) {
        setEm(entityManager);
    }

    public void saveStructureMessage(final CodeListStructure structure) {
        try {
            deleteEntityByNamedQuery(CodeListStructure.class, CodeListStructure.DELETE_FROM_CODE_LIST_STRUCTURE, new HashMap<String, Object>(){{put("acronym", structure.getAcronym());}});
            createEntity(structure);
        } catch (ServiceException e) {
            log.error("Error during saving of CodeListStructure entity!");
        }
    }


    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public void setEm(EntityManager em) {
        this.em = em;
    }
}
