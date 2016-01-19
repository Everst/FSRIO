package com.jbt;
/* Ahmad is fixing
 * 
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.w3c.dom.*;

import javax.xml.parsers.*;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class NSF {
	
	public static String main(String inputfolder, String outfolder, String host, String user, String passwd, String dbname) throws IOException,SAXException,ParserConfigurationException {
		
		scrape(outfolder,inputfolder,host,user,passwd,dbname);
		return "NSF";

	}
	
	public static File[] getFiles(String dir) {
		File[] files = new File(dir).listFiles();
		return files;
	}

	public static void scrape(String outfolder, String inputfolder, String host, String user, String passwd, String dbname) throws IOException,SAXException,ParserConfigurationException {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"NSF_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER","project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_ABSTRACT","project__LAST_UPDATE",
				"project__DATE_ENTERED", "project__PROJECT_FUNDING",
				"institution_data__INSTITUTION_NAME","institution_data__INSTITUTION_COUNTRY",
				"institution_index__inst_id",
				"agency_index__aid","comment"};
		csvout.printRecord(header);

		File[] files = getFiles(inputfolder);
		for (File file : files) {
			if (file.getName().endsWith("xml")) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(file);
				//doc.getDocumentElement().normalize();
	
				// Get Award Information
				Element award = (Element) doc.getElementsByTagName("Award").item(0);
				String project__PROJECT_TITLE = "";
				String project__PROJECT_NUMBER = "";
				String project__PROJECT_FUNDING = "";
				String project__PROJECT_ABSTRACT = "";
				String project__PROJECT_START_DATE = "";
				String project__PROJECT_END_DATE = "";
				String project_awardInstrument = "";
				String project__LAST_UPDATE = "";
				String project__DATE_ENTERED = "";
				String institution_data__INSTITUTION_COUNTRY = "1";
				int project__ACTIVITY_STATUS = 0;
				int flag  = 0;
				String comment = "";
				Date dateNow = null;
				if(award != null) {
					
					//Project number	
					project__PROJECT_NUMBER = award.getElementsByTagName("AwardID").item(0).getTextContent();
					String query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
					ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
					try {
						result.next();
						String number = result.getString(1);
					}
					catch (Exception ex) {
						
					project__PROJECT_TITLE = award.getElementsByTagName("AwardTitle").item(0).getTextContent();
					project__PROJECT_ABSTRACT = award.getElementsByTagName("AbstractNarration").item(0).getTextContent();
	
					String project__PROJECT_TITLE1 = project__PROJECT_TITLE.toLowerCase() + project__PROJECT_ABSTRACT.toLowerCase();
					
					if(!(project__PROJECT_TITLE1.contains("food safety") || project__PROJECT_TITLE1.contains("feed safety") || 
							project__PROJECT_TITLE1.contains("food protection") || project__PROJECT_TITLE1.contains("foodborne") || 
							project__PROJECT_TITLE1.contains("salmonella") || project__PROJECT_TITLE1.contains("escherichia coli") || 
							project__PROJECT_TITLE1.contains("food defense") || project__PROJECT_TITLE1.contains("food regulatory") || 
							project__PROJECT_TITLE1.contains("produce and safety") || project__PROJECT_TITLE1.contains("seafood") ||
							project__PROJECT_TITLE1.contains("BOVINE SPONGIFORM ENCEPHALOPATHY".toLowerCase()) || 
							project__PROJECT_TITLE1.contains("shellfish") || 
							project__PROJECT_TITLE1.contains("fish product") || project__PROJECT_TITLE1.contains("fish oil") ||
							project__PROJECT_TITLE1.contains("avian influenza") || 
							(project__PROJECT_TITLE1.contains("food") && project__PROJECT_TITLE1.contains("safety")) || 
							(project__PROJECT_TITLE1.contains("campylobacter") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("clostridium") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("cryptosporidium") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("salmonella") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("shigella") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("listeria") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("staphylococcus") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("vibrio")  && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("hepatitis") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("cyclospora") && project__PROJECT_TITLE1.contains("food")) || 
							(project__PROJECT_TITLE1.contains("seafood")  && project__PROJECT_TITLE1.contains("safety")) || 
							(project__PROJECT_TITLE1.contains("food") && project__PROJECT_TITLE1.contains("processing")) || 
							project__PROJECT_TITLE1.contains("food Regulations") || 
							(project__PROJECT_TITLE1.contains("beef") && project__PROJECT_TITLE1.contains("safety")) ||
							(project__PROJECT_TITLE1.contains("e. coli") && project__PROJECT_TITLE1.contains("food")) ||
							project__PROJECT_TITLE1.contains("food packaging") || project__PROJECT_TITLE1.contains("food analysis") ||
							project__PROJECT_TITLE1.contains("food systems") || project__PROJECT_TITLE1.contains("food standards") || 
							project__PROJECT_TITLE1.contains("food additives")))
							continue;
					
					
	
						project__PROJECT_FUNDING = award.getElementsByTagName("AwardAmount").item(0).getTextContent();
						project__PROJECT_START_DATE = award.getElementsByTagName("AwardEffectiveDate").item(0).getTextContent();
						project__PROJECT_END_DATE = award.getElementsByTagName("AwardExpirationDate").item(0).getTextContent();
						project_awardInstrument = award.getElementsByTagName("AwardInstrument").item(0).getTextContent();
		
						dateNow = new Date();
						Date dateProj = new Date(project__PROJECT_END_DATE);
						long diff = dateNow.getTime() - dateProj.getTime();
						long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
						if (days >= 0) {
							if(project_awardInstrument.toLowerCase().contains("continuing"))
								project__ACTIVITY_STATUS = 5;
							else
								project__ACTIVITY_STATUS = 1;
						}
						else
							project__ACTIVITY_STATUS = 4;
	
					
					project__LAST_UPDATE = dateFormat.format(current);
					DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
					project__DATE_ENTERED = dateFormatEnter.format(current);
		
					// Get Investigator information
					Element investigator = (Element) doc.getElementsByTagName("Investigator").item(0);
					String FirstName = null;
					String LastName = null;
					String investigator__EMAIL_ADDRESS = null;
					String investigator__name = null;
					
					
					if(investigator != null) {
						FirstName = investigator.getElementsByTagName("FirstName").item(0).getTextContent();
						LastName = investigator.getElementsByTagName("LastName").item(0).getTextContent();
						investigator__EMAIL_ADDRESS = investigator.getElementsByTagName("EmailAddress").item(0).getTextContent();
					}
					if(LastName != null && FirstName != null) 
						investigator__name = LastName + ", " + FirstName;
		
		
					// Get Institution information
					Element institution = (Element) doc.getElementsByTagName("Institution").item(0);
					String institution_data__INSTITUTION_NAME = null;
					String institution_data__city_name = null;
					String states__state_name = null;
					String countries__COUNTRY_NAME = null;
					String states__states_abbrv = null;
					if(institution != null) {
						institution_data__INSTITUTION_NAME = institution.getElementsByTagName("Name").item(0).getTextContent();
						institution_data__city_name = institution.getElementsByTagName("CityName").item(0).getTextContent();
						states__state_name = institution.getElementsByTagName("StateName").item(0).getTextContent();
						states__states_abbrv = institution.getElementsByTagName("StateCode").item(0).getTextContent();
						countries__COUNTRY_NAME = institution.getElementsByTagName("CountryName").item(0).getTextContent();
					}
		
		
					// See if the institution ID exists, if not make it -1 to reflect we need to add.
					int insitution_data__ID = -1; 
					String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
					ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
					try {
						rs.next();
						insitution_data__ID = Integer.parseInt(rs.getString(1));
					}
					catch (Exception e) {
						comment = "Please populate institution fields by exploring the institution named on the project or identify if there is already disambiguated institution and add respective index.";
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
					int insitution_data__INSTITUION_STATE = -1; 
					String GetstateIDsql = "SELECT ID FROM " + dbname + ".states WHERE abbrv = \"" +  states__states_abbrv + "\";";
					ResultSet rs3 = MysqlConnect.sqlQuery(GetstateIDsql,host,user,passwd);
					try {
						rs3.next();
						insitution_data__INSTITUION_STATE = Integer.parseInt(rs3.getString(1));
					}
					catch (Exception e) {
						;
					}
		
					// Determining project type.
					int projecttype__ID = 999;
					if(project_awardInstrument.toLowerCase().contains("grant")) {
						projecttype__ID = 3;
					}
					else {
						// Checking to see if project type exists in projecttype table.
						String GetProjectTypeIDSQL = "SELECT ID FROM " + dbname + ".projecttype WHERE NAME LIKE \"" +  project_awardInstrument + "\";";
						ResultSet rs4 = MysqlConnect.sqlQuery(GetstateIDsql,host,user,passwd);
						try {
							rs4.next();
							projecttype__ID = Integer.parseInt(rs4.getString(1));
						}
						catch (Exception e) {
							;
						}
					}
		
					// Let us see if we can find the investigator in the already existing data. 
					// Condition: investigator must belong to the same institution that we just parsed.
					// We first use email, then name.
					int investigator_data__ID = -1;
					String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE EMAIL_ADDRESS LIKE \"" +  investigator__EMAIL_ADDRESS + "\" AND INSTITUTION = \"" + insitution_data__ID + "\";";
					ResultSet rs5 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
					try {
						rs5.next();
						investigator_data__ID = Integer.parseInt(rs5.getString(1));
					}
					catch (Exception e) {
						;
					}
					GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator__name + "\" AND INSTITUTION =\"" + insitution_data__ID + "\";";
					ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
					try {
						rs6.next();
						investigator_data__ID = Integer.parseInt(rs6.getString(1));
					}
					catch (Exception e) {
						;
					}
		
					// Create variables that are needed in the tables, but havent been created so far
					String project__source_url ="http://www.nsf.gov/awardsearch/showAward?AWD_ID="+ project__PROJECT_NUMBER +"&HistoricalAwards=false";
					String agency_index__aid = "6";
		
					String[] output = {project__PROJECT_NUMBER,project__PROJECT_TITLE,project__source_url,
							project__PROJECT_START_DATE,project__PROJECT_END_DATE,
							project__PROJECT_ABSTRACT,project__LAST_UPDATE,
							project__DATE_ENTERED,project__PROJECT_FUNDING,
							institution_data__INSTITUTION_NAME,
							institution_data__INSTITUTION_COUNTRY,
							String.valueOf(insitution_data__ID),
							agency_index__aid,comment};
		
					csvout.printRecord(output);
					}
				}
			}
		}
		csvout.close();

	}
}
