/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.entities.codelists.baseentities;

import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import eu.europa.ec.fisheries.uvms.domain.DateRange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.*;
import un.unece.uncefact.data.standard.mdr.response.DelimitedPeriodType;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.TextType;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry.LOW_CASE_ANALYSER;

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

    protected static final String CODE_STR = ".CODE";
    protected static final String DESCRIPTION_STR = ".DESCRIPTION";
    protected static final String EN_DESCRIPTION_STR = ".ENDESCRIPTION";
    protected static final String VERSION_STR = ".VERSION";

    protected void populateCommonFields(MDRDataNodeType mdrDataType) throws FieldNotMappedException {

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
            if (StringUtils.contains(fieldName, CODE_STR)) {
                setCode(fieldValue);
                fieldsToRemove.add(field);
            } else if (StringUtils.contains(fieldName, DESCRIPTION_STR)
                    || StringUtils.contains(fieldName, EN_DESCRIPTION_STR)) {
                setDescription(fieldValue);
                fieldsToRemove.add(field);
            } else if (StringUtils.contains(fieldName, VERSION_STR)) {
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

    public abstract void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException;

    private String getValueFromTextType(TextType textType){
        return textType != null ? textType.getValue() : null;
    }

    public abstract String getAcronym();
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