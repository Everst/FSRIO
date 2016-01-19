package com.jbt;
/* Fix potatoes multiple institutions sep by "&" or ", "
 * Look at dairy in particular
 * Cereals - something is definitely broken
 * Pork - dates do not parse at all plus need to add institution fields
 */
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class AHDB {
	public static void main(String outfolder, String[] links, String host, String user, String passwd, String dbname, String logfile) throws Exception {
		Logger logger = Logger.getLogger ("");
		logger.setLevel (Level.OFF);
		for (String link : links) {
			if (link.replace("potatoes", "").length() < link.length()) {
				AHDB.potatoes(outfolder, link,host,user,passwd,dbname);
			}
			if (link.replace("horticulture", "").length() < link.length()) {
				AHDB.horticulture(outfolder, link,host,user,passwd,dbname);
			}
			if (link.replace("dairy", "").length() < link.length()) {
				AHDB.dairy(outfolder, link,host,user,passwd,dbname);
			}
			if (link.replace("beefandlamb", "").length() < link.length()) {
				AHDB.meat(outfolder, link,host,user,passwd,dbname,logfile);
			}
			if (link.replace("cereals", "").length() < link.length()) {
				AHDB.cereals(outfolder, link,host,user,passwd,dbname);
			}
			if (link.replace("pork", "").length() < link.length()) {
				AHDB.pork(outfolder,link,host,user,passwd,dbname);
			}
		}
		
	}

	public static void potatoes(String outfolder, String url, String host, String user, String passwd, String dbname) throws Exception {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_potatoes_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER", "project__PROJECT_TITLE", 
				"project__source_url", "project__PROJECT_START_DATE",
				"project__PROJECT_END_DATE", "project__PROJECT_OBJECTIVE",
				"project__LAST_UPDATE", "project__DATE_ENTERED",  "agency_index__aid",
				"investigator_data__name", "institution_data__ID", "investigator_data__ID","institution_data__INSTITUTION_NAME" };
		csvout.printRecord(header);

		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		
		for (int i = 0; i < 4; i++ ){
			if(i != 0) {
				url = url + "&page=" + String.valueOf(i);
			}
			HtmlPage startPage = webClient.getPage(url);
			Document doc = Jsoup.parse(startPage.asXml());
			Elements links = doc.select("li[class=listing-publication").select("div[class=pub-content]").select("a");
			for (Element link: links) {
				if (link.attr("href").contains(".pdf")) continue;
				String project__PROJECT_NUMBER = "";
				String project__PROJECT_TITLE = "";
				String project__source_url = "";
				String project__PROJECT_START_DATE = "";
				String project__PROJECT_END_DATE = "";
				String project__PROJECT_OBJECTIVE = "";
				String project__LAST_UPDATE = "";
				String project__DATE_ENTERED = "";
				String project__PROJECT_FUNDING = "";
				String agency_index__aid = "146";
				String investigator_data__name = "";
				int institution_index__inst_id = -1;
				String investigator_data__ID = "-1";
				String comment = "";
				String institution_data__ID  = "-1";
				
				//Processing variables
				String piName = null;
				String piLastName = null;
				String piFirstName = null;
				
				//Institution variables
				String institution_data__INSTITUTION_NAME = "";
				
				
				project__source_url = "http://potatoes.ahdb.org.uk/" + link.attr("href");
				Document finaldoc = Jsoup.connect(project__source_url).timeout(50000).get();
				project__PROJECT_TITLE = finaldoc.select(":containsOwn(Full Research Project Title)").text().replace("Full Research Project Title:", "").trim();
				project__PROJECT_NUMBER = finaldoc.select("h1[id=page-title]").text().split(" ")[0];
				if (project__PROJECT_TITLE.equals("")) {
					project__PROJECT_TITLE = finaldoc.select("h1[id=page-title]").text().replace(project__PROJECT_NUMBER+" ", "");
				}
				
				//Sometimes there's no project number and therefore the link need to be passed
				Pattern checkProjNum = Pattern.compile("\\d+");
				Matcher matchProjNum = checkProjNum.matcher(project__PROJECT_NUMBER);
				if (!matchProjNum.find()) {
					continue;
				}
				//Project number	
				String query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
				ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
				try {
					result.next();
					String number = result.getString(1);
				}
				catch (Exception ex) {
				
				//Parse investigator name in correct format
				piName = finaldoc.select("div[class=field field-name-field-author field-type-text field-label-inline clearfix]").select("div[class=field-item even]").text();
				piLastName = piName.split(" ")[piName.split(" ").length-1];
				piFirstName = piName.replace(" "+piLastName,"");
				investigator_data__name = piLastName+", "+piFirstName;
				
				//Parse institution name
				institution_data__INSTITUTION_NAME = finaldoc.select("div[class=field field-name-field-contractor field-type-text field-label-inline clearfix]").select("div[class=field-item even]").text();
				
				project__PROJECT_OBJECTIVE = finaldoc.select("div[class=field field-name-body field-type-text-with-summary field-label-hidden]").select("div[class=field-item even]").text();
				String duration = finaldoc.select("div[class=field field-name-field-headline field-type-text field-label-hidden]").select("div[class=field-item odd]").text();
				Pattern p = Pattern.compile("\\d+");
				Matcher matcher = p.matcher(duration);
				if (matcher.find()) {
					project__PROJECT_START_DATE = matcher.group();
				}
				if (matcher.find()) {
					project__PROJECT_END_DATE = matcher.group();
				}
				
				// Find institution
				String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
				ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
				try {
					rs.next();
					institution_data__ID = rs.getString(1);
				}
				catch (Exception e) {
					comment = "Please populate institution fields by exploring the institution named on the project.";
				}
				
				//Find investigator
				String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
				ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
				try {
					rs6.next();
					investigator_data__ID = rs6.getString(1);
				}
				catch (Exception e) {
					;
				}
				project__LAST_UPDATE = dateFormat.format(current);
				DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
				project__DATE_ENTERED = dateFormatEnter.format(current);
				String[] output = {project__PROJECT_NUMBER, project__PROJECT_TITLE, 
						project__source_url, project__PROJECT_START_DATE,
						project__PROJECT_END_DATE, project__PROJECT_OBJECTIVE,
						project__LAST_UPDATE, project__DATE_ENTERED,  agency_index__aid,
						investigator_data__name, institution_data__ID, investigator_data__ID,institution_data__INSTITUTION_NAME };
				csvout.printRecord(output);
				
			}
		}
		}
		csvout.close();
	}
	
	public static void horticulture(String outfolder, String url, String host, String user, String passwd, String dbname) throws Exception {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_horticulture_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER", "project__PROJECT_TITLE", 
				"project__source_url", "project__PROJECT_START_DATE",
				"project__PROJECT_END_DATE", "project__PROJECT_OBJECTIVE",
				"project__LAST_UPDATE", "project__DATE_ENTERED",  "agency_index__aid",
				"investigator_data__name", "institution_data__ID", "investigator_data__ID", "project__PROJECT_FUNDING",
				"institution_data__INSTITUTION_NAME"};
		csvout.printRecord(header);
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		HtmlPage startPage = webClient.getPage(url);
		Document doc = Jsoup.parse(startPage.asXml());
		
		int nPages = doc.select("ul[class=pager]").select("li[class=pager-item").size();
		String url2 = "";
		for (int i = 0; i <= nPages; ++i) {
			if(i != 0) {
			    url2 = url + "?page=" + String.valueOf(i);
			   }
			   else {
			    url2 = url;
			   }
			startPage = webClient.getPage(url2);

			doc = Jsoup.parse(startPage.asXml());
			Elements links = doc.select("article").select("li[class=node-readmore first last]").select("a");

			for(Element link: links) {
				String project__PROJECT_NUMBER = "";
				String project__PROJECT_TITLE = "";
				String project__source_url = "";
				String project__PROJECT_START_DATE = "";
				String project__PROJECT_END_DATE = "";
				String project__PROJECT_OBJECTIVE = "";
				String project__LAST_UPDATE = "";
				String project__DATE_ENTERED = "";
				String project__PROJECT_FUNDING = "";
				String agency_index__aid = "146";
				String investigator_data__name = "";
				int institution_index__inst_id = -1;
				String investigator_data__ID = "-1";
				String comment = "";
				String institution_data__ID  = "-1";
				
				//Processing variables
				String piName = null;
				String piLastName = null;
				String piFirstName = null;
				
				//Institution variables
				String institution_data__INSTITUTION_NAME = "";
				
				
				project__source_url = "http://horticulture.ahdb.org.uk/" + link.attr("href");
				Document finaldoc = Jsoup.connect(project__source_url).timeout(50000).get();
				project__PROJECT_TITLE = finaldoc.select("span[property=dc:title]").attr("content");
				project__PROJECT_NUMBER = finaldoc.select("header").select("h2").text().split("-")[0].replace("HDC user info and login Search form ", "");
				
				//Project number check within DB	
				String query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
				ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
				try {
					result.next();
					String number = result.getString(1);
				}
				catch (Exception ex) {
				
				
				project__PROJECT_OBJECTIVE = 
						finaldoc.select("div[class=content]").select("div[class=field field-name-body field-type-text-with-summary field-label-hidden]").text(); 
				project__PROJECT_START_DATE = finaldoc.select("div[class=content]").select("div[class=field field-name-field-start-date field-type-datetime field-label-inline clearfix]").select("span").attr("content").substring(0, 4);
				project__PROJECT_END_DATE = finaldoc.select("div[class=content]").select("div[class=field field-name-field-release-date field-type-datetime field-label-inline clearfix]").select("span").attr("content").substring(0, 4);
				String temp = finaldoc.select("div[class=content]").select("div[class=field field-name-field-author field-type-text field-label-inline clearfix]").select("div[class=field-item even]").text();
				
				//Parse investigator name in correct format
				piName = temp.split(",")[0].replace(" Warwick Crop Centre","");
				Pattern patName = Pattern.compile("^Prof |^Professor |^Dr. |^Doctor |^Dr |^Ms |^Mrs |^Mr ");
				Matcher matchName = patName.matcher(piName);
				piName = matchName.replaceAll("");
				piLastName = piName.split(" ")[piName.split(" ").length-1];
				piFirstName = piName.replace(" "+piLastName,"");
				investigator_data__name = piLastName+", "+piFirstName;
				
				try {
					institution_data__INSTITUTION_NAME = temp.split(",")[1];	
				}
				catch (Exception e) {;}
				String cost = finaldoc.select(":containsOwn(\u00A3)").text().replaceAll(",", "").replaceAll(" ", "");
				if(cost.toLowerCase().contains("cost") && cost.length() < 100) {
					Pattern p = Pattern.compile("\\d+");
					Matcher m = p.matcher(cost);
					if (m.find()) {
						project__PROJECT_FUNDING =  m.group();
					}
				}
				try {
					project__PROJECT_OBJECTIVE = project__PROJECT_OBJECTIVE.substring(project__PROJECT_OBJECTIVE.toLowerCase().indexOf("summary"));
				}

				catch(Exception e) {;}
				project__PROJECT_OBJECTIVE =finaldoc.select("p").text();
				project__PROJECT_NUMBER = finaldoc.select("header").select("h2").text().replaceAll("HDC user info and login Search form", "").split("-")[0].trim();
				project__LAST_UPDATE = dateFormat.format(current);
				DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
				project__DATE_ENTERED = dateFormatEnter.format(current);
				// Find institution
				String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
				ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
				try {
					rs.next();
					institution_data__ID = rs.getString(1);
				}
				catch (Exception e) {
					comment = "Please populate institution fields by exploring the institution named on the project.";
				}
				
				//Find investigator
				String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
				ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
				try {
					rs6.next();
					investigator_data__ID = rs6.getString(1);
				}
				catch (Exception e) {
					;
				}
				String[] output = {project__PROJECT_NUMBER, project__PROJECT_TITLE, 
						project__source_url, project__PROJECT_START_DATE,
						project__PROJECT_END_DATE, project__PROJECT_OBJECTIVE,
						project__LAST_UPDATE, project__DATE_ENTERED,  agency_index__aid,
						investigator_data__name, institution_data__ID, investigator_data__ID, project__PROJECT_FUNDING,
						institution_data__INSTITUTION_NAME};
				csvout.printRecord(output);
				}
			}
		}
		csvout.close();

	}

	public static void dairy(String outfolder, String url, String host, String user, String passwd, String dbname) throws Exception {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_dairy_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER", "project__PROJECT_TITLE", 
				"project__source_url", "project__PROJECT_START_DATE",
				"project__PROJECT_END_DATE", "project__PROJECT_OBJECTIVE",
				"project__LAST_UPDATE", "project__DATE_ENTERED",  "agency_index__aid",
				"investigator_data__name", "institution_data__ID", "investigator_data__ID" };
		csvout.printRecord(header);

		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		HtmlPage startPage = webClient.getPage(url);
		Document doc = Jsoup.parse(startPage.asXml());
		Elements links = doc.select("a");
		int i = 0;
		for (Element link: links) {
			if (!(link.attr("href").contains("current-projects"))) {
				continue;
			}
			if (i <2 ) {i+=1; continue;}
			String project__PROJECT_NUMBER = "";
			String project__PROJECT_TITLE = "";
			String project__source_url = "";
			String project__PROJECT_START_DATE = "";
			String project__PROJECT_END_DATE = "";
			String project__PROJECT_OBJECTIVE = "";
			String project__LAST_UPDATE = "";
			String project__DATE_ENTERED = "";
			String project__PROJECT_FUNDING = "";
			String agency_index__aid = "146";
			String investigator_data__name = "";
			int institution_index__inst_id = -1;
			String investigator_data__ID = "-1";
			String comment = "";
			String institution_data__ID  = "-1";
			
			
			//Institution variables
			String institution_data__INSTITUTION_NAME = "";
			Document finaldoc = null;
			project__source_url = "http://dairy.ahdb.org.uk/" + link.attr("href");
			try {
				finaldoc = Jsoup.connect(project__source_url).timeout(50000).get();
			}
			catch (Exception eee) {
				HtmlPage startPageProj = webClient.getPage(project__source_url);
				finaldoc = Jsoup.parse(startPageProj.asXml());
			}
			project__PROJECT_TITLE = finaldoc.select("div[class=column2]").select("h3").text();
			project__PROJECT_START_DATE = finaldoc.select("p:containsOwn(Start Date)").text();
			String temp = finaldoc.select("p").text();
			try {
				project__PROJECT_START_DATE = temp.substring(temp.indexOf("Start Date"), temp.indexOf("Completion Date")).replaceAll("\\D+", "");	
			}
			catch(Exception e) {;}
			try {
				project__PROJECT_END_DATE = temp.substring(temp.indexOf("Completion Date"), temp.indexOf("Lead Contractor")).replaceAll("\\D+", "");;
			}
			catch(Exception e) {;}
			try {
			institution_data__INSTITUTION_NAME = temp.substring(temp.indexOf("Lead Contractor"), temp.toLowerCase().indexOf("other delivery")).replace("Lead Contractor", "").replace("- Research Partnership study", "").trim();
			}
			catch(Exception e) {;}
			if (institution_data__INSTITUTION_NAME == "") {
				try {
					
				institution_data__INSTITUTION_NAME = temp.substring(temp.indexOf("Lead Contractor"), temp.indexOf("Funder")).replace("Lead Contractor", "").replace("- Research Partnership study", "").trim();
			}
			catch(Exception e) {;}
			}
			temp = finaldoc.text();
			try {
				project__PROJECT_OBJECTIVE = temp.substring(temp.indexOf("Aims"), temp.toLowerCase().indexOf("start date")).replace("Aims & Objectives", "");	
			}
			catch (Exception e) {;}
			project__LAST_UPDATE = dateFormat.format(current);
			DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
			project__DATE_ENTERED = dateFormatEnter.format(current);
			
			// Find institution
			String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
			ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
			try {
				rs.next();
				institution_data__ID = rs.getString(1);
			}
			catch (Exception e) {
				comment = "Please populate institution fields by exploring the institution named on the project.";
			}
			
			//Find investigator
			String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
			ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
			try {
				rs6.next();
				investigator_data__ID = rs6.getString(1);
			}
			catch (Exception e) {
				;
			}
			String[] output = {project__PROJECT_NUMBER, project__PROJECT_TITLE, 
					project__source_url, project__PROJECT_START_DATE,
					project__PROJECT_END_DATE, project__PROJECT_OBJECTIVE,
					project__LAST_UPDATE, project__DATE_ENTERED,  agency_index__aid,
					investigator_data__name, institution_data__ID, investigator_data__ID };
			csvout.printRecord(output);
			
		}
		csvout.close();
	}
	
	public static void meat(String outfolder, String url, String host, String user, String passwd, String dbname, String logfile) throws Exception {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_meat_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER", "project__PROJECT_TITLE", 
				"project__source_url", "project__PROJECT_START_DATE",
				"project__PROJECT_END_DATE", "project__PROJECT_OBJECTIVE",
				"project__LAST_UPDATE", "project__DATE_ENTERED",  "agency_index__aid",
				 "institution_data__ID", "investigator_data__ID", "institution_data__INSTITUTION_NAME" };
		csvout.printRecord(header);
		String beef = url + "meat-eating-quality-and-safety-beef/";
		String sheep = url + "meat-eating-quality-and-safety-sheep/";
		String generic = url + "meat-eating-quality-and-safety-generic/";
		String []  urls = {beef, sheep, generic};
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		
		for (String url1: urls) {
			
			try {
				HtmlPage startPage = null;
				Document finaldoc = null;
				try {
					startPage = webClient.getPage(url1);
					finaldoc = Jsoup.parse(startPage.asXml());
					}
				catch (Exception exc) {
					finaldoc = Jsoup.connect(url1).timeout(50000).get();
				}
				Elements links = finaldoc.select("li").select("a[title!=\"\"]");
				for (Element link : links) {
					String project__PROJECT_NUMBER = "";
					String project__PROJECT_TITLE = "";
					String project__source_url = "";
					String project__PROJECT_START_DATE = "";
					String project__PROJECT_END_DATE = "";
					String project__PROJECT_OBJECTIVE = "";
					String project__LAST_UPDATE = "";
					String project__DATE_ENTERED = "";
					String project__PROJECT_FUNDING = "";
					String agency_index__aid = "146";
					String investigator_data__name = "";
					int institution_index__inst_id = -1;
					String investigator_data__ID = "-1";
					String comment = "";
					String institution_data__ID  = "-1";
					
					
					//Institution variables
					String institution_data__INSTITUTION_NAME = "";
					
					//Project URL
					project__source_url = link.attr("href");
					
					try {
						startPage = webClient.getPage(project__source_url);
						finaldoc = Jsoup.parse(startPage.asXml());
					}
					catch (Exception exc) {
						finaldoc = Jsoup.connect(project__source_url).timeout(50000).get();
					}
					
					project__PROJECT_NUMBER = finaldoc.select(":containsOwn(project number)").text();
					project__PROJECT_NUMBER = project__PROJECT_NUMBER.toLowerCase().replaceAll("project number", "").replace(":", "").trim();
					project__PROJECT_NUMBER = project__PROJECT_NUMBER.replaceAll(String.valueOf((char) 160), "");
					//Project number	
					String query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
					ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
					try {
						result.next();
						String number = result.getString(1);
					}
					catch (Exception ex) {
					String temp = finaldoc.select("div[class=entry-content]").text();
					try {
						project__PROJECT_TITLE = temp.substring(0, temp.toLowerCase().indexOf("project number"));	
					}
					catch(Exception e) {
						project__PROJECT_TITLE = finaldoc.select("article").select("h1[class=entry-title]").text();
					}
					if (project__PROJECT_TITLE == "") 
						project__PROJECT_TITLE = finaldoc.select("title").text();
					institution_data__INSTITUTION_NAME = finaldoc.select(":containsOwn(lead contractor)").text();
					institution_data__INSTITUTION_NAME = institution_data__INSTITUTION_NAME.toLowerCase().replaceAll("lead contractor", "").replace(":", "").trim();
					institution_data__INSTITUTION_NAME = institution_data__INSTITUTION_NAME.replaceAll(String.valueOf((char) 160), "");
					String duration = finaldoc.select(":containsOwn(start & end date)").text();
					Pattern p = Pattern.compile("\\d{4}");
					Matcher matcher = p.matcher(duration);
					if (matcher.find()) {
						project__PROJECT_START_DATE = matcher.group();
					}
					if (matcher.find()) {
						project__PROJECT_END_DATE = matcher.group();
					}
					project__PROJECT_OBJECTIVE = finaldoc.text();
					try {
						project__PROJECT_OBJECTIVE.substring(project__PROJECT_OBJECTIVE.toLowerCase().indexOf("problem:"), project__PROJECT_OBJECTIVE.toLowerCase().indexOf("final report"));
						
					}
					catch(Exception e) {
						;
						}
					if (project__PROJECT_OBJECTIVE == "" ) {
						try {
							project__PROJECT_OBJECTIVE.substring(project__PROJECT_OBJECTIVE.toLowerCase().indexOf("problem:"));
						}
						catch (Exception e) {}
					}
					
					// Find institution
					String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
					ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
					try {
						rs.next();
						institution_data__ID = rs.getString(1);
					}
					catch (Exception e) {
						comment = "Please populate institution fields by exploring the institution named on the project.";
					}
					
					//Find investigator
					String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
					ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
					try {
						rs6.next();
						investigator_data__ID = rs6.getString(1);
					}
					catch (Exception e) {
						;
					}
					project__LAST_UPDATE = dateFormat.format(current);
					DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
					project__DATE_ENTERED = dateFormatEnter.format(current);
					String[] output = {project__PROJECT_NUMBER, project__PROJECT_TITLE, 
							project__source_url, project__PROJECT_START_DATE,
							project__PROJECT_END_DATE, project__PROJECT_OBJECTIVE,
							project__LAST_UPDATE, project__DATE_ENTERED,  agency_index__aid,
							institution_data__ID, investigator_data__ID, institution_data__INSTITUTION_NAME};
					csvout.printRecord(output);
					}
				}
			}
			catch (Exception eee) {
				try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)))) {
					StringWriter errors = new StringWriter();
					eee.printStackTrace(new PrintWriter(errors));
					out.println(currentDateLog
				    			+"   "
				    			+"Perhaps the link is broken or does not exist - "+url1+" ."
				    			+" Here is some help with traceback:"
				    			+errors.toString());
				}catch (IOException e) {

				}
			}
			
		}
		csvout.close();
		
	}
	
	public static void cereals(String outfolder, String url, String host, String user, String passwd, String dbname) throws Exception {
		String weed = url.replace("disease", "weed");
		String pest = url.replace("disease", "pest");
		String nutrient = url.replace("nutrient", "weed");
		String soil = url.replace("disease", "soil");
		String environment = url.replace("disease", "environment");
		String grain = url.replace("disease", "grain-quality");

		String urls [] = {url, weed, pest, nutrient, soil, environment, grain};
		
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);

		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_cereals_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));

		String[] header = {"project__PROJECT_NUMBER", "project__PROJECT_TITLE", 
				"project__source_url", "project__PROJECT_START_DATE",
				"project__PROJECT_END_DATE", "project__PROJECT_OBJECTIVE",
				"project__LAST_UPDATE", "project__DATE_ENTERED",  "agency_index__aid",
				"investigator_data__name", "investigator_data__ID", "institution_data__ID" , 
				"institution_data__INSTITUTION_NAME", "project__PROJECT_FUNDING"};
		csvout.printRecord(header);
		WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		for (String url1: urls) {
			HtmlPage startPage = webClient.getPage(url1);
			Document doc = Jsoup.parse(startPage.asXml());
			Set<Element> links = new HashSet<Element>();
			links.addAll(doc.select("section[class=block]").select("a"));
			for (Element link : links) {
				String project__PROJECT_NUMBER = "";
				String project__PROJECT_TITLE = "";
				String project__source_url = "";
				String project__PROJECT_START_DATE = "";
				String project__PROJECT_END_DATE = "";
				String project__PROJECT_OBJECTIVE = "";
				String project__LAST_UPDATE = "";
				String project__DATE_ENTERED = "";
				String project__PROJECT_FUNDING = "";
				String agency_index__aid = "146";
				String investigator_data__name = "";
				int institution_index__inst_id = -1;
				String investigator_data__ID = "-1";
				String comment = "";
				String institution_data__ID  = "-1";
				
				
				//Institution variables
				String institution_data__INSTITUTION_NAME = "";
				project__source_url = "http://cereals.ahdb.org.uk/" + link.attr("href");
				
				HtmlPage t = webClient.getPage(project__source_url);
				Document finaldoc = Jsoup.parse(t.asXml());
				
				try {
					project__PROJECT_NUMBER = finaldoc.select("dt:contains(project number) + dd").first().text();
					if (project__PROJECT_NUMBER != "RD-2011-3757")
						continue;
					project__PROJECT_TITLE = finaldoc.select("article").select("header").select("h2").first().text();
					project__PROJECT_START_DATE = finaldoc.select("dt:contains(Start date) + dd").first().text();
					project__PROJECT_END_DATE = finaldoc.select("dt:contains(End date) + dd").first().text();
					project__PROJECT_START_DATE = project__PROJECT_START_DATE.substring(project__PROJECT_START_DATE.length()-4);
					project__PROJECT_END_DATE = project__PROJECT_END_DATE.substring(project__PROJECT_END_DATE.length()-4);
					investigator_data__name = finaldoc.select("dt:contains(Lead partner) + dd").first().text();
					investigator_data__name = investigator_data__name.split(",")[0];
					investigator_data__name = investigator_data__name.replace("(ADAS)", "");
					investigator_data__name = investigator_data__name.replace("ADAS", "");
				}
				
				catch(Exception e) {;}
				if (project__PROJECT_END_DATE.contains("/")) {
					project__PROJECT_END_DATE =  project__PROJECT_END_DATE.substring(project__PROJECT_END_DATE.length()-2);
					project__PROJECT_END_DATE = "20" + project__PROJECT_END_DATE;
				}
				if (project__PROJECT_START_DATE.contains("/")) {
					project__PROJECT_START_DATE =  project__PROJECT_START_DATE.substring(project__PROJECT_START_DATE.length()-2);
					project__PROJECT_START_DATE = "20" + project__PROJECT_START_DATE;
				}
				try {
					institution_data__INSTITUTION_NAME = finaldoc.select("dt:contains(Lead partner) + dd").first().text().split(",")[1];
				}
				catch (Exception e) {
					institution_data__INSTITUTION_NAME = investigator_data__name;
					investigator_data__name = "";
				}
				if (investigator_data__name == "") {
					try {
						investigator_data__name = finaldoc.select("dt:contains(Lead scientist) + dd").first().text();
						if (institution_data__INSTITUTION_NAME == "") {
							try {
								
								institution_data__INSTITUTION_NAME = investigator_data__name.split(",")[1];
								investigator_data__name = investigator_data__name.split(",")[0];
							}
							catch(Exception e) {;}
							
						}
					}
					catch (Exception e) {;}
					
				}
				//Project number	
				String query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
				ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
				try {
					result.next();
					String number = result.getString(1);
				}
				catch (Exception ex) {
								
				
				project__PROJECT_FUNDING = finaldoc.select("dt:contains(funding) + dd").first().text().replace("\u00A3", "");
				project__PROJECT_FUNDING = project__PROJECT_FUNDING.split("\\(")[0];
				// Find institution
				String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
				ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
				try {
					rs.next();
					institution_data__ID = rs.getString(1);
				}
				catch (Exception e) {
					comment = "Please populate institution fields by exploring the institution named on the project.";
				}
				
				//Find investigator
				String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
				ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
				try {
					rs6.next();
					investigator_data__ID = rs6.getString(1);
				}
				catch (Exception e) {
					;
				}
				project__LAST_UPDATE = dateFormat.format(current);
				DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
				project__DATE_ENTERED = dateFormatEnter.format(current);
				project__PROJECT_OBJECTIVE =finaldoc.select("p").text(); 
				String[] output = {project__PROJECT_NUMBER, project__PROJECT_TITLE, 
						project__source_url, project__PROJECT_START_DATE,
						project__PROJECT_END_DATE, project__PROJECT_OBJECTIVE,
						project__LAST_UPDATE, project__DATE_ENTERED,  agency_index__aid,
						investigator_data__name, investigator_data__ID, institution_data__ID , 
						institution_data__INSTITUTION_NAME, project__PROJECT_FUNDING };
				csvout.printRecord(output);
				
			}
			}
		}
		csvout.close();
	}
	
	public static void pork(String outfolder, String url, String host, String user, String passwd, String dbname) throws Exception {
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);
		
		CSVPrinter csvout = new CSVPrinter(new FileWriter(outfolder+"AHDB_pork_"+currentStamp+".csv"), CSVFormat.EXCEL.withDelimiter(','));
		// There is no project number.
		String[] header = {"project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_OBJECTIVE", "project__LAST_UPDATE",
				"project__DATE_ENTERED", "institution_data__INSTITUTION_NAME",
				"institution_data__ID",
				"agency_index__aid","comment"};
		csvout.printRecord(header);
		
		WebClient webClient = new WebClient();
		HtmlPage startPage = webClient.getPage(url);
		Document doc = Jsoup.parse(startPage.asXml());
		Elements links = doc.select("div[id=innerNav]").select("li[class=current Page]").select("a");
		int i = 0;
		for (Element link: links) {
			String project__PROJECT_NUMBER = "";
			String project__PROJECT_TITLE = "";
			String project__source_url = "";
			String project__PROJECT_START_DATE = "";
			String project__PROJECT_END_DATE = "";
			String project__PROJECT_OBJECTIVE = "";
			String project__LAST_UPDATE = "";
			String project__DATE_ENTERED = "";
			String project__PROJECT_FUNDING = "";
			String agency_index__aid = "147";
			String investigator_data__name = "";
			int institution_index__inst_id = -1;
			String institution_data__INSTITUTION_NAME = "";
			int investigator_data__ID = -1;
			String comment = "";
			String institution_data__ID = "-1";
			

			//Project source URL
			if (i == 0) {
				i++;
				continue;
			}
			project__source_url = "http://pork.ahdb.org.uk/"+link.attr("href");

			Document finaldoc = Jsoup.connect(project__source_url).timeout(50000).get();
			project__PROJECT_TITLE = finaldoc.select("span[class=link active]").text();
			project__PROJECT_OBJECTIVE = finaldoc.select("article").text();
			Elements t = finaldoc.select("article").select("p, ul, li");
			String duration = t.select(":containsOwn(Duration)").text().replace("Duration:", "").trim();
			Pattern patDur = Pattern.compile("(\\d+).*?(\\d+)$");
			Matcher matchDur = patDur.matcher(duration);
			try {
				project__PROJECT_START_DATE = matchDur.group(1);
				project__PROJECT_END_DATE = matchDur.group(2);
			}
			catch(Exception e) {;}
			investigator_data__name = t.select(":containsOwn(AHDB Pork-funded studentship)").text();
			investigator_data__name = investigator_data__name.substring(investigator_data__name.indexOf("(")+1,investigator_data__name.indexOf(")"));
			try {
				institution_data__INSTITUTION_NAME = t.select(":containsOwn(Research partner)").text().split(":")[1].trim();
			}
			catch(Exception e) {;}
			String text = t.text().toLowerCase();
			try {
				project__PROJECT_OBJECTIVE = t.text().substring(text.toLowerCase().indexOf("aims and obj"), t.text().indexOf("Findings")-1);
				project__PROJECT_OBJECTIVE = project__PROJECT_OBJECTIVE.replace("Aims and objectives", "");
			}
			catch(Exception e) {;}
			
			// Get institution Id, if exists
			String GetInstIDsql = "SELECT ID FROM " + dbname + ".institution_data WHERE INSTITUTION_NAME = \"" +  institution_data__INSTITUTION_NAME + "\";";
			ResultSet rs = MysqlConnect.sqlQuery(GetInstIDsql,host,user,passwd);
			try {
				rs.next();
				institution_data__ID = rs.getString(1);
			}
			catch (Exception e) {
				comment = "Please populate institution fields by exploring the institution named on the project.";
			}
			
			// See if PI exists.
			String GetInvestigatorSQL = "SELECT ID FROM " + dbname + ".investigator_data WHERE NAME LIKE \"" +  investigator_data__name + "\" AND INSTITUTION =\"" + institution_data__ID + "\";";
			ResultSet rs6 = MysqlConnect.sqlQuery(GetInvestigatorSQL,host,user,passwd);
			try {
				rs6.next();
				investigator_data__ID = Integer.parseInt(rs6.getString(1));
			}
			catch (Exception e) {
				
			}
			project__LAST_UPDATE = dateFormat.format(current);
			DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
			project__DATE_ENTERED = dateFormatEnter.format(current);
			
			String[] output = {project__PROJECT_TITLE,
					project__source_url,
					project__PROJECT_START_DATE,project__PROJECT_END_DATE,
					project__PROJECT_OBJECTIVE, project__LAST_UPDATE,
					project__DATE_ENTERED, institution_data__INSTITUTION_NAME,
					institution_data__ID,
					agency_index__aid,comment};
			csvout.printRecord(output);
		}
		csvout.close();
		webClient.close();
	}
}
