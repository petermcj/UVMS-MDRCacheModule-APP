/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.entities.codelists.baseentities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.annotations.Field;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;

@SuppressWarnings("serial")
@Embeddable
public class RectangleCoordinates implements Serializable {

    @Column(name = "south")
    @Field(name="south")
    private String south;

    @Column(name = "west")
    @Field(name="west")
    private String west;

    @Column(name = "north")
    @Field(name="north")
    private String north;

    @Column(name = "east")
    @Field(name="east")
    private String east;

    public RectangleCoordinates() {
        super();
    }

    public RectangleCoordinates(MDRDataNodeType mdrDataType) {
        List<MDRElementDataNodeType> fieldsToRemove  = new ArrayList<>();
        List<MDRElementDataNodeType> subordinateMDRElementDataNodes = mdrDataType.getSubordinateMDRElementDataNodes();
        for(MDRElementDataNodeType field : subordinateMDRElementDataNodes){
            String fieldName  = field.getName().getValue();
            String fieldValue = field.getValue().getValue();
            if(StringUtils.endsWith(fieldName, ".WEST")){
                this.setWest(fieldValue);
                fieldsToRemove.add(field);
            } else if(StringUtils.endsWith(fieldName, ".EAST")){
                this.setEast(fieldValue);
                fieldsToRemove.add(field);
            } else if(StringUtils.endsWith(fieldName, ".NORTH")){
                this.setNorth(fieldValue);
                fieldsToRemove.add(field);
            } else if(StringUtils.endsWith(fieldName, ".SOUTH")){
                this.setSouth(fieldValue);
                fieldsToRemove.add(field);
            }
        }
        subordinateMDRElementDataNodes.removeAll(fieldsToRemove);
    }

    public String getSouth() {
        return south;
    }
    public void setSouth(String south) {
        this.south = south;
    }
    public String getWest() {
        return west;
    }
    public void setWest(String west) {
        this.west = west;
    }
    public String getNorth() {
        return north;
    }
    public void setNorth(String north) {
        this.north = north;
    }
    public String getEast() {
        return east;
    }
    public void setEast(String east) {
        this.east = east;
    }

}