
How to efficiently introduce new codeLists in the MDR cache :

    a. Call the "Structure service" with the desired codelist acronym : mdr/rest/service/structure.
       This will send an OBJ_DESC request to FLUX TL, which will respond with the Code list description, meaning the fields that compose it!

    b. Create the new entity being careful to make sure it extends MasterDataRegistry (This is a must)!


    How to efficiently update your Liquibase scripts after adding the new entity :

    1. Create SQL Creation scripts (first you need to configure persistence.xml with the sql generation needed hibernate properties to generate sql files when running "mvn clean install").
	   Then you need to add each class entry in the persistence.xml under src->test->resources->META-INF folder. (See how it is already and add the new entries)
	   -->> To aid you with the new entries you can use : MdrCacheFactoryTest.testCacheInitAndPrintClassesForPersistenceXML() which will print in the console all the MDR Related entries.
	   It will print something like this in your console :

	   .......
	   <class>eu.europa.ec.fisheries.mdr.domain.codelists.FluxLocationCharacteristic</class>
	   <class>eu.europa.ec.fisheries.mdr.domain.codelists.FluxFaReportType</class>
	   .......

	2. Run clean install (profile: postgres dev) to generate the .ddl script files (they will contain the SQL scripts for the tables creation).
	   If this doesn't create the required files then use : HibernateExporterTest.exportTest() and it will cretae the creation script under "target/DDLscripts".

	2. Copy the content of create-tables.ddl/.sql and then Run it in "pgAdmin" - to the interested schema (ex. activity) - to generate the tables -
	   (PS: For postgres it will require some changes to the generated SQL code since it is not retro compatible with postgress)
	   Using Postgis dialect I had to perform this corrections to the generated ddl script:

			1. Change from int to BIGINT or int8;
			2. From float to REAL or float8;
			3. Put Colons (;) at the end of the rows;

	3. Now that the tables are created open CMDER and cd to the LIQUIBASE folder under the interested project (postgres must be up and running)
	  (Example for Activity module : "cd C:\Projects-ARHS\trunk\Modules\Activity\LIQUIBASE" )

	4. Run [[[  mvn liquibase:generateChangeLog -Ppostgres,exec ]]] command.
	   This will generate a 'generatedChangelog.xml' file under '..\LIQUIBASE\postgres\changelog'.
	   ATTENTION : If the changeLog file exist it will append to it so be aware that you need to empty the file before this procedure.
	   This file will contain the liquibase scripts for the generation of all the tables.

	5. For each table (section) contained in the 'generatedChangelog.xml' we have to create a separate xml file and store it under '\LIQUIBASE\postgres\schema\tables'.
	   The name of the file should have the name of the table and the extension ".xml".

	   -->> To aid with this there is a File Utility you can use : LiquibaseUtil.createXMLLiquibaseEntries();
	   ATTENTION : checks that the LiquibaseUtil setup is the one you need!! ChangeLog file location, outputDirectory ecc..

	6. Next we have to update 'db-changelog-createTables_0.1.xml' file - contained in '..\LIQUIBASE\postgres\changelog\v0.1' - with the new files entries (paths).
	   Meaning: for each file entry we created, a row pointing to that file will be added to 'db-changelog-createTables_0.1.xml' file.
	   -->> To aid you with creating the needed entries you can use MdrCacheFactoryTest.testEnlistFileEntriesInConsole();
	   It will print something like this in your console :
		.......
		<include file="\postgres\schema\tables\mdr_acronymversion.xml"/>
		<include file="\postgres\schema\tables\mdr_codelist_status.xml"/>
	    .......

	6.1 Now you are set up! You can test if everything went well by droping the tables from mdr db and recreating them through Liquibase.

	7. Under the LIQUIBASE folder you can find the "maven_commands.txt" file which contains all the maven commands that are needed to finish LIQUIBASE configurations :

		## To clean and install database locally on postgres you can run:
        mvn liquibase:update -Dverbose=true -Ppostgres,exec

        ## To clean and RE-install database locally on postgres :
        mvn clean liquibase:update -Ppostgres,exec

        ## To clean and RE-install database (locally on postgres) including the test data :
        mvn clean liquibase:update -Ppostgres,exec,testdata

        ## Check status :
        mvn clean liquibase:status -Ppostgres,exec

        ## Generate changelog from existing database :
        mvn liquibase:generateChangeLog -Ppostgres,exec

        ## Generate changelog with the exported data (exports them from the db) :
        mvn liquibase:generateChangeLog -Ppostgres,exec,export

        ## Generate SQL changes from changelog table :
        mvn liquibase:updateSQL -Ppostgres,exec

        ## Drop all tables (including other db objects like sequences etc..) :
        mvn liquibase:dropAll -Ppostgres,exec

	 ** All the commands are self-explanatoty as you can see.

    OPTIONAL :

    1. If you want to store test data so that you can use/restore them during development you need to call the "synchronize all" mdr service,
    so that you get data from FLUX MDM service.

    2. Once the data is in the mdr db you can run the "mvn liquibase:generateChangeLog -Ppostgres,exec,export" to export the data in the changeLog file (clear changelog before of this procedure).

    3. Copy the data from changeLog and put them in "testData.xml" file located under "...\LIQUIBASE\postgres\changelog\testdata".

    4. To test this you can drop mdr db and recreate everything with the "mvn clean liquibase:update -Ppostgres,exec,testdata" to include test data
       after creation of the tables.