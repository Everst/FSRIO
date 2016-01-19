package com.jbt;
/* fix comment; multiple PI info; and normalization
 * Plus get filtering by food safety somehow
 */
import java.io.*;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.text.WordUtils;


public class NIH {
	
	HashMap<String, String> abstracts = new HashMap<String, String>();
	HashMap<String, String> indexMap = new HashMap<String, String>();
	
	public static String main(String inputfolder, String inputfolder_abstracts, String outfolder, String host, String user, String passwd, String dbname) throws IOException {
		
		NIH obj  =  new NIH();
		
		obj.abstracts(inputfolder_abstracts);
		obj.projects(outfolder,inputfolder,host,user,passwd,dbname);
		
		return "NIH";
	
	}

	public File[] getFiles(String dir) {
		File[] files = new File(dir).listFiles();

		return files;
	}

	public void abstracts(String inputfolder_abstracts) throws IOException {
		File[] files = getFiles(inputfolder_abstracts);
		for (File file : files) {

			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(new FileReader(file));
			try {	
			for (CSVRecord record : records) {
					try {
					
					abstracts.put(record.get("APPLICATION_ID"), record.get("ABSTRACT_TEXT"));
					}
					catch (Exception e) {
						
					}
				}
			} catch (Exception ee) {
				
			}
			
		}
	}

	public void projects(String outfolder, String inputfolder, String host, String user, String passwd, String dbname) throws IOException {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"NIH_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));
		
		String[] header = {"project__PROJECT_NUMBER","project__AGENCY_FULL_NAME", "project__PROJECT_ABSTRACT", "countries__COUNTRY_NAME", "project__PROJECT_TITLE",
				"project__PROJECT_START_DATE", "project__PROJECT_END_DATE", "project__PROJECT_FUNDING", "project__PROJECT_TYPE",
				"project__LAST_UPDATE", "project__ACTIVITY_STATUS", "project__DATE_ENTERED", "investigator_data__name",
				"project__PROJECT_NUMBER", "institution_data__INSTITUTION_NAME", "institution_data__city_name",
				"states__states_abbrv", "comment",  "insitution_data__ID", "institution_data__INSTITUTION_STATE", 
				"investigator_data__ID", "agency_data__ID"};
		csvout.printRecord(header);

		File[] files = getFiles(inputfolder);
		for (File file : files) {
			if (!file.getName().contains(".csv")) {
				continue;
			}
			Iterable<CSVRecord> records = CSVFormat.EXCEL.withHeader().parse(new FileReader(file));
			for (CSVRecord record : records) {
				String project__AGENCY_FULL_NAME = "";
				String project__PROJECT_ABSTRACT= "";
				String countries__COUNTRY_NAME = "";
				String project__PROJECT_TITLE = "";
				String project__PROJECT_START_DATE = "";
				String project__PROJECT_END_DATE = "";
				String project__PROJECT_FUNDING = "";
				String project__PROJECT_TYPE = "3";
				String project__LAST_UPDATE = "";
				String project__ACTIVITY_STATUS = "0";
				String project__DATE_ENTERED = "";
				String investigator_data__name = "";
				String project__PROJECT_NUMBER = "";
				String institution_data__INSTITUTION_NAME = "";
				String institution_data__city_name = "";
				String states__states_abbrv = "";
				String comment = "";
				int insitution_data__ID = -1; 
				int insitution_data__INSTITUION_STATE = -1; 
				int investigator_data__ID = -1;
				int agency_data__ID = -1;
				
				// Project information
				project__AGENCY_FULL_NAME = record.get("IC_NAME");
				project__PROJECT_ABSTRACT = abstracts.get(record.get("APPLICATION_ID"));
				project__PROJECT_TITLE = WordUtils.capitalizeFully(record.get("PROJECT_TITLE"),' ','-');
				project__PROJECT_FUNDING = record.get("TOTAL_COST");
				project__PROJECT_START_DATE = record.get("PROJECT_START");
				project__PROJECT_END_DATE = record.get("PROJECT_END");
				project__PROJECT_NUMBER = record.get("FULL_PROJECT_NUM");
				Date dateNow = new Date();
				long days = 0;
				try {
					Date dateProj = new Date(project__PROJECT_END_DATE);

					long diff = dateNow.getTime() - dateProj.getTime();
					days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
				}
				catch (Exception e) {;}
				
				if (days >= 0)
					project__ACTIVITY_STATUS = "1";

				else
					project__ACTIVITY_STATUS = "4";
				project__LAST_UPDATE = dateFormat.format(current);
				DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
				project__DATE_ENTERED = dateFormatEnter.format(current);
				try {
					project__PROJECT_START_DATE = project__PROJECT_START_DATE.substring(6);
					project__PROJECT_END_DATE = project__PROJECT_END_DATE.substring(6);	
				}
				catch (Exception e) {;}
				

				// Get Investigator information
				investigator_data__name = record.get("PI_NAMEs").replace(";", "");
				investigator_data__name = WordUtils.capitalizeFully(investigator_data__name,' ','-');


				// Get Institution information

				institution_data__INSTITUTION_NAME = record.get("ORG_NAME");
				institution_data__city_name = record.get("ORG_CITY");
				states__states_abbrv = record.get("ORG_STATE");
				countries__COUNTRY_NAME = record.get("ORG_COUNTRY");


				// See if the institution ID exists, if not make it -1 to reflect we need to add.
				String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
				ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
				try {
					rs.next();
					insitution_data__ID = Integer.parseInt(rs.getString(1));
				}
				catch (Exception e) {
					comment = "Please populate institution fields by exploring the institution named on the project.";
				}


				// See if the country ID exists, if not make it -1 to reflect we need to add.
				int insitution_data__INSTITUION_COUNTRY = -1; 
				String GetcountryIDsql = "SELECT ID FROM " + dbname + ".countries WHERE COUNTRY_NAME = \"" +  countries__COUNTRY_NAME + "\";";
				ResultSet rs2 = MysqlConnect.sqlQuery(GetcountryIDsql,host,user,passwd);
				try {
					rs2.next();
					insitution_data__INSTITUION_COUNTRY = Integer.parseInt(rs.getString(1));
				}
				catch (Exception e) {
					;
				}


				// See if the state ID exists, if not make it -1 to reflect we need to add.
				String GetstateIDsql = "SELECT ID FROM " + dbname + ".states WHERE abbrv = \"" +  states__states_abbrv + "\";";
				ResultSet rs3 = MysqlConnect.sqlQuery(GetstateIDsql,host,user,passwd);
				try {
					rs3.next();
					insitution_data__INSTITUION_STATE = Integer.parseInt(rs3.getString(1));
				}
				catch (Exception e) {
					;
				}

				// Let us see if we can find the investigator in the already existing data. 
				// Condition: investigator must belong to the same institution that we just parsed.
				// We first use email, then name.

				String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + insitution_data__ID + "\";";
				ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
				try {
					rs6.next();
					investigator_data__ID = Integer.parseInt(rs6.getString(1));
				}
				catch (Exception e) {
					;
				}


				String GetAgencySQL = "SELECT ID FROM " + dbname + ".agency_data WHERE AGENCY_FULL_NAME LIKE \"" +  project__AGENCY_FULL_NAME + "\";";
				ResultSet rs7 = MysqlConnect.sqlQuery(GetAgencySQL,host,user,passwd);
				try {
					rs7.next();
					agency_data__ID = Integer.parseInt(rs7.getString(1));
				}
				catch (Exception e) {
					;
				}
				String[] output = {project__PROJECT_NUMBER, project__AGENCY_FULL_NAME, project__PROJECT_ABSTRACT, countries__COUNTRY_NAME, project__PROJECT_TITLE,
						project__PROJECT_START_DATE, project__PROJECT_END_DATE, project__PROJECT_FUNDING, project__PROJECT_TYPE,
						project__LAST_UPDATE, project__ACTIVITY_STATUS, project__DATE_ENTERED, investigator_data__name,
						project__PROJECT_NUMBER, institution_data__INSTITUTION_NAME, institution_data__city_name,
						states__states_abbrv, comment,  String.valueOf(insitution_data__ID), String.valueOf(insitution_data__INSTITUION_STATE), 
						String.valueOf(investigator_data__ID), String.valueOf(agency_data__ID)};

				csvout.printRecord(output);
			}
			csvout.close();
		}
	}
}
