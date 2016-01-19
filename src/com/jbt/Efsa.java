package com.jbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.opencsv.CSVWriter;

public class Efsa {

	public static String main(String url, String outfolder, String host, String user, String passwd, String dbname, String logfile) throws IOException {
		String pat = "en/tender.*?/tender/|en/node/915681";
		Efsa.scrape(url,pat,outfolder,host,user,passwd,dbname,logfile);
		return "EFSA";
	}
	
	public static void scrape(String url, String pat, String outfolder, String host, String user, String passwd, String dbname, String logfile) throws IOException {
		//Get current date to assign filename
		Date current = new Date();
		DateFormat dateFormatCurrent = new SimpleDateFormat("yyyyMMdd");
		String currentStamp = dateFormatCurrent.format(current);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateLog = dateFormat.format(current);
		
		CSVWriter csvout = new CSVWriter(new FileWriter(outfolder+"EFSA_"+currentStamp+".csv"),'\t');
		String[] header = {"project__PROJECT_NUMBER","project__PROJECT_TITLE",
				"project__source_url",
				"project__PROJECT_START_DATE","project__PROJECT_END_DATE",
				"project__PROJECT_MORE_INFO","project__PROJECT_OBJECTIVE",
				"project__PROJECT_ABSTRACT","project__LAST_UPDATE",
				"project__DATE_ENTERED", "project__PROJECT_FUNDING",
				"institution_data__INSTITUTION_NAME",
				"institution_data__INSTITUTION_ADDRESS1","institution_data__INSTITUTION_CITY",
				"institution_data__INSTITUTION_STATE","institution_data__INSTITUTION_COUNTRY",
				"institution_data__INSTITUTION_ZIP",
				"institution_index__inst_id",
				"agency_index__aid","comment"};
		csvout.writeNext(header);
		
		Document doc = Jsoup.connect(url)
				.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
			      .referrer("http://www.google.com").timeout(1000).get();
		String sizeLinks = doc.select("div:containsOwn(Results 1)").text(); 
		Pattern searchSize = Pattern.compile("(\\d+)$");
		Matcher matchSize = searchSize.matcher(sizeLinks);
		int numPages = 0;
		while (matchSize.find()) {
			numPages = Integer.valueOf(matchSize.group(1))/20;
		}
		
		if (numPages == 0) {
			//Log an error that it didn't work
		} else {
		
			for (int i=0;i<=numPages;i++) {
				Document listTenders = Jsoup.connect(url+"&page="+String.valueOf(i))
						.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
    				      .referrer("http://www.google.com").timeout(1000).get();
				Elements links = listTenders.select("a[href]");
				Pattern pattern = 
			            Pattern.compile(pat);
				//Check whether all links are being captured given pattern - should be 20 per page except for very last
				int checkNums = 0;
				
				for (Element link : links) {
					Matcher matcher = 
				            pattern.matcher(link.attr("href"));
					if (matcher.find()) {
						checkNums++;
						Document linkdoc = Jsoup.connect(link.attr("abs:href"))
								.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
	          				      .referrer("http://www.google.com").timeout(1000).get();
						Elements furtherLink = linkdoc.select("a:containsOwn(award notice)");
						Element infoLink = null;
						if (furtherLink.size()!=0) {
							infoLink = furtherLink.last();
						} else {
							try {
								furtherLink = linkdoc.select("a:containsOwn(Bekanntmachung)");
								infoLink = furtherLink.last();
							}
							catch (Exception ex) {
								//Log that there is simply no link there
							}
						}
						if (infoLink != null) {
							try {
								//Get to next phase
								Document finaldoc = Jsoup.connect(infoLink.attr("abs:href"))
										.userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
			          				      .referrer("http://www.google.com").timeout(1000).get();
								Element content = finaldoc.getElementById("fullDocument");
								
								//Declare needed strings
								String project__PROJECT_NUMBER = null;
								String project__PROJECT_TITLE = null;
								String project__source_url = null;
								String project__PROJECT_START_DATE = null;
								String project__PROJECT_END_DATE = null;
								String project__PROJECT_MORE_INFO = null;
								String project__PROJECT_OBJECTIVE = null;
								String project__PROJECT_ABSTRACT = null;
								String project__LAST_UPDATE = null;
								String project__DATE_ENTERED = null;
								String project__PROJECT_FUNDING = null;
								String agency_index__aid = "122";
								int institution_index__inst_id = -1;
								String comment = "";
								
								//Institution variables
								String institution_data__INSTITUTION_NAME = null;
								String institution_data__INSTITUTION_ADDRESS1 = null;
								String institution_data__INSTITUTION_CITY = null;
								String institution_data__INSTITUTION_COUNTRY = null;
								String institution_data__INSTITUTION_ZIP = null;
								String institution_data__INSTITUTION_STATE = null;
								
								//Processing variables
								String instInfo = null;
								String query = null;
								
								//Project source URL
								project__source_url = infoLink.attr("abs:href");
								
								//Project number
								Element numElem = content.select("span:containsOwn(File reference number attributed)").first();
								Pattern badSymb = Pattern.compile("^[\\W_]+");
								project__PROJECT_NUMBER = numElem.nextElementSibling().text();
								Matcher matchSymb = badSymb.matcher(project__PROJECT_NUMBER);
								project__PROJECT_NUMBER = matchSymb.replaceAll("");
								Pattern patNum = Pattern.compile("^(.*?)\\s");
								Matcher matchNum = patNum.matcher(project__PROJECT_NUMBER);
								if (matchNum.find()) {
									project__PROJECT_NUMBER = matchNum.group(1);
								} else {
									Pattern badSymbEnd = Pattern.compile("\\.$");
									Matcher matchSymbEnd = badSymbEnd.matcher(project__PROJECT_NUMBER);
									project__PROJECT_NUMBER = matchSymbEnd.replaceAll("");
								}
								
								query = "SELECT PROJECT_NUMBER FROM "+dbname+".project where PROJECT_NUMBER = \""+project__PROJECT_NUMBER+"\"";
								ResultSet result = MysqlConnect.sqlQuery(query,host,user,passwd);
								try {
									result.next();
									String number = result.getString(1);
								}
								catch (Exception ex) {
									//Title
									Element titleElem = content.select("span:containsOwn(Title attributed to)").first();
									project__PROJECT_TITLE = titleElem.nextElementSibling().text();
									project__PROJECT_TITLE = project__PROJECT_TITLE.replace(project__PROJECT_NUMBER,"");
									matchSymb = badSymb.matcher(project__PROJECT_TITLE);
									project__PROJECT_TITLE = matchSymb.replaceAll("");
									
									//Project abstract
									Element abstElem = content.select("span:containsOwn(Short description of)").first();
									project__PROJECT_OBJECTIVE = abstElem.nextElementSibling().text();
									matchSymb = badSymb.matcher(project__PROJECT_OBJECTIVE);
									project__PROJECT_OBJECTIVE = matchSymb.replaceAll("");
									
									//Project funding
									Element fundElem = content.select("span:containsOwn(Total final value)").last().nextElementSibling();
									String funding = fundElem.text();
									Pattern patFund = Pattern.compile("Value: (.*?) EUR");
									Matcher matchFund = patFund.matcher(funding);
									while (matchFund.find()) {
										project__PROJECT_FUNDING = matchFund.group(1).replace(" ","");
									}
									
									//Date stamp
									project__LAST_UPDATE = dateFormat.format(current);
									DateFormat dateFormatEnter = new SimpleDateFormat("yyyy-MM-dd");
									project__DATE_ENTERED = dateFormatEnter.format(current);
									
									//Project start date
									Element dateElem = content.select("span:containsOwn(Date of contract award)").first().nextElementSibling();
									String startDate = dateElem.text();
									Pattern patDate = Pattern.compile("(\\d+)$");
									Matcher matchDate = patDate.matcher(startDate);
									while (matchDate.find()) {
										project__PROJECT_START_DATE = matchDate.group(1);
									}
									
									//Institution info - can be several if multiple contracts awarded under one tender
									Elements instElems = content.select("span:containsOwn(Name and address of economic operator)");
									for (Element instElem : instElems) {
										institution_data__INSTITUTION_NAME = null;
										institution_data__INSTITUTION_ADDRESS1 = null;
										institution_data__INSTITUTION_CITY = null;
										institution_data__INSTITUTION_COUNTRY = null;
										institution_data__INSTITUTION_ZIP = null;
										institution_data__INSTITUTION_STATE = null;
										institution_index__inst_id = -1;
										
										Element instContainer = instElem.nextElementSibling();
										instInfo = StringEscapeUtils.unescapeHtml4(instContainer.children().select("p").html().toString());
										List<String> matches = new ArrayList<String>();
										for (String instInfoElem : instInfo.split("<br>")) {
											matches.add(instInfoElem);
										}
										
										String[] allMatches = new String[matches.size()];
										allMatches = matches.toArray(allMatches);
										institution_data__INSTITUTION_NAME = allMatches[0];
										
										//Check institution in MySQL DB
										query = "SELECT * from "+dbname+".institution_data where institution_name like \""+institution_data__INSTITUTION_NAME+"\"";
										result = MysqlConnect.sqlQuery(query,host,user,passwd);
										try {
											result.next();
											institution_index__inst_id = result.getInt(1);
										}
										catch (Exception e) {
											institution_data__INSTITUTION_ADDRESS1 = allMatches[1];
											int instCountryIndex = 3;
											if (matches.size() == 3) {
												instCountryIndex = 2;
												Pattern patAddr = Pattern.compile("^(.*?)[A-Z][a-z][a-z\\-]+.*?,\\s([A-Z][a-z][a-z\\-]+)");
												Matcher matchAddr = patAddr.matcher(allMatches[2]);
												Pattern trailSpace = Pattern.compile("\\s+$");
												if (matchAddr.find()) {
													institution_data__INSTITUTION_ZIP = matchAddr.group(1);
													Matcher matchSpace = trailSpace.matcher(institution_data__INSTITUTION_ZIP);
													institution_data__INSTITUTION_ZIP = matchSpace.replaceAll("");
													institution_data__INSTITUTION_CITY = matchAddr.group(2);
												}
											} else {
												Pattern patAddr = Pattern.compile("^(.*?)([A-Z][a-z][A-Za-z\\-]+)");
												Matcher matchAddr = patAddr.matcher(allMatches[2]);
												Pattern trailSpace = Pattern.compile("\\s+$");
												if (matchAddr.find()) {
													institution_data__INSTITUTION_ZIP = matchAddr.group(1);
													Matcher matchSpace = trailSpace.matcher(institution_data__INSTITUTION_ZIP);
													institution_data__INSTITUTION_ZIP = matchSpace.replaceAll("");
													institution_data__INSTITUTION_CITY = matchAddr.group(2);
												}
											}
											institution_data__INSTITUTION_COUNTRY = WordUtils.capitalizeFully(allMatches[instCountryIndex]);
											query = "SELECT * FROM "+dbname+".countries WHERE COUNTRY_NAME = \""
													+institution_data__INSTITUTION_COUNTRY+"\"";
											result = MysqlConnect.sqlQuery(query,host,user,passwd);
											try {
												result.next();
												institution_data__INSTITUTION_COUNTRY = result.getString(1);
											}
											catch (Exception exc) {
												// Country does not exist in DB --> comment: "Check country field"
												comment = "Please check the country name and respective index in the DB - might be a spelling mistake or new country.";
												
											}
											if (Integer.valueOf(institution_data__INSTITUTION_COUNTRY) == 1) {
												try {
													query = "SELECT abbrv FROM "+dbname+".states";
													result = MysqlConnect.sqlQuery(query,host,user,passwd);
													while (result.next()) {
														String state = result.getString(1);
														Pattern patState = Pattern.compile("("+state+")");
														Matcher matchState = patState.matcher(allMatches[2]);
														if (matchState.find()) {
															institution_data__INSTITUTION_STATE = state;
															institution_data__INSTITUTION_ZIP = institution_data__INSTITUTION_ZIP.replace(state,"");
															break;
														}
														
													}
													if (institution_data__INSTITUTION_STATE == null) {
														//Add to comment field rather than just have it there re-write other comments
														comment = "Please check the address information on project_source_url to see whether state field is present.";
													}
												}
												catch (Exception exc) {
													
												}
												
											}
									
										}
										
										//Write resultant values into CSV
										String[] output = {project__PROJECT_NUMBER,project__PROJECT_TITLE,project__source_url,
												project__PROJECT_START_DATE,project__PROJECT_END_DATE,
												project__PROJECT_MORE_INFO,project__PROJECT_OBJECTIVE,
												project__PROJECT_ABSTRACT,project__LAST_UPDATE,
												project__DATE_ENTERED,project__PROJECT_FUNDING,
												institution_data__INSTITUTION_NAME,
												institution_data__INSTITUTION_ADDRESS1, institution_data__INSTITUTION_CITY,
												institution_data__INSTITUTION_STATE, institution_data__INSTITUTION_COUNTRY,
												institution_data__INSTITUTION_ZIP,
												String.valueOf(institution_index__inst_id),
												agency_index__aid,comment};
										
											csvout.writeNext(output);
												
									}
									
									
								}
							}
							catch (Exception ee) {
								//Log exception that page does not exist and link is broken
								try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(logfile, true)))) {
									StringWriter errors = new StringWriter();
									ee.printStackTrace(new PrintWriter(errors));
									out.println(currentDateLog
								    			+"   "
								    			+"Perhaps the link is broken or does not exist - "+infoLink.attr("abs:href")+" ."
								    			+" Here is some help with traceback:"
								    			+errors.toString());
								}catch (IOException e) {

								}
								
							}
							
							
							
							
						} else {
							//Handle if there is no contract award notice yet; perhaps just pass
						}
						
					}
						
				}
				if (i!=numPages && checkNums != 20) {
					//Log error that some links were missed and they have to double check somehow
				}
				
			}
		}
		
		csvout.close();
		
	}
}
