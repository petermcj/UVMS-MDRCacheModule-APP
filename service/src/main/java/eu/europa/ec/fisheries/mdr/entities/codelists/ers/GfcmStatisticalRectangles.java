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
import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.RectangleCoordinates;
import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;

@SuppressWarnings("serial")
@Entity
@Table(name = "mdr_gfcm_statistical_rectangles")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class GfcmStatisticalRectangles extends MasterDataRegistry {

	@Id
	@Column(name = "id", unique = true, nullable = false)
	@SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_gfcm_statistical_rectangles_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
	private long id;

	@Embedded
	@IndexedEmbedded
	@Analyzer(definition = LOW_CASE_ANALYSER)
	private RectangleCoordinates rectangle;

	@Override
	public String getAcronym() {
		return "GFCM_STAT_RECTANGLE";
	}

	@Override
	public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
		populateCommonFields(mdrDataType);
		rectangle = new RectangleCoordinates(mdrDataType);
	}

	public RectangleCoordinates getRectangle() {
		return rectangle;
	}
	public void setRectangle(RectangleCoordinates rectangle) {
		this.rectangle = rectangle;
	}

}