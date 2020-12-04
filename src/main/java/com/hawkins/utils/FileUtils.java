package com.hawkins.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.servlet.ServletContext;

import org.apache.commons.io.LineIterator;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

import com.hawkins.dmanager.DownloadEntry;

public class FileUtils {

	private FileUtils() {

	}

	private static final Logger logger = LogManager.getLogger(FileUtils.class.getName());

	public static void copyToriginalFileName (DownloadEntry d) {

		try {
			Path copied = Paths.get(d.getFolder() + d.getFile());
			Path originalPath = Paths.get(d.getFolder() + d.getOriginalFileName());
			Files.copy(copied, originalPath, StandardCopyOption.REPLACE_EXISTING);

			Files.isSameFile(copied, originalPath);

			Files.deleteIfExists(copied);

		} catch (IOException ioe) {
			if (logger.isDebugEnabled()) {
				logger.debug(ioe.getMessage());
			}
		}


	}

	public static String fileTail (String filename, int linecount) {

		StringBuffer sb = new StringBuffer();
		String lineSeperator = System.getProperty("line.separator");

		File file = new File(filename);
		int counter = 0; 

		int linesInFile = numberOfLines(file);

		// Can't read more lines than exist in the file
		
		if (linecount > linesInFile) {
			linecount = linesInFile;
		}

		try {
			ReversedLinesFileReader object = new ReversedLinesFileReader(file, Charset.forName("UTF-8"));

			while(counter < linecount) {
				sb.append(object.readLine());
				sb.append(lineSeperator);
				counter++;
			}

			object.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return sb.toString();

	}

	public static int numberOfLines (File thisFile) {

		int lineCount = 0;

		FileReader fileReader;
		
		try {
			fileReader = new FileReader(thisFile);
			
			BufferedReader br = new BufferedReader(fileReader);
			String s;              
			while((s=br.readLine())!=null)
			{
				lineCount++; 

			}
			fileReader.close();		

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		return lineCount;
	}
	
	// abc.zip
    // abc.pdf,..
    public static MediaType getMediaTypeForFileName(ServletContext servletContext, String fileName) {
        // application/pdf
        // application/xml
        // image/gif, ...
        String mineType = servletContext.getMimeType(fileName);
        try {
            MediaType mediaType = MediaType.parseMediaType(mineType);
            return mediaType;
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
