package com.jbt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.select.Elements;

import com.opencsv.CSVWriter;

public class Relu {

	public static String main(String[] links, String outfolder, String host, String user, String passwd, String dbname, String logfile) throws IOException {
		
		Relu.scrape(links,outfolder,host,user,passwd,dbname,logfile);
		return "RELU";
	}
	
	public static void scrape(String[] links, String outfolder, String host, String user, String passwd, String dbname, String logfile) throws IOException {
		//Get current date to assign filename
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);
		
		CSVWriter csvout = new CSVWriter(new FileWriter(outfolder+"RELU_"+currentStamp+".csv"),'\t');
		String[] header = {"project__PROJECT_NUMBER","project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_MORE_INFO","project__PROJECT_OBJECTIVE",
				"project__PROJECT_ABSTRACT","project__LAST_UPDATE",
				"project__DATE_ENTERED",
				"institution_data__INSTITUTION_NAME",
				"institution_data__INSTITUTION_ADDRESS1","institution_data__INSTITUTION_URL",
				"institution_data__INSTITUTION_ZIP",
				"institution_data__INSTITUTION_CITY", "institution_data__INSTITUTION_COUNTRY",
				"investigator_data__name",
				"institution_index__inst_id","investigator_index__inv_id",
				"agency_index__aid","investigator_data__INSTITUTION","comment"};
		csvout.writeNext(header);
		
		for (String link : links) {
			Document doc = Jsoup.connect(link)
					.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
				      .referrer("http://www.google.com").timeout(1000).get();
            Elements projLinks = doc.select("a[href*=relu.data-archive.ac.uk/explore-data/search-browse/project]");
            
            for (Element projLink : projLinks) {
            	Document finaldoc = Jsoup.connect(projLink.attr("href"))
						.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
    				      .referrer("http://www.google.com").timeout(1000).get();
				
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
				String institution_data__INSTITUTION_CITY = null;
				String institution_data__INSTITUTION_COUNTRY = null;
				String institution_data__INSTITUTION_ZIP = null;
				String institution_data__INSTITUTION_ADDRESS1 = null;
				String institution_data__INSTITUTION_URL = null;
				String agency_index__aid = "87";
				int institution_index__inst_id = -1;
				int investigator_data__INSTITUTION = -1;
				int investigator_index__inv_id = -1;
				String comment = "";
				String investigator_data__name = null;
				
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
				
				try {
					//Project number
					project__PROJECT_NUMBER = finaldoc.select("th:containsOwn(Award:)").first().nextElementSibling().text();
					query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
					ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
					try {
						result.next();
						String number = result.getString(1);
					}
					catch (Exception ex) {
							//Project source URL
							project__source_url = projLink.attr("href");
							
							//Project title
							project__PROJECT_TITLE = projLink.text();
							
							//Project start and end dates
							String dates = finaldoc.select("th:containsOwn(Dates:)").first().nextElementSibling().text();
							project__PROJECT_START_DATE = dates.split(" - ")[0].split("/")[dates.split(" - ")[0].split("/").length-1];
							project__PROJECT_END_DATE = dates.split(" - ")[1].split("/")[dates.split(" - ")[1].split("/").length-1];
							
							//Project objective
							project__PROJECT_OBJECTIVE = finaldoc.select("th:containsOwn(Dates:)").first().parent().nextElementSibling().nextElementSibling().text();
							
							//PI and institution name
							instInfo = finaldoc.select("th:containsOwn(PI:)").first().nextElementSibling().text();
							piInfo = instInfo.split(", ")[0];
							Pattern patUniv = Pattern.compile("University|College");
							Matcher matchUniv = patUniv.matcher(instInfo.split(", ")[instInfo.split(", ").length-1]);
							Pattern patCityLast = Pattern.compile("North Wyke|Aberdeen|Wallingford|Knoxville|York|Lancaster");
							Matcher matchCityLast = patCityLast.matcher(instInfo.split(", ")[instInfo.split(", ").length-1]);
							if (matchCityLast.find() && !matchUniv.find()) {
								institution_data__INSTITUTION_NAME = instInfo.split(", ")[instInfo.split(", ").length-2];
								institution_data__INSTITUTION_CITY = instInfo.split(", ")[instInfo.split(", ").length-1];
							}
							else if (matchUniv.find()) {
								institution_data__INSTITUTION_NAME = instInfo.split(", ")[instInfo.split(", ").length-1];
							}
							else if (instInfo.split(", ").length > 3) {
								institution_data__INSTITUTION_NAME = instInfo.split(", ")[instInfo.split(", ").length-2];
								institution_data__INSTITUTION_CITY = instInfo.split(", ")[instInfo.split(", ").length-1];
								if (institution_data__INSTITUTION_NAME.equals("Canberra")) {
									institution_data__INSTITUTION_NAME = instInfo.split(", ")[instInfo.split(", ").length-3];
									institution_data__INSTITUTION_CITY = instInfo.split(", ")[instInfo.split(", ").length-2];
								}
							} else {
								institution_data__INSTITUTION_NAME = instInfo.split(", ")[instInfo.split(", ").length-1];
							}
							
							//Check institution in MySQL DB
							query = "SELECT * from "+dbname+".institution_data where institution_name like \""+institution_data__INSTITUTION_NAME+"\"";
							result = MysqlConnect.sqlQuery(query,host,user,passwd);
							try {
								result.next();
								institution_index__inst_id = result.getInt(1);
								investigator_data__INSTITUTION = institution_index__inst_id;
							}
							catch (Exception e) {

							}
							//There can be several PIs - so, iterate through all and create separate rows for each
							for (String piOne : piInfo.split(" and ")) {
								piLastName = piOne.split(" ")[piOne.split(" ").length-1];
								Pattern patFname = Pattern.compile("^(.*?)\\s+[\\w-]+$");
								Matcher matcherFname = patFname.matcher(piOne.replace("Dr. ", ""));
								if (matcherFname.find()) {
									piFirstName = matcherFname.group(1);
								}
								piName = piLastName+", "+piFirstName;

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
										project__DATE_ENTERED,
										institution_data__INSTITUTION_NAME,
										institution_data__INSTITUTION_ADDRESS1, institution_data__INSTITUTION_URL,
										institution_data__INSTITUTION_ZIP,
										institution_data__INSTITUTION_CITY, institution_data__INSTITUTION_COUNTRY,
										investigator_data__name,
										String.valueOf(institution_index__inst_id),String.valueOf(investigator_index__inv_id),
										agency_index__aid,String.valueOf(investigator_data__INSTITUTION),comment};
								
									csvout.writeNext(output);
								
							}
					}
				}
				catch (Exception eee) {
					//Log the link and error
					
					try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)))) {
						StringWriter errors = new StringWriter();
						eee.printStackTrace(new PrintWriter(errors));
						out.println(currentDateLog
					    			+"   "
					    			+"Perhaps the link is broken or does not exist, e.g. Page Not Found - "+projLink.attr("href")+" ."
					    			+" Here is some help with traceback:"
					    			+errors.toString());
					}catch (IOException e) {

					}
				}
            }
		}
		csvout.close();
		
	}

}
