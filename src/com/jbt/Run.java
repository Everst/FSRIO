package com.jbt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;


public class Run {
	
	public static void main(String[] args) throws IOException,SAXException,ParserConfigurationException,Exception {
		Properties prop = new Properties();
		InputStream in = new FileInputStream(new File(args[0]));
		prop.load(in);
		in.close();
		String outfolder = prop.getProperty("OUTPUT_FOLDER");
		
		//Get MySQL credentials
		String host = prop.getProperty("MYSQL_HOST");
		String user = prop.getProperty("MYSQL_USERNAME");
		String passwd = args[1];
		String dbname = prop.getProperty("MYSQL_DBNAME");
		String logfile = prop.getProperty("LOG_FILE");
		
		
		String[] dataSources = {"CampdenBri","Defra","Efsa","Esrc","Fsa","Fspb","NIH","NSF","Omafra","Relu","AHDB"};
		String[] sources = prop.getProperty("SOURCES").split(",");
		if (!sources[0].equals("all")) {
			dataSources = sources;
		}
		
		int len = dataSources.length;
		for (String source : dataSources) {
			if (source.equals("CampdenBri")) {
				CampdenBri.main(outfolder,prop.getProperty("CAMPDENBRI_MAIN_LINKS").split(","),
						prop.getProperty("CAMPDENBRI_ADD_LINKS").split(","),host,user,passwd,dbname);
				len = len-1;
				System.out.println("CampdenBRI website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Defra")) {
				Defra.main(prop.getProperty("DEFRA_MAINPAGE_URL"),outfolder,host,user,passwd,dbname);
				len = len-1;
				System.out.println("DEFRA website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Efsa")) {
				Efsa.main(prop.getProperty("EFSA_MAINPAGE_URL"),outfolder,host,user,passwd,dbname,logfile);
				len = len-1;
				System.out.println("EFSA website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Esrc")) {
				Esrc.main(prop.getProperty("ESRC_MAINPAGE_URL"),outfolder,host,user,passwd,dbname);
				len = len-1;
				System.out.println("ESRC website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Fsa")) {
				Fsa.main(prop.getProperty("FSA_LINKS").split(","),outfolder,host,user,passwd,dbname,logfile);
				len = len-1;
				System.out.println("FSA website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Fspb")) {
				Fspb.main(prop.getProperty("FSPB_MAINPAGE_URL"),outfolder,host,user,passwd,dbname,logfile);
				len = len-1;
				System.out.println("FSPB website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("NIH")) {
				NIH.main(prop.getProperty("INPUT_FOLDER_NIH"),prop.getProperty("INPUT_FOLDER_NIH_ABSTRACTS"),outfolder,host,user,passwd,dbname);
				len = len-1;
				System.out.println("NIH award files parsed successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("NSF")) {
				NSF.main(prop.getProperty("INPUT_FOLDER_NSF"),outfolder,host,user,passwd,dbname);
				len = len-1;
				System.out.println("NSF award files parsed successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Omafra")) {
				Omafra.main(prop.getProperty("OMAFRA_MAINPAGE_URL"),outfolder,host,user,passwd,dbname);
				len = len-1;
				System.out.println("OMAFRA website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("Relu")) {
				Relu.main(prop.getProperty("RELU_LINKS").split(","),outfolder,host,user,passwd,dbname,logfile);
				len = len-1;
				System.out.println("RELU website scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			if (source.equals("AHDB")) {
				AHDB.main(outfolder,prop.getProperty("AHDB_LINKS").split(","),host,user,passwd,dbname,logfile);
				len = len-1;
				System.out.println("AHDB websites scraped successfully... "+len+" source(s) left to scrape/parse.");
			}
			
		}
		
		
	}

}
