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
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;

/**
 * Created by kovian on 01/09/2017.
 */
@Entity
@Table(name = "mdr_conversion_factor")
@Indexed
public class ConversionFactor extends MasterDataRegistry {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_conversion_factor_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "is_group")
    @Field(name = "is_group")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String isGroup;

    @Column(name = "scient_name")
    @Field(name = "scient_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String scientName;

    @Column(name = "en_name")
    @Field(name = "en_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String enName;

    @Column(name = "fr_name")
    @Field(name = "fr_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String frName;

    @Column(name = "es_name")
    @Field(name = "es_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String esName;

    @Column(name = "family")
    @Field(name = "family")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String family;

    @Column(name = "bioorder")
    @Field(name = "bioorder")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String bioorder;

    @Column(name = "taxocode")
    @Field(name = "taxocode")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String taxocode;

    @Column(name = "state")
    @Field(name = "state")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String state;

    @Column(name = "presentation")
    @Field(name = "presentation")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String presentation;

    @Column(name = "factor")
    @Field(name = "factor")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String factor;

    @Column(name = "places_code")
    @Field(name = "places_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String placesCode;

    @Column(name = "places_code2")
    @Field(name = "places_code2")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String placesCode2;

    @Column(name = "places_enname")
    @Field(name = "places_enname")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String placesEnname;

    @Column(name = "legal_source")
    @Field(name = "legal_source")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String legalSource;

    @Column(name = "collective")
    @Field(name = "collective")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String collective;

    @Column(name = "comment")
    @Field(name = "comment")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String comment;

    @Override
    public String getAcronym() {
        return "CONVERSION_FACTOR";
    }

    @Override
    public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
        populateCommonFields(mdrDataType);
        for (MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()) {
            String fieldName = field.getName().getValue();
            String fieldValue = field.getValue().getValue();
            if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.CODE")) {
                this.setCode(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.ISGROUP")) {
                setIsGroup(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.SCIENTNAME")) {
                this.setScientName(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.ENNAME")) {
                this.setEnName(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.FRNAME")) {
                this.setFrName(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.ESNAME")) {
                this.setEsName(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.FAMILY")) {
                this.setFamily(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.BIOORDER")) {
                this.setBioorder(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "ALL_SPECIES.TAXOCODE")) {
                this.setTaxocode(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.STATE")) {
                this.setState(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.PRESENTATION")) {
                this.setPresentation(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.FACTOR")) {
                this.setFactor(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "PLACES.CODE2")) {
                this.setPlacesCode2(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "PLACES.CODE")) {
                this.setPlacesCode(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "PLACES.ENNAME")) {
                this.setPlacesEnname(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.LEGALSOURCE")) {
                this.setLegalSource(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.COLLECTIVE")) {
                this.setCollective(fieldValue);
            } else if (StringUtils.equalsIgnoreCase(fieldName, "CONVERSION_FACTOR.COMMENT")) {
                this.setComment(fieldValue);
                this.setDescription(fieldValue);
            } else {
                logError(fieldName, this.getClass().getSimpleName());
            }
        }
    }

    public String getIsGroup() {
        return isGroup;
    }

    public void setIsGroup(String isGroup) {
        this.isGroup = isGroup;
    }

    public String getScientName() {
        return scientName;
    }

    public void setScientName(String scientName) {
        this.scientName = scientName;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getFrName() {
        return frName;
    }

    public void setFrName(String frName) {
        this.frName = frName;
    }

    public String getEsName() {
        return esName;
    }

    public void setEsName(String esName) {
        this.esName = esName;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getBioorder() {
        return bioorder;
    }

    public void setBioorder(String bioorder) {
        this.bioorder = bioorder;
    }

    public String getTaxocode() {
        return taxocode;
    }

    public void setTaxocode(String taxocode) {
        this.taxocode = taxocode;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPresentation() {
        return presentation;
    }

    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    public String getFactor() {
        return factor;
    }

    public void setFactor(String factor) {
        this.factor = factor;
    }

    public String getPlacesCode() {
        return placesCode;
    }

    public void setPlacesCode(String placesCode) {
        this.placesCode = placesCode;
    }

    public String getPlacesCode2() {
        return placesCode2;
    }

    public void setPlacesCode2(String placesCode2) {
        this.placesCode2 = placesCode2;
    }

    public String getPlacesEnname() {
        return placesEnname;
    }

    public void setPlacesEnname(String placesEnname) {
        this.placesEnname = placesEnname;
    }

    public String getLegalSource() {
        return legalSource;
    }

    public void setLegalSource(String legalSource) {
        this.legalSource = legalSource;
    }

    public String getCollective() {
        return collective;
    }

    public void setCollective(String collective) {
        this.collective = collective;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}