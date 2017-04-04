/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.entities.codelists;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.*;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;

import javax.persistence.*;

/**
 * Created by kovian on 11/22/2016.
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "mdr_location")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class Location extends MasterDataRegistry {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_location_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "code_2")
    @Field(name = "code_2")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String code2;

    @Column(name = "en_name")
    @Field(name = "en_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String enName;

    @Column(name = "latitude")
    @Field(name = "latitude")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private Double latitude;

    @Column(name = "longitude")
    @Field(name = "longitude")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private Double longitude;

    @Column(name = "fishing_port_ind")
    @Field(name = "fishing_port_ind")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private Boolean fishingPortInd;

    @Column(name = "landing_place_ind")
    @Field(name = "landing_place_ind")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private Boolean landingPlaceInd;

    @Column(name = "commercial_port_ind")
    @Field(name = "commercial_port_ind")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private Boolean commercialPortInd;

    @Column(name = "unlo_code")
    @Field(name = "unlo_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String unloCode;

    @Column(name = "un_function_code")
    @Field(name = "un_function_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String unFunctionCode;

    @Column(name = "coordinates")
    @Field(name = "coordinates")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String coordinates;

    @Override
    public String getAcronym() {
        return "LOCATION";
    }

    @Override
    public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
        populateCommonFields(mdrDataType);
        for (MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()) {
            String fieldName = field.getName().getValue();
            String fieldValue = field.getName().getValue();
            if (StringUtils.contains(fieldName, "CODE2")) {
                this.setCode2(fieldValue);
            } else if (StringUtils.contains(fieldName, "LATITUDE")) {
                this.setLatitude(Double.valueOf(fieldValue));
            } else if (StringUtils.contains(fieldName, "LONGITUDE")) {
                this.setLongitude(Double.valueOf(fieldValue));
            } else if (StringUtils.contains(fieldName, "FISHINGPORTIND")) {
                this.setFishingPortInd(Boolean.valueOf(fieldValue));
            } else if (StringUtils.contains(fieldName, "LANDINGPLACEIND")) {
                this.setLandingPlaceInd(Boolean.valueOf(fieldValue));
            } else if (StringUtils.contains(fieldName, "COMMERCIALPORTIND")) {
                this.setCommercialPortInd(Boolean.valueOf(fieldValue));
            } else if(StringUtils.contains(fieldName, "ENNAME")){
                this.setEnName(fieldValue);
            }  else if (StringUtils.contains(fieldName, "LOCODE")) {
                this.setUnloCode(fieldValue);
            } else if (StringUtils.contains(fieldName, "COORDINATES")) {
                this.setCoordinates(fieldValue);
            } else if (StringUtils.contains(fieldName, "UNFCTCODE")) {
                this.setUnFunctionCode(fieldValue);
            } else {
                throw new FieldNotMappedException(this.getClass().getSimpleName(), fieldName);
            }
        }
    }

    public String getCode2() {
        return code2;
    }
    public void setCode2(String iso3CountryCode) {
        this.code2 = iso3CountryCode;
    }
    public Double getLatitude() {
        return latitude;
    }
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    public Double getLongitude() {
        return longitude;
    }
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    public Boolean getFishingPortInd() {
        return fishingPortInd;
    }
    public void setFishingPortInd(Boolean fishingPortInd) {
        this.fishingPortInd = fishingPortInd;
    }
    public Boolean getLandingPlaceInd() {
        return landingPlaceInd;
    }
    public void setLandingPlaceInd(Boolean landingPlaceInd) {
        this.landingPlaceInd = landingPlaceInd;
    }
    public Boolean getCommercialPortInd() {
        return commercialPortInd;
    }
    public void setCommercialPortInd(Boolean commercialPortInd) {
        this.commercialPortInd = commercialPortInd;
    }
    public String getUnloCode() {
        return unloCode;
    }
    public void setUnloCode(String unloCode) {
        this.unloCode = unloCode;
    }
    public String getCoordinates() {
        return coordinates;
    }
    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }
    public String getUnFunctionCode() {
        return unFunctionCode;
    }
    public void setUnFunctionCode(String unFunctionCode) {
        this.unFunctionCode = unFunctionCode;
    }
    public String getEnName() {
        return enName;
    }
    public void setEnName(String enName) {
        this.enName = enName;
    }

}