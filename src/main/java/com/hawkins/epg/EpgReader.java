package com.hawkins.epg;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import com.hawkins.properties.DownloadProperties;

public class EpgReader {
	
	public static DownloadProperties properties = DownloadProperties.getInstance();
	private static Document document;
	private static String szEnd = "";
	private static String szStart = "";
	
	private static Date startTime = null;
	private static Date endTime = null;
	
	public EpgReader() {
		super();
	}
	
	public void changeLocalTime() {
		changeLocalTime(properties.getEpgFileName());
	}

	public static void changeLocalTime(String fileName) {
		
		

		SimpleDateFormat xmlDateFormat = new SimpleDateFormat("yyyyMMddHHmmss Z");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		SAXReader reader = new SAXReader();

		try {
			document = reader.read(fileName);

			Element rootElement = document.getRootElement();

			Iterator itProgramme = rootElement.elementIterator("programme");

			while (itProgramme.hasNext() ) {
				Element pgmElement = (Element) itProgramme.next();

				szStart = pgmElement.attribute("start").getStringValue();
				szEnd = pgmElement.attribute("stop").getStringValue();
			
				String szNewStart = szStart.replace("+0000", "+0200");
				String szNewEnd = szEnd.replace("+0000", "+0200");
				
				pgmElement.attribute("start").setText(szNewStart);
				pgmElement.attribute("stop").setText(szNewEnd);
			}
			
			OutputFormat format = OutputFormat.createPrettyPrint();
			XMLWriter writer;
			
			String outputFile = properties.getDownloadPath() + "/xteveNew.xml";
			writer = new XMLWriter(new BufferedOutputStream(new FileOutputStream(outputFile)), format);
			// writer = new XMLWriter(System.out, format);
			writer.write(document);
			writer.close();
			

		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static String formatStr(String instr) {
		instr = instr.replaceAll("'","''");
		/*instr = instr.replaceAll("'","'");
        instr = instr.replaceAll("\"",""");
        instr = instr.replaceAll(">",">");
        instr = instr.replaceAll("<","<");*/
		return instr;
	}
}
