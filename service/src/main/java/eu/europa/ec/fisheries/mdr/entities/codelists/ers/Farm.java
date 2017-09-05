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

/**
 * Created by kovian on 22/03/2017.
 */

@Entity
@Table(name = "mdr_farm")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class Farm extends MasterDataRegistry {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_farm_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "iso_2_code")
    @Field(name="iso_2_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String iso2Code;

    @Column(name = "en_name")
    @Field(name="en_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String enName;

    @Column(name = "place_fk_x_key")
    @Field(name="place_fk_x_key")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String placeFkXKey;

    @Column(name = "contracting_party")
    @Field(name="contracting_party")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String contractingParty;

    @Column(name = "legal_reference")
    @Field(name="legal_reference")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String legalReference;


    @Override
    public String getAcronym() {
        return "FARM";
    }

    @Override
    public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
        populateCommonFields(mdrDataType);
        for(MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()){
            String fieldName  = field.getName().getValue();
            String fieldValue  = field.getValue().getValue();
            if(StringUtils.equalsIgnoreCase(fieldName, "FARM.CODE2")){
                this.setLegalReference(fieldValue);
            } else if(StringUtils.equalsIgnoreCase(fieldName, "FARM.ENNAME")){
                this.setLegalReference(fieldValue);
            } else if(StringUtils.equalsIgnoreCase(fieldName, "FARM.PLACEFK_X_KEY")){
                this.setLegalReference(fieldValue);
            } else if(StringUtils.equalsIgnoreCase(fieldName, "FARM.ENDESCRIPTION")){
                this.setLegalReference(fieldValue);
            } else if(StringUtils.equalsIgnoreCase(fieldName, "FARM.CONTRACTINGPARTY")){
                this.setLegalReference(fieldValue);
            }  else {
                logError(fieldName, this.getClass().getSimpleName());
            }
        }
    }

    public String getIso2Code() {
        return iso2Code;
    }
    public void setIso2Code(String iso2Code) {
        this.iso2Code = iso2Code;
    }
    public String getEnName() {
        return enName;
    }
    public void setEnName(String enName) {
        this.enName = enName;
    }
    public String getPlaceFkXKey() {
        return placeFkXKey;
    }
    public void setPlaceFkXKey(String placeFkXKey) {
        this.placeFkXKey = placeFkXKey;
    }
    public String getContractingParty() {
        return contractingParty;
    }
    public void setContractingParty(String contractingParty) {
        this.contractingParty = contractingParty;
    }
    public String getLegalReference() {
        return legalReference;
    }
    public void setLegalReference(String legalReference) {
        this.legalReference = legalReference;
    }
}

