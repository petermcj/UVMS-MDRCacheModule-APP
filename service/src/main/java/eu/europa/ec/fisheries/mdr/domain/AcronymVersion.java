package eu.europa.ec.fisheries.mdr.domain;

import eu.europa.ec.fisheries.uvms.domain.DateRange;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by kovian on 10/11/2016.
 */
@Entity
@Table(name = "mdr_acronymversion")
public class AcronymVersion implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_acronymversion_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "version_name")
    private String versionName;

    @Embedded
    private DateRange validity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "status_ref_id", nullable = false)
    private MdrCodeListStatus mdrCodeListStatus;

    public AcronymVersion() {
        super();
    }

    public AcronymVersion(String versionName, DateRange validity) {
        this.versionName = versionName;
        this.validity = validity;
    }

    public String getVersionName() {
        return versionName;
    }
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    public DateRange getValidity() {
        return validity;
    }
    public void setValidity(DateRange validity) {
        this.validity = validity;
    }
    public MdrCodeListStatus getMdrCodeListStatus() {
        return mdrCodeListStatus;
    }
    public void setMdrCodeListStatus(MdrCodeListStatus mdrCodeListStatus) {
        this.mdrCodeListStatus = mdrCodeListStatus;
    }
}