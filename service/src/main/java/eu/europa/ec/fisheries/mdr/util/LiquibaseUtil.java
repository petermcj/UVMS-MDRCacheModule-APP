/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.util;

import com.google.common.base.Charsets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.lang.StringUtils;

/**
 * Created by kovian on 24/11/2016.
 *
 * This class is a utility class which, after having created the generatedChangelog.xml LIQUIBASE file it can read
 * it and generate separate xml files for each table entry of generatedChangelog.xml.
 *
 * Each time you want to use this class remember to :
 *
 *  1. changeLogFilePath = Where is your generatedChangelog.xml located;
 *  2. locationForGeneratedXmls = Where will the new xmls (single table entries) will be created;
 *
 */
public class LiquibaseUtil {

    private static final Logger log = Logger.getLogger("LiquibaseUtilLogger");

    // This Class isn't supposed to have instances.
    private LiquibaseUtil(){
        super();
    }

    static {
        log.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        log.addHandler(handler);
    }

    static String splitter                 = "<changeSet ";
    static String changeLogFilePath        = "C:\\GIT Repository\\Mdr Github\\UVMS-MDRCacheModule-DB\\LIQUIBASE\\postgres\\changelog\\generatedChangelog.xml";
    static String locationForGeneratedXmls = "C:\\newLiquibaseScripts\\";
    static String filePrefix               = StringUtils.EMPTY;
    static String fileSuffix               = ".xml";
    private static int sequence            = 0;

    public static void createXMLLiquibaseEntries() throws IOException {

        String filecontentStr = readFile(changeLogFilePath, Charsets.UTF_8);

        List<String> filesContents = Arrays.asList(filecontentStr.split(splitter));
        filesContents = fixFileContents(filesContents, splitter);
        log.log(Level.ALL, "\n\n I Found : " + filesContents.size() + " Change Sets in location : " + changeLogFilePath);

        for (String content : filesContents) {
            content = "\t"+content;
            String fileName = getFileName(/*filePrefix, fileSuffix,*/ content/*, locationForGeneratedXmls*/);
            if(StringUtils.isEmpty(fileName)){
                continue;
            }
            String filePath = new StringBuilder(locationForGeneratedXmls).append(filePrefix).append(fileName).append(fileSuffix).toString();
            String header   = createHeaderAndSequenceSections(fileName);
            String footer   = createAddPrimaryKeyAndFooter(fileName);
            String finalContent = addAllTogether(content, header, footer);
            sequence++;
            createNewFile(filePath, finalContent);
        }

        log.log(Level.ALL, "\n\n-->>>> All the work for file creation ended successfully. \n-->>>> You can get your files at : "+locationForGeneratedXmls);
    }

    private static String createAddPrimaryKeyAndFooter(String fileName) {
        return "\n\t<changeSet author=\"kovian (generated)\" id=\"1490280409454-"+sequence+"\" objectQuotingStrategy=\"QUOTE_ALL_OBJECTS\">\n" +
                "        <addPrimaryKey columnNames=\"id\" constraintName=\""+fileName+"_pkey\" tableName=\""+fileName+"\"/>\n" +
                "    </changeSet>" + "\n\t\n" +
                "\t<changeSet author=\"kovian\" id=\"76817789687171-"+sequence+"\" dbms=\"postgresql\">\n" +
                "\t\t<addDefaultValue \n" +
                "\t\t\t\tcolumnDataType=\"BIGINT\"\n" +
                "\t\t\t\tcolumnName=\"id\"\n" +
                "\t\t\t\tdefaultValueSequenceNext=\""+fileName+"_seq\"\n" +
                "\t\t\t\ttableName=\""+fileName+"\"/>\n" +
                "\t</changeSet>\t\n" +
                "\t\n" +
                "</databaseChangeLog>";
    }

    private static String createHeaderAndSequenceSections(String fileName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<databaseChangeLog xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\" \n" +
                "\t\t\t\t   xmlns:ext=\"http://www.liquibase.org/xml/ns/dbchangelog-ext\" \n" +
                "\t\t\t\t   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "\t\t\t\t   xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog " +
                "http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.3.xsd http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd\">\n" +
                "       \n" +
                "    <changeSet author=\"kovian\" id=\"1890672105152481"+sequence+"\">\n" +
                "\t  <createSequence cycle=\"false\" incrementBy=\"1\" maxValue=\"9223372036854775807\" minValue=\"1\"\n" +
                "\t\t\t\t\t  sequenceName=\""+fileName+"_seq\" startValue=\"2000\"/>\n" +
                "    </changeSet>  \n\n";
    }

    private static String addAllTogether(String content, String header, String footer) {
        return new StringBuilder(header).append(content).append(footer).toString();
    }

    private static List<String> fixFileContents(List<String> filesContents, String splitter) {
        List<String> fixedContents = new ArrayList<>();
        for (String content : filesContents) {
            if (content.contains("tableName=")) {
                String contentR = splitter + content;
                fixedContents.add(contentR);
            }
        }
        return fixedContents;
    }

    private static String getFileName(/*String filePrefix, String fileSuffix, */String content /*, String newLocation*/) {
        String cutStr;
        try {
            if(content.contains("addPrimaryKey")){
                return StringUtils.EMPTY;
            }
            if(StringUtils.isNotEmpty(content) && content.contains("tableName")){
                cutStr = content.substring(content.indexOf("tableName=\""), content.indexOf("tableName=\"") + 100);
                return cutStr.substring(cutStr.indexOf("\"") + 1, cutStr.indexOf("\"", cutStr.indexOf("\"") + 1));
            } else {
                return StringUtils.EMPTY;
            }
        } catch(Exception ex){
            System.out.println("The following script threw : " + content);
            System.out.println(ex);
        }
        return StringUtils.EMPTY;
    }


    private static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    private static void createNewFile(String filePath, String content) {
        log.log(Level.ALL, "\n Creating file : " + filePath+". Sequence : "+sequence);
        File fileEntry = new File(filePath);
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileEntry), Charsets.UTF_8))) {
            writer.write(content);
        } catch (UnsupportedEncodingException e) {
            log.log(Level.ALL, "UnsupportedEncodingException : ", e);
        } catch (FileNotFoundException e) {
            log.log(Level.ALL, "FileNotFoundException : ", e);
        } catch (IOException e) {
            log.log(Level.ALL, "IOException : ", e);
        }
    }

    public static void setSequence(int sequence1) {
        sequence = sequence1;
    }

}
