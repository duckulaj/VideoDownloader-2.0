package com.hawkins.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletContext;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.http.MediaType;

import com.hawkins.dmanager.DownloadEntry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {

	private FileUtils() {

	}


	public static void copyToriginalFileName (DownloadEntry d) {

		try {
			Path copied = Paths.get(d.getFolder() + d.getFile());
			Path originalPath = Paths.get(d.getFolder() + d.getOriginalFileName());
			Files.copy(copied, originalPath, StandardCopyOption.REPLACE_EXISTING);

			Files.isSameFile(copied, originalPath);

			Files.deleteIfExists(copied);

		} catch (IOException ioe) {
			if (log.isDebugEnabled()) {
				log.debug(ioe.getMessage());
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
			while((br.readLine())!=null)
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
    
    public static boolean isAllowed(String fileName) {

		Optional<String> extension = Optional.ofNullable(fileName)
				.filter(f -> f.contains("."))
				.map(f -> f.substring(fileName.lastIndexOf(".") + 1));		

		return Arrays.asList(Constants.allowedExtensions).contains(extension.get());

	}
	
	public static List<String> getFiles(String dir) throws IOException {
		
		log.info("Retrieving sorted list of files from {}", dir);
		
	    List<String> fileList = new LinkedList<String>();
	    try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
	        for (Path path : stream) {
	            if (!Files.isDirectory(path) && FileUtils.isAllowed(path.toString())) {
	                fileList.add(path.getFileName()
	                    .toString());
	            }
	        }
	        if (!fileList.isEmpty()) {
	        	Collections.sort(fileList);
	        }
	    }
	    
	    
	    return fileList;
	}
}
