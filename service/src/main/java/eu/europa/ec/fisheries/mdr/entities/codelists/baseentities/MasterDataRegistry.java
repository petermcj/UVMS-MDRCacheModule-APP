/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.entities.codelists.baseentities;

import static eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry.LOW_CASE_ANALYSER;

import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import eu.europa.ec.fisheries.uvms.domain.DateRange;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.TokenFilterDef;
import org.hibernate.search.annotations.TokenizerDef;
import un.unece.uncefact.data.standard.mdr.response.DelimitedPeriodType;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.TextType;

@SuppressWarnings("serial")
@MappedSuperclass
@AnalyzerDef(name = LOW_CASE_ANALYSER,
        tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
        filters = {
                @TokenFilterDef(factory = StopFilterFactory.class, params = {
                        @Parameter(name = "ignoreCase", value = "true")
                }),
                @TokenFilterDef(factory = CommonGramsFilterFactory.class),
                @TokenFilterDef(factory = LowerCaseFilterFactory.class),
        })
@Slf4j
public abstract class MasterDataRegistry implements Serializable {

    public static final String LOW_CASE_ANALYSER = "lowCaseAnalyser";

    @Embedded
    @IndexedEmbedded
    private DateRange validity;

    @Column(name = "version")
    @Field(name = "version")
    private String version;

    @Column(name = "code")
    @Field(name = "code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String code;

    @Column(name = "description")
    @Field(name = "description")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String description;

    private static final String CODE_STR = ".CODE";
    private static final String DESCRIPTION_STR = ".DESCRIPTION";
    private static final String EN_DESCRIPTION_STR = ".ENDESCRIPTION";
    private static final String VERSION_STR = ".VERSION";

    // Fields that will contain [ACRONYM].[FIELD_NAME] values after calling populateDataNodeNames();.
    @Transient
    private String APP_CODE_STR;
    @Transient
    private String APP_DESCRIPTION_STR;
    @Transient
    private String APP_EN_DESCRIPTION_STR;
    @Transient
    private String APP_VERSION_STR;

    protected void populateCommonFields(MDRDataNodeType mdrDataType) throws FieldNotMappedException {

        populateDataNodeNames();

        // Start date end date
        final DelimitedPeriodType validityPeriod = mdrDataType.getEffectiveDelimitedPeriod();
        if (validityPeriod != null) {
            this.setValidity(new DateRange(validityPeriod.getStartDateTime().getDateTime().toGregorianCalendar().getTime(),
                    validityPeriod.getEndDateTime().getDateTime().toGregorianCalendar().getTime()));
        }

        // Code, Description, Version
        List<MDRElementDataNodeType> fieldsToRemove = new ArrayList<>();
        final List<MDRElementDataNodeType> subordinateMDRElementDataNodes = mdrDataType.getSubordinateMDRElementDataNodes();
        for (MDRElementDataNodeType field : subordinateMDRElementDataNodes) {
            String fieldName = getValueFromTextType(field.getName());
            String fieldValue = getValueFromTextType(field.getValue());
            if (StringUtils.equalsIgnoreCase(fieldName, APP_CODE_STR)) {
                setCode(fieldValue);
                fieldsToRemove.add(field);
            } else if (StringUtils.equalsIgnoreCase(fieldName, APP_DESCRIPTION_STR)
                    || StringUtils.equalsIgnoreCase(fieldName, APP_EN_DESCRIPTION_STR)) {
                setDescription(fieldValue);
                fieldsToRemove.add(field);
            } else if (StringUtils.equalsIgnoreCase(fieldName, APP_VERSION_STR)) {
                setVersion(fieldValue);
                fieldsToRemove.add(field);
            }
        }
        // If we are inside here it means that code and description have to be both set, otherwise we have attributes missing.
        if (StringUtils.isEmpty(code) || StringUtils.isEmpty(description)) {
            log.warn("[[WARNING]] Code or Description missing.");
        }
        subordinateMDRElementDataNodes.removeAll(fieldsToRemove);
    }

    /**
     * Populates the APP_CODE_STR ecc.
     * In the end they will have values like ACTION_TYPE.CODE, ACTION_TYPE.DESCRIPTION ecc..
     */
    private void populateDataNodeNames() {
        String acronym = getAcronym();
        APP_CODE_STR = acronym + CODE_STR;
        APP_DESCRIPTION_STR = acronym + DESCRIPTION_STR;
        APP_EN_DESCRIPTION_STR = acronym + EN_DESCRIPTION_STR;
        APP_VERSION_STR = acronym + VERSION_STR;
    }

    protected void logError(String fieldName, String className) {
        log.error("The field '" + fieldName + "' for Codelist : " + className + " has not been mapped!");
    }


    public abstract void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException;

    public abstract String getAcronym();

    private String getValueFromTextType(TextType textType) {
        return textType != null ? textType.getValue() : null;
    }
    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }
    public DateRange getValidity() {
        return validity;
    }
    public void setValidity(DateRange validity) {
        this.validity = validity;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

}