/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.dao;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;

import com.ninja_squad.dbsetup.operation.Operation;
import eu.europa.ec.fisheries.uvms.BaseDAOTest;

public abstract class BaseMdrDaoTest extends BaseDAOTest {

    // Delete tables statements.
    protected static final Operation DELETE_ALL_MDR_CR_NAFO_STOCK = sequenceOf(
            deleteAllFrom("mdr.mdr_cr_nafo_stock")            
    );
    protected static final Operation DELETE_ALL_MDR_SPECIES = sequenceOf(
            deleteAllFrom("mdr.mdr_fao_species")
    );
    protected static final Operation DELETE_ALL_FAO_SPECIES = sequenceOf(
            deleteAllFrom("mdr.mdr_fao_species")
    );
    protected static final Operation DELETE_ALL_MDR_CODELISTSTATUS = sequenceOf(
            deleteAllFrom("mdr.mdr_codelist_status")
    );

    protected static final Operation DELETE_ALL_CONFIGURATIONS = sequenceOf(
            deleteAllFrom("mdr.mdr_configuration")
    );

    protected static final Operation INSERT_MDR_CR_NAFO_STOCK_REFERENCE_DATA = sequenceOf(
            insertInto("mdr.mdr_cr_nafo_stock")
                    .columns("id", "created_on", "species_code", "species_name", "area_code", "area_description")
                    .values(1L, java.sql.Date.valueOf("2014-12-12"), "ANG", "Lophius americanus", "N3NO", "NAFO 3N, 3O = FAO 21.3.N + 21.3.O")
                    .build()
    );

    protected static final Operation INSERT_MDR_FAO_SPECIES = sequenceOf(
            insertInto("mdr.mdr_fao_species")
                    .columns("id", "start_date", "code", "description")
                    .values(1L, java.sql.Date.valueOf("2014-12-12"),  "C", "Creation")
                    .build()
    );

    protected static final Operation INSERT_MDR_CODELIST_STATUS = sequenceOf(
            insertInto("mdr.mdr_codelist_status")
                    .columns("id", "object_acronym", "schedulable").values(100L, "ACTION_TYPE", "Y").build(),
            insertInto("mdr.mdr_codelist_status")
                    .columns("id", "object_acronym", "schedulable").values(200L, "CONVERSION_FACTOR", "Y").build(),
            insertInto("mdr.mdr_codelist_status")
                    .columns("id", "object_acronym", "schedulable").values(300L, "GEAR_TYPE", "N").build()
    );

    protected static final Operation INSERT_ACRONYMS_VERSIONS = sequenceOf(
            insertInto("mdr.mdr_acronymversion")
                    .columns("id", "status_ref_id", "version_name", "start_date", "end_date").values(100L, 100L, "VERS_1", "2015-10-10 16:02:59.047", "2016-12-10 16:02:59.047").build(),
            insertInto("mdr.mdr_acronymversion")
                    .columns("id", "status_ref_id",  "version_name", "start_date", "end_date").values(200L, 100L, "VERS_2", "2015-10-10 16:02:59.047", "2016-12-10 16:02:59.047").build(),
            insertInto("mdr.mdr_acronymversion")
                    .columns("id", "status_ref_id",  "version_name", "start_date", "end_date").values(300L, 300L, "VERS_2", "2015-10-10 16:02:59.047", "2016-12-10 16:02:59.047").build()
    );

    protected static final Operation INSERT_ALL_CONFIGURATIONS = sequenceOf(insertInto("mdr.mdr_configuration")
                    .columns("id", "config_name", "config_value").values(1L, "SOME_CONFIG_1","SOME_VALUE").build(),
            insertInto("mdr.mdr_configuration")
                    .columns("id", "config_name", "config_value").values(2L, "SOME_CONFIG_2","SOME_VALUE").build(),
            insertInto("mdr.mdr_configuration")
                    .columns("id", "config_name", "config_value").values(3L, "SOME_CONFIG_3","SOME_VALUE").build(),
            insertInto("mdr.mdr_configuration")
                    .columns("id", "config_name", "config_value").values(4L, "MDR_SCHED_CONFIG_NAME","SOME_VALUE").build()


    );
   
    protected String getSchema() {
        return "mdr";
    }
    protected String getPersistenceUnitName() {
        return "testPU";
    }
}