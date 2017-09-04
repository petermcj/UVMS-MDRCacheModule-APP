/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.hibernateExporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Formatter;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.junit.Test;
import org.reflections.Reflections;

/*import static org.hibernate.spatial.dialect.postgis.*;*/

/**
 * Created by kovian on 01/09/2017.
 */
public class HibernateExporterTest {

    String createAcronymVersion = "create table mdr.mdr_acronymversion (id bigint not null, end_date timestamp, start_date timestamp, version_name varchar(255), status_ref_id bigint not null, primary key (id));\n";
    String createStatusTable = "create table mdr.mdr_codelist_status (id bigint not null, last_attempt timestamp, last_status varchar(255), last_success timestamp, object_acronym varchar(255), object_description varchar(255), object_name varchar(255), object_source varchar(255), schedulable varchar(1), end_date timestamp, start_date timestamp, primary key (id));\n";

    String statusSquence = "create sequence mdr.mdr_codelist_status_seq INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 200 CACHE 1;ALTER TABLE mdr.mdr_codelist_status_seq OWNER TO mdr;\n";

    String augmentSequenceStart = " INCREMENT 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 2000 CACHE 1; ALTER TABLE ";
    String augmentSequenceEnd = " OWNER TO mdr";
    String addVersionToStatus = "alter table mdr.mdr_acronymversion add constraint FK_322iyjopmjhfv4uw0jpcfguug foreign key (status_ref_id) references mdr.mdr_codelist_status;\n";

    String sqlScriptGenerationDir = "target/DDLscripts";
    PrintWriter sqlFileWritter;

    @Test
    public void exportTest() {
        prepareFilesAndDirs();
        HibernateExporter exporter = new HibernateExporter("org.hibernate.spatial.dialect.postgis.PostgisDialect", "eu.europa.ec.fisheries.mdr.entities.codelists");
        System.out.println("\n\n Generated Script : \n\n");
        exporter.exportToConsole();
        System.out.println("Done...");
    }



    private void prepareFilesAndDirs() {
        String filePath = new File(sqlScriptGenerationDir).getAbsolutePath();
        createDirectoryIfDoesNotExist(filePath);
        try {
            sqlFileWritter = new PrintWriter("target/DDLscripts/createTables.sql", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void createDirectoryIfDoesNotExist(String dirPath) {
        File theDir = new File(dirPath);
        boolean exists = false;
        try {
            exists = theDir.mkdirs();
        } catch (SecurityException sex) {
            System.out.println("JAVA Security check does not permit access to create Directory : " + dirPath + sex);
        }
        if (exists) {
            System.out.println("Directory : " + dirPath + " didn't exist! Created it..");
        }

    }

    private class HibernateExporter {

        private String dialect;
        private String entityPackage;

        private boolean generateCreateQueries = true;
        private boolean generateDropQueries = false;

        private Configuration hibernateConfiguration;

        public HibernateExporter(String dialect, String entityPackage) {

            this.dialect = dialect;
            this.entityPackage = entityPackage;

            hibernateConfiguration = createHibernateConfig();
        }

        public void export(File exportFile) throws FileNotFoundException {
            export(new FileOutputStream(exportFile), generateCreateQueries, generateDropQueries);
        }

        public void exportToConsole() {
            export(System.out, generateCreateQueries, generateDropQueries);
        }

        public void export(OutputStream out, boolean generateCreateQueries, boolean generateDropQueries) {
            Dialect hibDialect = Dialect.getDialect(hibernateConfiguration.getProperties());
            try {
                PrintWriter writer = new PrintWriter(out);
                if (generateCreateQueries) {
                    String[] createSQL = hibernateConfiguration.generateSchemaCreationScript(hibDialect);
                    write(writer, createSQL, new Formatter());
                }
                if (generateDropQueries) {
                    String[] dropSQL = hibernateConfiguration.generateDropSchemaScript(hibDialect);
                    write(writer, dropSQL, new Formatter());
                }
            } catch (Exception ex) {
                System.out.println("Exception occurred.." + ex);
                ex.printStackTrace();
            }
        }

        private void write(PrintWriter writer, String[] lines, Formatter formatter) {
            sqlFileWritter.write(createAcronymVersion);
            sqlFileWritter.write(createStatusTable);
            for (String creationScript : lines) {
                String finalCreationScript;
                if (creationScript.contains("_seq")) {
                    int seqInd = creationScript.lastIndexOf("sequence") + 9;
                    String tableName = creationScript.substring(seqInd, creationScript.length());
                    finalCreationScript = creationScript + augmentSequenceStart + tableName + augmentSequenceEnd;
                } else {
                    finalCreationScript = creationScript;
                }
                String finalScript = finalCreationScript.replaceAll("mdr_", "mdr.mdr_");
                System.out.println(finalScript + ";");
                sqlFileWritter.write(finalScript + ";\n");
            }
            sqlFileWritter.write(addVersionToStatus);
            sqlFileWritter.write(statusSquence);
            sqlFileWritter.close();

            //writer.println(formatter.format(string) + ";");
        }

        private Configuration createHibernateConfig() {

            hibernateConfiguration = new Configuration();

            final Reflections reflections = new Reflections(entityPackage);
            for (Class<?> cl : reflections.getTypesAnnotatedWith(MappedSuperclass.class)) {
                hibernateConfiguration.addAnnotatedClass(cl);
                System.out.println("Mapped = " + cl.getName());
            }
            for (Class<?> cl : reflections.getTypesAnnotatedWith(Entity.class)) {
                hibernateConfiguration.addAnnotatedClass(cl);
                System.out.println("Mapped = " + cl.getName());
            }
            hibernateConfiguration.setProperty(AvailableSettings.DIALECT, dialect);
            return hibernateConfiguration;
        }

        public boolean isGenerateDropQueries() {
            return generateDropQueries;
        }

        public void setGenerateDropQueries(boolean generateDropQueries) {
            this.generateDropQueries = generateDropQueries;
        }

        public Configuration getHibernateConfiguration() {
            return hibernateConfiguration;
        }

        public void setHibernateConfiguration(Configuration hibernateConfiguration) {
            this.hibernateConfiguration = hibernateConfiguration;
        }

        public void writeToFile(String filename, int[] x) throws IOException {
            BufferedWriter outputWriter;
            outputWriter = new BufferedWriter(new FileWriter(filename));
            for (int i = 0; i < x.length; i++) {
                outputWriter.write(x[i] + ";");
                outputWriter.newLine();
            }
            outputWriter.flush();
            outputWriter.close();
        }
    }
}
