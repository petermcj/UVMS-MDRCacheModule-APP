/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.entities;

import static eu.europa.ec.fisheries.mdr.entities.CodeListStructure.DELETE_FROM_CODE_LIST_STRUCTURE;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Created by kovian on 06/10/2017.
 */
@Entity
@Table(name = "code_list_structure")
@NamedQueries({
        @NamedQuery(
                name = DELETE_FROM_CODE_LIST_STRUCTURE,
                query = "DELETE FROM CodeListStructure WHERE acronym=:acronym"
        )}
)
public class CodeListStructure implements Serializable {

    public static final String DELETE_FROM_CODE_LIST_STRUCTURE = "delete.from.codeliststructure";

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "code_list_structure_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "acronym")
    private String acronym;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update")
    private Date lastUpdate;

    @Column(name = "obj_def_message")
    private String objDefMessage;


    public CodeListStructure() {
        super();
    }

    public CodeListStructure(String acronym, Date lastUpdate, String objDefMessage) {
        this.acronym = acronym;
        this.lastUpdate = lastUpdate;
        this.objDefMessage = objDefMessage;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getAcronym() {
        return acronym;
    }
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }
    public Date getLastUpdate() {
        return lastUpdate;
    }
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    public String getObjDefMessage() {
        return objDefMessage;
    }
    public void setObjDefMessage(String objDefMessage) {
        this.objDefMessage = objDefMessage;
    }

}
