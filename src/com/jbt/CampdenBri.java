package com.jbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.select.Elements;

import com.opencsv.CSVWriter;

public class CampdenBri {
	
	
	public static String main(String outfolder, String[] links, String[] links2, String host, String user, String passwd, String dbname) throws IOException {
		
		CampdenBri.scrapeV2(links,outfolder,host,user,passwd,dbname);
		
		/* These links2 are not part of current FSRIO approach but perhaps Campden BRI changed their website
		and we can benefit from additional project info */
		CampdenBri.scrapeV1(links2,outfolder,host,user,passwd,dbname);
		
		return "CampdenBRI";
		
	}
	public static void scrapeV1(String[] links, String outfolder, String host, String user, String passwd, String dbname) throws IOException {
		//Get current date to assign filename
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);
		
		CSVWriter csvout = new CSVWriter(new FileWriter(outfolder+"CampdenBRI_"+currentStamp+".csv"),'\t');
		String[] header = {"project__PROJECT_NUMBER","project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_MORE_INFO","project__PROJECT_OBJECTIVE",
				"project__PROJECT_ABSTRACT","project__LAST_UPDATE",
				"project__DATE_ENTERED","investigator_data__EMAIL_ADDRESS",
				"investigator_data__name","investigator_data__PHONE_NUMBER",
				"institution_index__inst_id","investigator_index__inv_id",
				"agency_index__aid","investigator_data__INSTITUTION"};
		csvout.writeNext(header);
		
		for (String link : links) {
			Document finaldoc = Jsoup.parse(new URL(link).openStream(), "UTF-8","");
			((org.jsoup.nodes.Document) finaldoc).outputSettings().charset().forName("UTF-8");
            ((org.jsoup.nodes.Document) finaldoc).outputSettings().escapeMode(EscapeMode.xhtml);
            Element content = finaldoc.select("main").first();
			content.select("br").remove(); 
            
			//Individual projects
			Elements projInfo = content.select("p");
			for (int i=0; i<projInfo.size();i++) {
				
				
				
				Element projElem = projInfo.get(i);
				
				Pattern patProj = Pattern.compile("Campden BRI [Pp]roject");
				Matcher matchProj = patProj.matcher(projElem.text());
				
				if (matchProj.find()) {
					//Declare needed strings
					String project__PROJECT_NUMBER = null;
					String project__PROJECT_TITLE = null;
					String project__source_url = null;
					String project__PROJECT_START_DATE = null;
					String project__PROJECT_END_DATE = null;
					String project__PROJECT_MORE_INFO = null;
					String project__PROJECT_OBJECTIVE = "";
					String project__PROJECT_ABSTRACT = null;
					String project__LAST_UPDATE = null;
					String project__DATE_ENTERED = null;
					String institution_data__INSTITUTION_NAME = null;
					String investigator_data__PHONE_NUMBER = null;
					String agency_index__aid = "139";
					int institution_index__inst_id = 437;
					int investigator_data__INSTITUTION = 437;
					int investigator_index__inv_id = -1;
					String comment = "";
					String investigator_data__name = null;
					String investigator_data__EMAIL_ADDRESS = null;
					
					//Processing variables
					String piInfo= null;
					String piName = null;
					String instInfo = null;
					String query = null;
					String piLastName = null;
					String piFirstName = null;
					
					//Dates entered and updated
					DateFormat dateFormatEntered = new SimpleDateFormat("yyyy-MM-dd");
					String currentEntered = dateFormatEntered.format(current);
					project__DATE_ENTERED = currentEntered;
					project__LAST_UPDATE = currentDateLog;
					
					//Project title
					project__PROJECT_TITLE = projElem.previousElementSibling().text();
					
					//Project number and date
					List<String> matches = new ArrayList<String>();
					Pattern numdate = Pattern.compile("\\d+");
					Matcher matchnumdate = numdate.matcher(projElem.text());
					while (matchnumdate.find()) {
						matches.add(matchnumdate.group());
					}
					String[] allMatches = new String[matches.size()];
					allMatches = matches.toArray(allMatches);
					if (matches.size() == 3) {
						project__PROJECT_NUMBER = allMatches[0];
						project__PROJECT_START_DATE = allMatches[1];
						project__PROJECT_END_DATE = allMatches[2];
					} else if (matches.size() == 2) {
						project__PROJECT_NUMBER = "tbc";
						project__PROJECT_START_DATE = allMatches[0];
						project__PROJECT_END_DATE = allMatches[1];
					} else {
						project__PROJECT_NUMBER = allMatches[0];
					}

					//Project objective and PI info
					int underSize;
					if (i+5 > projInfo.size()) {
						underSize = i+(projInfo.size()-i);
					} else {
						underSize = i+5;
					}
					for (int indElem=i+1;indElem<underSize;indElem++) {
						Element nextSib = projInfo.get(indElem);
						if (nextSib.attr("class") == "") {
							project__PROJECT_OBJECTIVE += nextSib.text() + " ";
						} else {
							//PI info
							piInfo = nextSib.select("strong").text();
							piLastName = piInfo.split(" ")[piInfo.split(" ").length-1];
							Pattern patFname = Pattern.compile("^(.*?)\\s+\\w+$");
							Matcher matcherFname = patFname.matcher(piInfo.replace("Dr. ", ""));
							while (matcherFname.find()) {
								piFirstName = matcherFname.group(1);
							}
							piName = piLastName+", "+piFirstName;
							investigator_data__EMAIL_ADDRESS = nextSib.select("a").text();
							nextSib.select("strong").remove();
							nextSib.select("a").remove();
							investigator_data__PHONE_NUMBER = nextSib.text().replace(" ","").replace("+","");
							break;
						}
					}
				
			
			if (project__PROJECT_NUMBER != "tbc") {
				query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
				ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
				try {
					result.next();
					String number = result.getString(1);
				}
				catch (Exception ex) {
				
					//Check PI name in MySQL DB
					query = "SELECT * FROM "+dbname+".investigator_data where name like \""+piName+"\"";
					result = MysqlConnect.sqlQuery(query,host,user,passwd);
					try {
						result.next();
						investigator_index__inv_id = result.getInt(1);
					}
					catch (Exception e) {
						query = "SELECT * FROM "+dbname+".investigator_data where name regexp \"^"+piLastName+", "+piFirstName.substring(0,1)+"\"";
						result = MysqlConnect.sqlQuery(query,host,user,passwd);
						try {
							result.next();
							investigator_index__inv_id = result.getInt(1);
						}
						catch (Exception except) {
							
						}	
					}
					
					if (investigator_index__inv_id == -1) {
						investigator_data__name = piName;
					} 
					
					//Write resultant values into CSV
					String[] output = {project__PROJECT_NUMBER,project__PROJECT_TITLE,project__source_url,
							project__PROJECT_START_DATE,project__PROJECT_END_DATE,
							project__PROJECT_MORE_INFO,project__PROJECT_OBJECTIVE,
							project__PROJECT_ABSTRACT,project__LAST_UPDATE,
							project__DATE_ENTERED,investigator_data__EMAIL_ADDRESS,
							investigator_data__name,investigator_data__PHONE_NUMBER,
							String.valueOf(institution_index__inst_id),String.valueOf(investigator_index__inv_id),
							agency_index__aid,String.valueOf(investigator_data__INSTITUTION)};
					
						csvout.writeNext(output);	
					
					}
				}
			}
		}
		}
		csvout.close();
		
	}
	
	public static void scrapeV2(String[] links, String outfolder, String host, String user, String passwd, String dbname) throws IOException {
		//Get current date to assign filename
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);
		
		CSVWriter csvout = new CSVWriter(new FileWriter(outfolder+"CampdenBRI_V2_"+currentStamp+".csv"),'\t');
		String[] header = {"project__PROJECT_NUMBER","project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_MORE_INFO","project__PROJECT_OBJECTIVE",
				"project__PROJECT_ABSTRACT","project__LAST_UPDATE",
				"project__DATE_ENTERED","investigator_data__EMAIL_ADDRESS",
				"investigator_data__name","investigator_data__PHONE_NUMBER",
				"institution_index__inst_id","investigator_index__inv_id",
				"agency_index__aid","investigator_data__INSTITUTION"};
		csvout.writeNext(header);
		
		for (String link : links) {
			
			Document finaldoc = Jsoup.parse(new URL(link).openStream(), "UTF-8","");
			((org.jsoup.nodes.Document) finaldoc).outputSettings().charset().forName("UTF-8");
            ((org.jsoup.nodes.Document) finaldoc).outputSettings().escapeMode(EscapeMode.xhtml);
            Element content = finaldoc.select("div.main_box").first();
			content.select("br").remove(); 
            
           
			
			//Individual projects
			Elements projInfo = content.select("p");
			for (int i=0; i<projInfo.size();i++) {
				
				
				
				Element projElem = projInfo.get(i);
				
				Pattern patProj = Pattern.compile("Campden BRI [Pp]roject");
				Matcher matchProj = patProj.matcher(projElem.text());

				if (matchProj.find()) {
					//Declare needed strings
					String project__PROJECT_NUMBER = null;
					String project__PROJECT_TITLE = null;
					String project__source_url = null;
					String project__PROJECT_START_DATE = null;
					String project__PROJECT_END_DATE = null;
					String project__PROJECT_MORE_INFO = null;
					String project__PROJECT_OBJECTIVE = "";
					String project__PROJECT_ABSTRACT = null;
					String project__LAST_UPDATE = null;
					String project__DATE_ENTERED = null;
					String institution_data__INSTITUTION_NAME = null;
					String investigator_data__PHONE_NUMBER = null;
					String agency_index__aid = "139";
					int institution_index__inst_id = 437;
					int investigator_data__INSTITUTION = 437;
					int investigator_index__inv_id = -1;
					String comment = "";
					String investigator_data__name = null;
					String investigator_data__EMAIL_ADDRESS = null;
					
					//Processing variables
					String piInfo= null;
					String piName = null;
					String instInfo = null;
					String query = null;
					String piLastName = null;
					String piFirstName = null;
					
					//Dates entered and updated
					DateFormat dateFormatEntered = new SimpleDateFormat("yyyy-MM-dd");
					String currentEntered = dateFormatEntered.format(current);
					project__DATE_ENTERED = currentEntered;
					project__LAST_UPDATE = currentDateLog;
					
					//Project title
					project__PROJECT_TITLE = projElem.select("strong").text();

					//Project number and date
					List<String> matches = new ArrayList<String>();
					Pattern numdate = Pattern.compile("\\d+");
					Matcher matchnumdate = numdate.matcher(projElem.text());
					while (matchnumdate.find()) {
						matches.add(matchnumdate.group());
					}
					String[] allMatches = new String[matches.size()];
					allMatches = matches.toArray(allMatches);
					if (matches.size() == 3) {
						project__PROJECT_NUMBER = allMatches[0];
						project__PROJECT_START_DATE = allMatches[1];
						project__PROJECT_END_DATE = allMatches[2];
					} else if (matches.size() == 2) {
						project__PROJECT_NUMBER = "tbc";
						project__PROJECT_START_DATE = allMatches[0];
						project__PROJECT_END_DATE = allMatches[1];
					} else {
						project__PROJECT_NUMBER = allMatches[0];
					}

					//Project objective and PI info
					int underSize;
					if (i+5 > projInfo.size()) {
						underSize = i+(projInfo.size()-i);
					} else {
						underSize = i+5;
					}
					for (int indElem=i+1;indElem<underSize;indElem++) {
						Element nextSib = projInfo.get(indElem);
						Pattern patSib = Pattern.compile("Contact:");
						Matcher matchSib = patSib.matcher(nextSib.text());
						if (matchSib.find()) {
							//PI info
							piInfo = nextSib.select("strong").text();
							piLastName = piInfo.split(" ")[piInfo.split(" ").length-1];
							Pattern patFname = Pattern.compile("^(.*?)\\s+\\w+$");
							Matcher matcherFname = patFname.matcher(piInfo.replace("Dr. ", ""));
							while (matcherFname.find()) {
								piFirstName = matcherFname.group(1);
							}
							piName = piLastName+", "+piFirstName;
							investigator_data__EMAIL_ADDRESS = nextSib.select("a").text();
							nextSib.select("strong").remove();
							nextSib.select("a").remove();
							investigator_data__PHONE_NUMBER = nextSib.text().replace("Contact: ","").replace("+","").replace(" e-mail:","");
							break;

						} else {
							project__PROJECT_OBJECTIVE += nextSib.text() + " ";
						}
					}
				
			
			if (project__PROJECT_NUMBER != "tbc") {
				query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
				ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
				try {
					result.next();
					String number = result.getString(1);
				}
				catch (Exception ex) {
				
					//Check PI name in MySQL DB
					query = "SELECT * FROM "+dbname+".investigator_data where name like \""+piName+"\"";
					result = MysqlConnect.sqlQuery(query,host,user,passwd);
					try {
						result.next();
						investigator_index__inv_id = result.getInt(1);
					}
					catch (Exception e) {
						query = "SELECT * FROM "+dbname+".investigator_data where name regexp \"^"+piLastName+", "+piFirstName.substring(0,1)+"\"";
						result = MysqlConnect.sqlQuery(query,host,user,passwd);
						try {
							result.next();
							investigator_index__inv_id = result.getInt(1);
						}
						catch (Exception except) {
							
						}	
					}
					
					if (investigator_index__inv_id == -1) {
						investigator_data__name = piName;
					} 
					
					//Write resultant values into CSV
					String[] output = {project__PROJECT_NUMBER,project__PROJECT_TITLE,project__source_url,
							project__PROJECT_START_DATE,project__PROJECT_END_DATE,
							project__PROJECT_MORE_INFO,project__PROJECT_OBJECTIVE,
							project__PROJECT_ABSTRACT,project__LAST_UPDATE,
							project__DATE_ENTERED,investigator_data__EMAIL_ADDRESS,
							investigator_data__name,investigator_data__PHONE_NUMBER,
							String.valueOf(institution_index__inst_id),String.valueOf(investigator_index__inv_id),
							agency_index__aid,String.valueOf(investigator_data__INSTITUTION)};
					
						csvout.writeNext(output);	
					
					}
				}
			}
		}
		}
		csvout.close();
		
	}
}
