/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.entities.codelists.ers;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;

@Entity
@Table(name = "mdr_fa_br")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class FaBr extends MasterDataRegistry {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_fa_br_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "field")
    @Field(name = "field")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String field;

    @Column(name = "message_if_failing")
    @Field(name = "message_if_failing")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String messageIfFailing;

    @Column(name = "sequence_order")
    @Field(name = "sequence_order")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String sequenceOrder;

    @Column(name = "br_level_fk_x_key")
    @Field(name = "br_level_fk_x_key")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String brLevelFkXKey;

    @Column(name = "br_sublevel")
    @Field(name = "br_sublevel")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String brSublevel;

    @Column(name = "activation_indicator")
    @Field(name = "activation_indicator")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String activationIndicator;

    @Column(name = "error_message")
    @Field(name = "error_message")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String errorMessage;

    @Column(name = "flux_gp_validation_type_code")
    @Field(name = "flux_gp_validation_type_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String fluxGpValidationTypeCode;

    @Column(name = "flux_gp_validation_en_descr")
    @Field(name = "flux_gp_validation_en_descr")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String fluxGpValidationEnDescr;

    @Column(name = "context")
    @Field(name = "context")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String context;


    @Override
    public String getAcronym() {
        return "FA_BR";
    }

    @Override
    public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
        populateCommonFields(mdrDataType);
        for (MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()) {
            String fieldName = field.getName().getValue();
            String fieldValue = field.getValue().getValue();
            if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.CODE")) {
                this.setCode(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.ENDESCRIPTION")) {
                this.setDescription(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.FIELD")) {
                this.setField(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.ENMESSAGE")) {
                this.setMessageIfFailing(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.SEQORDER")) {
                this.setSequenceOrder(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.BRLEVELFK_X_KEY")) {
                this.setBrLevelFkXKey(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR_DEF.BRSUBLEVEL")) {
                this.setBrSublevel(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR.ACTIVEIND")) {
                this.setActivationIndicator(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR.ERROR_MESSAGE")) {
                this.setErrorMessage(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FA_BR.CONTEXT")) {
                this.setContext(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FLUX_GP_VALIDATION_TYPE.ENDESCRIPTION")) {
                this.setFluxGpValidationEnDescr(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "FLUX_GP_VALIDATION_TYPE.CODE")) {
                this.setFluxGpValidationTypeCode(fieldValue);
            } else {
                logError(fieldName, this.getClass().getSimpleName());
            }
        }
    }

    public String getField() {
        return field;
    }
    public void setField(String field) {
        this.field = field;
    }
    public String getMessageIfFailing() {
        return messageIfFailing;
    }
    public void setMessageIfFailing(String messageIfFailing) {
        this.messageIfFailing = messageIfFailing;
    }
    public String getSequenceOrder() {
        return sequenceOrder;
    }
    public void setSequenceOrder(String sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }
    public String getBrLevelFkXKey() {
        return brLevelFkXKey;
    }
    public void setBrLevelFkXKey(String brLevelFkXKey) {
        this.brLevelFkXKey = brLevelFkXKey;
    }
    public String getActivationIndicator() {
        return activationIndicator;
    }
    public void setActivationIndicator(String activationIndicator) {
        this.activationIndicator = activationIndicator;
    }
    public String getBrSublevel() {
        return brSublevel;
    }
    public void setBrSublevel(String brSublevel) {
        this.brSublevel = brSublevel;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public String getFluxGpValidationTypeCode() {
        return fluxGpValidationTypeCode;
    }
    public void setFluxGpValidationTypeCode(String fluxGpValidationTypeCode) {
        this.fluxGpValidationTypeCode = fluxGpValidationTypeCode;
    }
    public String getFluxGpValidationEnDescr() {
        return fluxGpValidationEnDescr;
    }
    public void setFluxGpValidationEnDescr(String fluxGpValidationEnDescr) {
        this.fluxGpValidationEnDescr = fluxGpValidationEnDescr;
    }
    public String getContext() {
        return context;
    }
    public void setContext(String context) {
        this.context = context;
    }
}
