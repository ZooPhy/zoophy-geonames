package edu.asu.zoophy.geonames.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import edu.asu.zoophy.geonames.indexer.geotree.Adm;
import edu.asu.zoophy.geonames.indexer.geotree.Country;
import edu.asu.zoophy.geonames.indexer.geotree.GeoNameLocation;
import edu.asu.zoophy.geonames.indexer.geotree.GeoNamesTree;


public class Indexer {

	private static Logger log = Logger.getLogger("Indexer");
	static String geonameResourcesDir = null;
	static String luceneIndexDir = null;
	static Set<String> featClassExcl = null;
	static Set<String> featCodeExcl = null;
	static Set<String> featCodeIncl = null;
	static Set<String> geonameIdsExcl = null;
	static String geoAllCountriesFile = null;
	static GeoNamesTree geoTree = null;
	static LuceneWriter luceneWriter = null;

	public static void createIndex() {
		loadProperties();
		geoTree = GeoNamesTree.getInstance(geonameResourcesDir);
		luceneWriter =  new LuceneWriter(luceneIndexDir);
		geoAllCountriesFile = geonameResourcesDir + "allCountries.txt";
		loadAllCountries();
		luceneWriter.exitWriter();
	}
	
	private static void loadProperties() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream("config/application.properties");
			prop.load(input);
			geonameResourcesDir = prop.getProperty("geonames.files.location");
			luceneIndexDir = prop.getProperty("lucene.index.location");
			String featClassExclProp = prop.getProperty("geonames.feature_class.exclude");
			String featCodeInclProp = prop.getProperty("geonames.feature_code.include");
			String featCodeExclProp = prop.getProperty("geonames.feature_code.exclude");
			String geonameIdsExclProp = prop.getProperty("geonames.geonameids.exclude");
			featClassExcl = getPropAsSet(featClassExclProp);
			featCodeIncl = getPropAsSet(featCodeInclProp);
			featCodeExcl = getPropAsSet(featCodeExclProp);
			geonameIdsExcl = getPropAsSet(geonameIdsExclProp);
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

	private static Set<String> getPropAsSet(String propvalue) {
		Set<String> codes = new HashSet<String>();
		for(String class_code: propvalue.split(",")){
			codes.add(class_code);
		}
		return codes;
	}

	private static void loadAllCountries() {
		int count = 0, increments = 500000, limit = -1;
		try {
			File geoFile = new File(geoAllCountriesFile);
			Scanner scan = new Scanner(geoFile);
			while (scan.hasNext()) {
				String record = scan.nextLine().trim();
				if(record.split("\t").length==19){
					String geonameId = record.split("\t")[0];
					String typeClass = record.split("\t")[6];
					String typeCode = record.split("\t")[7];
					// Do not process if not necessary
					if((featClassExcl.contains(typeClass) && !featCodeIncl.contains(typeCode))
							|| featCodeExcl.contains(typeCode) || geonameIdsExcl.contains(geonameId)){
						continue;
					}
					GeoNameLocation geoNameLoc = getGeoNameLocation(record);
					if(geoNameLoc != null){
						luceneWriter.indexRecord(geoNameLoc);
						count++;
						if(count % increments == 0) {
							log.info("allCountries.txt count: "+ count);
						}
					}
					if(limit > 0 && count >= limit) {
						break;
					}
				} else {
					log.info("------error: String size too long");
					log.info("length: "+ record.split("\t").length +" String: "+ record.substring(0, 10) );
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		log.info("----------AllCountries.txt completed, count: "+ count);
	}

	private static GeoNameLocation getGeoNameLocation(String record) {
		GeoNameLocation geoNameLoc = null;
		try{
			String[] geoname = record.split("\t"); 
			int id = Integer.parseInt(geoname[0]);
			String name = geoname[1];
			String asciiname = geoname[2];
			// We'll use specific alternate names
			// Set<String> alternatenames = new HashSet<String>(Arrays.asList(geoname[3].split(",")));
			Double latitude = Double.parseDouble(geoname[4]);
			Double longitude = Double.parseDouble(geoname[5]);
			String typeClass = geoname[6];
			String typeCode = geoname[7];
			String countrycode = geoname[8];
			String adm1 = geoname[10];
			String adm2 = geoname[11]; 
			String population = geoname[14];

			//Load specific alternate names i.e. english, abbrv etc.
			Set<String> alternatenames = null;
			if(geoTree.getAltNamesLookup().containsKey(id)){
				alternatenames = geoTree.getAltNamesLookup().get(id);
			} else {
				alternatenames = new HashSet<String>();
			}
			
			//Some continents don't have population, so better calculate them
			if (typeCode.equals("CONT") && population.equals("0")){
				String[] countries = geoname[9].split(",");
				int totalPop = 0;
				for (String ccode : countries) {
					if(geoTree.getCountryLookup().containsKey(ccode)){
						Country country = geoTree.getCountryLookup().get(ccode);
						totalPop +=  country.getPopulation();
					}
				}
				population = Integer.toString(totalPop);
			}
			
			geoNameLoc = new GeoNameLocation(id, name, asciiname, alternatenames,
					latitude, longitude, typeClass, typeCode,
					countrycode, population);

			//Get country
			if(geoTree.getCountryLookup().containsKey(countrycode)){
				Country country = geoTree.getCountryLookup().get(countrycode);
				geoNameLoc.setCountry(country);
			}

			//Get state
			if (!adm1.trim().isEmpty()){
				String statecode = countrycode + "." + adm1; 
				if(geoTree.getAdm1Lookup().containsKey(statecode)){
					Adm state = geoTree.getAdm1Lookup().get(statecode);
					geoNameLoc.setState(state);
				}
			}

			//Get county
			if (!adm1.trim().isEmpty()  && !adm2.trim().isEmpty()){
				String countycode = countrycode + "." + adm1 + "." + adm2; 
				if(geoTree.getAdm2Lookup().containsKey(countycode)){
					Adm county = geoTree.getAdm2Lookup().get(countycode);
					geoNameLoc.setCounty(county);
				}
			}

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return geoNameLoc;
	}

}
