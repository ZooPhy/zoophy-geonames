package edu.asu.zoophy.geonames.indexer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import edu.asu.zoophy.geonames.indexer.geotree.GeoNameLocation;


public class LuceneWriter {
	public static final List<String> stops = Arrays.asList("and", "of", "the", "state", "province", "county", "area", "region", "prefecture"); 
	public static final CharArraySet stopWordsOverride = new CharArraySet(stops, true);
	// If you don't want to use stop words, use the following line instead
	// CharArraySet stopWordsOverride = new CharArraySet(Collections.emptySet(), true);

	private static IndexWriter writer = null;
	private static final Logger log = Logger.getLogger("writeToLucene");
	
	public LuceneWriter(String pathToIndex) {
		log.info("Creating Lucene Indexer at '" + pathToIndex + "'");
		setupWriter(pathToIndex);
	}

	private void setupWriter(String pathToIndex) {
		try {
			Directory dir = FSDirectory.open(Paths.get(pathToIndex));
			Analyzer analyzer = new StandardAnalyzer(stopWordsOverride);
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(OpenMode.CREATE);
			writer = new IndexWriter(dir, iwc);
		} catch (Exception e){
			e.printStackTrace();
			log.info("error: "+e);
			System.exit(1);
		}
	}

	public void exitWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.info("error: "+e);
		}
	}

	public void indexRecords(List<GeoNameLocation> geoNameLocs) {
		int count = 0,increments = 500000 ;
		for(GeoNameLocation geoNameLoc : geoNameLocs) {
			indexRecord(geoNameLoc);
			if(count++ % increments == 0) {
				log.info("Lucene processed records: "+ count);
			}
		}
		log.info("----Lucene process completed, records: "+ count + "/" + geoNameLocs.size());
	}

	public void indexRecord(GeoNameLocation geoNameLoc) {
		try {
			// Main document object for indexing
			Document doc = new Document();
			boolean print = false;
			
			StringBuilder ancestorsNames = new StringBuilder();
			StringBuilder ancestorsIds = new StringBuilder();
			
			//Normalize names and formats for indexing
			String id = String.valueOf(geoNameLoc.getId());
			doc.add(new StringField("GeonameId", id, Field.Store.YES));

			String name = geoNameLoc.getName();
			Set<String> alternateNames = geoNameLoc.getAlternatenames();
			alternateNames.add(geoNameLoc.getAsciiname());
			alternateNames.remove(name);
			
			String typeClass = String.valueOf(geoNameLoc.getTypeClass());
			doc.add(new StringField("Class", typeClass, Field.Store.YES));

			String typeCode = String.valueOf(geoNameLoc.getTypeCode());
			doc.add(new StringField("Code", typeCode, Field.Store.YES));
			
			Long population = Long.parseLong(geoNameLoc.getPopulation());
			doc.add(new NumericDocValuesField("Population", population));
			doc.add(new StoredField("Population", population));
			
			String latitude = String.valueOf(geoNameLoc.getLatitude());
			doc.add(new StringField("Latitude", latitude, Field.Store.YES));

			String longitude = String.valueOf(geoNameLoc.getLongitude());
			doc.add(new StringField("Longitude", longitude, Field.Store.YES));
			
			//Add county if available
			if(geoNameLoc.getCounty() != null){
				String adm = String.valueOf(geoNameLoc.getCounty().getName());
				Set<String> countyAltNames = geoNameLoc.getCounty().getAlternatenames();
				String admId = String.valueOf(geoNameLoc.getCounty().getId());
				adm = getAlternateNamesStr(admId, adm, countyAltNames);
				doc.add(new TextField("County", adm, Field.Store.YES));
				doc.add(new StringField("ADM2", admId, Field.Store.YES));
				ancestorsNames.append(adm + ", ");
				ancestorsIds.append(admId + ", ");
			}
			
			//Add state if available
			if(geoNameLoc.getState() != null){
				String adm = geoNameLoc.getState().getName();
				Set<String> stateAltNames = geoNameLoc.getState().getAlternatenames();
				stateAltNames.add(adm);
				String stateCode = geoNameLoc.getState().getCode().split("\\.")[1];
				if(stateCode.matches("[A-Z]{2,5}")){
					stateAltNames.add(stateCode);
				}
				if (typeCode.equalsIgnoreCase("ADM1")){
					alternateNames.addAll(stateAltNames);
				}
				//Add field with alt names
				adm = getAlternateNamesStr(id, adm, stateAltNames);
				String admId = String.valueOf(geoNameLoc.getState().getId());
				doc.add(new TextField("State", adm, Field.Store.YES));
				doc.add(new StringField("ADM1", admId, Field.Store.YES));
				if (!typeCode.equalsIgnoreCase("ADM1")){
					ancestorsNames.append(adm + ", ");
					ancestorsIds.append(admId + ", ");
				}
			}
			
			//Add country if not a continent itself
			if(geoNameLoc.getCountry() != null){
				String country = String.valueOf(geoNameLoc.getCountry().getName());
				Set<String> countryAltNames = geoNameLoc.getCountry().getAlternatenames();
				countryAltNames.add(country);
				countryAltNames.add(geoNameLoc.getCountry().getIso());
				countryAltNames.add(geoNameLoc.getCountry().getIso3());
				if (typeCode.equalsIgnoreCase("PCLI")){
					alternateNames.addAll(countryAltNames);
				}
				doc.add(new TextField("Country", country, Field.Store.YES));
				String countryId = String.valueOf(geoNameLoc.getCountry().getId());
				doc.add(new StringField("PCL", countryId, Field.Store.YES));
				country = getAlternateNamesStr(countryId, country, countryAltNames);
				if (!typeCode.equalsIgnoreCase("PCLI")){
					ancestorsNames.append(country + ", ");
					ancestorsIds.append(countryId + ", ");
				}
				// Get Continent Info
				String continent = geoNameLoc.getCountry().getContinentName();
				String continentId = String.valueOf(geoNameLoc.getCountry().getContinentId());
				doc.add(new TextField("Continent", continent, Field.Store.YES));
				ancestorsNames.append(continent);
				ancestorsIds.append(continentId);
				//create ancestors for easy querying
				doc.add(new TextField("AncestorsNames", ancestorsNames.toString(), Field.Store.YES));
				doc.add(new TextField("AncestorsIds", ancestorsIds.toString(), Field.Store.YES));
			} else {
				//Check when it is not a country or continent or major region
				print = true;
			}
			
			//Finally add the name field
			name = getAlternateNamesStr(id, name, alternateNames);
			doc.add(new TextField("Name", name, Field.Store.YES));
			if (ancestorsNames.toString() != ""){
				doc.add(new TextField("FullHierarchy", name + ", " + ancestorsNames.toString(), Field.Store.YES));
			} else {
				doc.add(new TextField("FullHierarchy", name, Field.Store.YES));
			}

			// add all alternate names individually for strict search
			// doc.add(new TextField("Name", name, Field.Store.YES));
			// List<String> altNameList = getAlternateNamesList(id, name, alternateNames);
			// for(String altName : altNameList) {
			// 	doc.add(new TextField("AltName", altName, Field.Store.YES));
			// }

			if(print){
				for(IndexableField field: doc.getFields()){
					System.out.print(field.name() + ":" + field.stringValue() + ", ");
				}
				System.out.println();
			}

			//Create fields and index to lucene
			writer.addDocument(doc);
		} catch (Exception e){
			log.info("error: "+ e.getMessage() + " for " + geoNameLoc);
			e.printStackTrace();
		}
	}

	private String getAlternateNamesStr(String id, String name, Set<String> altNames){
		// Customize names if necessary
		name = cleanName(id, name);
		altNames = cleanAltNames(id, name, altNames);
		StringBuilder altNamesStr = new StringBuilder(name);
		if(altNames.size() > 0){
			altNamesStr.append(" (");
			boolean hasValidAlternateName = false;
			for (String alternateName : altNames){
				if (!alternateName.equalsIgnoreCase(name)){
					altNamesStr.append(alternateName + ", "); 
					hasValidAlternateName = true;
				}
			}
			altNamesStr.setLength(altNamesStr.length() - 2);
			if(hasValidAlternateName)
				altNamesStr.append(")");
		}
		return altNamesStr.toString();
	}

	private String cleanName(String id, String name){
		if (id.equals("2635167")){
			// change name from United Kingdom of Great Britan and Northern Island
			// We make this change as conjunctions in union names turns out
			// to be a bad idea overall for search operations
			name = "United Kingdom";
		}
		return name;
	}

	private Set<String> cleanAltNames(String id, String name, Set<String> altNames){
		// Customize if necessary
		if (id.equals("1562822")){
			altNames.add("Viet Nam");
		}
		return altNames;
	}

}
