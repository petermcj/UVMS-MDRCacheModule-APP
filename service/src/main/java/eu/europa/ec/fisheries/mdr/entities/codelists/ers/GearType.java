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
 * Created by kovian on 11/23/2016.
 */
@Entity
@Table(name = "mdr_gear_type")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class GearType extends MasterDataRegistry {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_gear_type_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
	private long id;

	@Column(name = "group_name")
	@Field(name="group_name")
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private String category;
	
	@Column(name = "sub_group_name")
	@Field(name="sub_group_name")
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private String subCategory;

	@Column(name = "iss_cfg_code")
	@Field(name="iss_cfg_code")
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private String issCfgCode;

	@Column(name = "iccat_code")
	@Field(name="iccat_code")
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private String iccatCode;

	@Column(name = "target")
	@Field(name="target")
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private String target;

	@Override
	public String getAcronym() {
		return "GEAR_TYPE";
	}


	@Override
	public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
		populateCommonFields(mdrDataType);
		for(MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()){
			String fieldName  = field.getName().getValue();
			String fieldValue  = field.getValue().getValue();
			if(StringUtils.equalsIgnoreCase(fieldName, "GEAR_TYPE.CATEGORY")){
				this.setCategory(fieldValue);
			} else if(StringUtils.equalsIgnoreCase(fieldName, "GEAR_TYPE.SUBCATEGORY")){
				this.setSubCategory(fieldValue);
			} else if(StringUtils.equalsIgnoreCase(fieldName, "GEAR_TYPE.ICCATCODE")){
				this.setIccatCode(fieldValue);
			} else if(StringUtils.equalsIgnoreCase(fieldName, "GEAR_TYPE.ISSCFGCODE")){
				this.setIssCfgCode(fieldValue);
			} else if(StringUtils.equalsIgnoreCase(fieldName, "GEAR_TYPE.TARGET")){
				this.setTarget(fieldValue);
			} else {
				logError(fieldName, this.getClass().getSimpleName());
			}
		}
	}


	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getSubCategory() {
		return subCategory;
	}
	public void setSubCategory(String subCategory) {
		this.subCategory = subCategory;
	}
	public String getIssCfgCode() {
		return issCfgCode;
	}
	public void setIssCfgCode(String issCfgCode) {
		this.issCfgCode = issCfgCode;
	}
	public String getIccatCode() {
		return iccatCode;
	}
	public void setIccatCode(String iccatCode) {
		this.iccatCode = iccatCode;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
}