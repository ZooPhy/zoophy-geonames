package edu.asu.zoophy.geonames.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Downloader {

    private static Logger log = Logger.getLogger("Downloader");

	private static String geonamesBaseURL = null;
	private static String geonamesResourcesDir = null;

	private static final String geoAllCountriesZipFile = "allCountries.zip";
	private static final String geoAllCountriesFile = "allCountries.txt";
	private static final String GeoAltNamesZipFile = "alternateNamesV2.zip";
	private static final String GeoAltNamesFile = "alternateNamesV2.txt";
	private static final String GeoCountryFile = "countryInfo.txt";
	private static final String GeoADM1File = "admin1CodesASCII.txt";
	private static final String GeoADM2File = "admin2Codes.txt";

    public static void downloadGeonamesFiles() {
		loadProperties();
		log.info("Downloading Geonames files...");
        // download AllCountries.zip file and extract it
        downloadFile(geoAllCountriesZipFile);
		extractFile(geoAllCountriesZipFile, geoAllCountriesFile);
        // download alternateNamesV2.zip file and extract it
        downloadFile(GeoAltNamesZipFile);
		extractFile(GeoAltNamesZipFile, GeoAltNamesFile);

        downloadFile(GeoCountryFile);
		downloadFile(GeoADM1File);
		downloadFile(GeoADM2File);
		log.info("Finished Downloading Geonames files");
	}

    private static void loadProperties() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("config/application.properties");
			prop.load(input);
			geonamesBaseURL = prop.getProperty("geonames.download.url");
			geonamesResourcesDir = prop.getProperty("geonames.files.location");
        } catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

    private static void extractFile(String zipFile, String destFile) {
		log.info("Extracting " + destFile + " from " + zipFile);
		BufferedOutputStream out;
		ZipInputStream zin;
		String zipFilePath = geonamesResourcesDir + zipFile;
		ZipEntry entry;
	    String tempName;
		try {
			zin = new ZipInputStream(new FileInputStream(zipFilePath));
		    while ((entry = zin.getNextEntry()) != null) {
		    	tempName = entry.getName(); 
		    	if (destFile.equalsIgnoreCase(tempName)) {
		    		break;
		    	}
		    }
			byte[] buffer = new byte[4096];
		    out = new BufferedOutputStream(new FileOutputStream(new File(geonamesResourcesDir, destFile)));
		    int count = -1;
		    while ((count = zin.read(buffer)) != -1) {
		    	out.write(buffer, 0, count);
		    }
		    out.close();
			zin.close();
			log.info("Finished Extracting " + destFile);
			log.info("Deleting " + zipFilePath);
			Path path = Paths.get(zipFilePath);
			Files.delete(path);
			log.info("Deleted " + zipFilePath);
		}
		catch (Exception e) {
			log.warning( "Error extracting " + zipFilePath + ": " + e.getMessage());
		}
	}
	
	private static void downloadFile(String filename) {
		String gUrl = geonamesBaseURL + filename;
		log.info("Downloading " + filename);
		InputStream in = null;
	    FileOutputStream fout = null;
	    try {
	    	URL url = new URL(gUrl);
	        in = new BufferedInputStream(url.openStream());
	        fout = new FileOutputStream(geonamesResourcesDir + filename);
	        final byte data[] = new byte[1024];
	        int count;
	        while ((count = in.read(data)) != -1) {
	            fout.write(data, 0, count);
	        }
	        fout.flush();
	        in.close();
	        fout.close();
	        log.info("Finished Downloading" + filename);
	    }
	    catch (IOException e) {
	    	log.warning( "IOException when downloading " + filename + ": " + e.getMessage());
		}
	}
   
}