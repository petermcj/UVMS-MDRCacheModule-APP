/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.entities;

import eu.europa.ec.fisheries.mdr.converter.CharAcronymListStateConverter;
import eu.europa.ec.fisheries.mdr.entities.constants.AcronymListState;
import eu.europa.ec.fisheries.uvms.commons.domain.CharBooleanConverter;
import eu.europa.ec.fisheries.uvms.commons.domain.DateRange;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang.StringUtils;


/**
 * @author kovian
 * 
 * Entity that has to be updated each time a Code_List has been synchronized.
 * If the update of an entity was succesful then the fields : last_update,last_attempt
 * should have the same value.
 */
@NamedQueries({
        @NamedQuery(name = MdrCodeListStatus.STATUS_AND_VERSIONS_QUERY,
                query = "SELECT status " +
                        "FROM MdrCodeListStatus status " +
                        "JOIN FETCH status.versions versions " +
                        "WHERE status.objectAcronym=:objectAcronym "),
        @NamedQuery(name = MdrCodeListStatus.STATUS_FOR_UUID,
                query = "SELECT status " +
                        "FROM MdrCodeListStatus status " +
                        "WHERE status.referenceUuid= :uuid ")
})
@Entity
@Table(name = "mdr_codelist_status")
public class MdrCodeListStatus implements Serializable {

    public static final String STATUS_AND_VERSIONS_QUERY = "statusAndVersions";
    public static final String STATUS_FOR_UUID           = "statusForUuid";

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_codelist_status_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "object_acronym")
    private String objectAcronym;
    
    @Column(name = "object_name")
    private String objectName;

    @Column(name = "object_description")
    private String objectDescription;

    @Column(name = "object_source")
    private String objectSource;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_attempt")
    private Date lastAttempt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_success")
    private Date lastSuccess;

    @Embedded
    private DateRange validity;

    @Column(name = "last_status")
    @Convert(converter = CharAcronymListStateConverter.class)
    private AcronymListState lastStatus;

    @Column(name = "schedulable", length = 1)
    @Convert(converter = CharBooleanConverter.class)
    private Boolean schedulable;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "mdrCodeListStatus", cascade = CascadeType.ALL)
    private Set<AcronymVersion> versions;

    @Column(name = "reference_uuid")
    private String referenceUuid;

    public MdrCodeListStatus(){super();}

    public MdrCodeListStatus(String objectAcronym, String objectName, Date lastUpdate, Date lastAttempt, AcronymListState state, Boolean schedulable) {
        this.objectAcronym = objectAcronym;
        this.objectName  = objectName;
        this.lastSuccess = lastUpdate;
        this.lastAttempt = lastAttempt;
        this.lastStatus = state;
        this.schedulable = schedulable;
        this.referenceUuid = StringUtils.EMPTY;
    }

    public String getObjectAcronym() {
        return objectAcronym;
    }
    public void setObjectAcronym(String objectAcronym) {
        this.objectAcronym = objectAcronym;
    }
    public Date getLastSuccess() {
        return lastSuccess;
    }
    public void setLastSuccess(Date lastUpdate) {
        this.lastSuccess = lastUpdate;
    }
    public AcronymListState getLastStatus() {
        return lastStatus;
    }
    public void setLastStatus(AcronymListState state) {
        this.lastStatus = state;
    }
    public Boolean getSchedulable() {
        return schedulable;
    }
    public void setSchedulable(Boolean updatable) {
        this.schedulable = updatable;
    }
    public Date getLastAttempt() {
        return lastAttempt;
    }
    public void setLastAttempt(Date lastAttempt) {
        this.lastAttempt = lastAttempt;
    }
	public String getObjectName() {
		return objectName;
	}
	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
    public String getObjectDescription() {
        return objectDescription;
    }
    public void setObjectDescription(String objectDescription) {
        this.objectDescription = objectDescription;
    }
    public String getObjectSource() {
        return objectSource;
    }
    public void setObjectSource(String objectSource) {
        this.objectSource = objectSource;
    }
    public DateRange getValidity() {
        return validity;
    }
    public void setValidity(DateRange validity) {
        this.validity = validity;
    }
    public Set<AcronymVersion> getVersions() {
        return versions;
    }
    public void setVersions(Set<AcronymVersion> versions) {
        this.versions = versions;
    }
    public String getReferenceUuid() {
        return referenceUuid;
    }
    public void setReferenceUuid(String reference_uuid) {
        this.referenceUuid = reference_uuid;
    }
}
