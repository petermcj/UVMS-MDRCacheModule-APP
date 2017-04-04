package eu.europa.ec.fisheries.mdr.entities.codelists;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.exception.FieldNotMappedException;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import un.unece.uncefact.data.standard.mdr.response.MDRDataNodeType;
import un.unece.uncefact.data.standard.mdr.response.MDRElementDataNodeType;

import javax.persistence.*;

/**
 * Created by kovian on 11/23/2016.
 */
@Entity
@Table(name = "mdr_fao_species")
@Indexed
@Analyzer(impl = StandardAnalyzer.class)
public class FaoSpecies extends MasterDataRegistry {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @SequenceGenerator(name = "SEQ_GEN", sequenceName = "mdr_fao_species_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GEN")
    private long id;

    @Column(name = "is_group")
    @Field(name="is_group")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String isGroup;

    @Column(name = "scientific_name")
    @Field(name="scientific_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String scientificName;

    @Column(name = "en_name")
    @Field(name="en_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String enName;

    @Column(name = "fr_name")
    @Field(name="fr_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String frName;

    @Column(name = "es_name")
    @Field(name="es_name")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String esName;

    @Column(name = "family")
    @Field(name="family")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String family;

    @Column(name = "bio_order")
    @Field(name="bio_order")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String bioOrder;

    @Column(name = "taxo_code")
    @Field(name="taxo_code")
    @Analyzer(definition = LOW_CASE_ANALYSER)
    private String taxoCode;

    @Override
    public void populate(MDRDataNodeType mdrDataType) throws FieldNotMappedException {
        populateCommonFields(mdrDataType);
        for(MDRElementDataNodeType field : mdrDataType.getSubordinateMDRElementDataNodes()){
            String fieldName  = field.getName().getValue();
            String fieldValue  = field.getName().getValue();
            if(StringUtils.contains(fieldName, "ISGROUP")){
                this.setIsGroup(fieldValue);
            } else if(StringUtils.contains(fieldName, "SCIENTNAME")){
                this.setScientificName(fieldValue);
            } else if(StringUtils.contains(fieldName, "ENNAME")){
                this.setEnName(fieldValue);
            } else if(StringUtils.contains(fieldName, "FRNAME")){
                this.setFrName(fieldValue);
            } else if(StringUtils.contains(fieldName, "ESNAME")){
                this.setEsName(fieldValue);
            } else if(StringUtils.contains(fieldName, "FAMILY")){
                this.setFamily(fieldValue);
            } else if(StringUtils.contains(fieldName, "BIOORDER")){
                this.setBioOrder(fieldValue);
            } else if(StringUtils.contains(fieldName, "TAXOCODE")){
                this.setEnName(fieldValue);
            } else {
                throw new FieldNotMappedException(this.getClass().getSimpleName(), fieldName);
            }
        }
    }

    @Override
    public String getAcronym() {
        return "FAO_SPECIES";
    }

    public String getIsGroup() {
        return isGroup;
    }
    public void setIsGroup(String isGroup) {
        this.isGroup = isGroup;
    }
    public String getScientificName() {
        return scientificName;
    }
    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
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
    public String getBioOrder() {
        return bioOrder;
    }
    public void setBioOrder(String bioOrder) {
        this.bioOrder = bioOrder;
    }
    public String getTaxoCode() {
        return taxoCode;
    }
    public void setTaxoCode(String taxoCode) {
        this.taxoCode = taxoCode;
    }
}
