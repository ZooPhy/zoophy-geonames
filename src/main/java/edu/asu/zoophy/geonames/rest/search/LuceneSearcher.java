package edu.asu.zoophy.geonames.rest.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import edu.asu.zoophy.geonames.indexer.LuceneWriter;
import edu.asu.zoophy.geonames.rest.exception.InvalidLuceneQueryException;
import edu.asu.zoophy.geonames.rest.exception.LuceneSearcherException;

/**
 * Responsible for retrieving information from Lucene
 * @author amagge
 */
@Repository("LuceneSearcher")
public class LuceneSearcher {
	
	private Directory indexDirectory;
	private QueryParser queryParser;
	private Map<String, String> custMap;

	private final static Logger logger = Logger.getLogger("LuceneSearcher");
	
	/**
	 * Method that starts the Lucene Service and sanity checks the index
	 */
	public LuceneSearcher(@Value("${lucene.index.location}") String indexLocation,
							@Value("${geonames.mapping.file}") String custMapFile) throws LuceneSearcherException {
		try {
			Path index = Paths.get(indexLocation);
			indexDirectory = FSDirectory.open(index);
			Analyzer analyzer = new StandardAnalyzer(LuceneWriter.stopWordsOverride);
			queryParser = new QueryParser("Name", analyzer); 
			logger.info("Connected to Index at: "+indexLocation);
			IndexReader reader = DirectoryReader.open(indexDirectory);
			logger.info("Number of docs: "+reader.numDocs());
			if(reader.numDocs()>0){
				logger.info("Getting fields for a sample document in the index. . .");
				reader.document(1).getFields();
				List<IndexableField> fields = reader.document(1).getFields();
				for(int i=0; i<fields.size();i++){
					logger.info(i+1 + ") " + fields.get(i).name() + ":"+ fields.get(i).stringValue());
				}
			} else {
				logger.warning("Index is empty!!");
			}
			reader.close();
			// Load the map
			logger.info("Loading custom map");
			custMap = getCustomMap(custMapFile);
			logger.info("Loaded custom map :" + custMap.size());
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
			throw new LuceneSearcherException("Could not open Lucene Index at: "+indexLocation+ " : "+ioe.getMessage());
		}
	}
	
	/**
	 * Closes Lucene resources
	 */
	@PreDestroy
	private void close() {
		try {
			indexDirectory.close();
			logger.info("Lucene Index closed");
		}
		catch (IOException ioe) {
			logger.warning("Issue closing Lucene Index: "+ioe.getMessage());
		}
	}

	/**
	 * Search Lucene Index for records matching querystring
	 * @param querystring - valid Lucene query string
	 * @param numRecords - number of requested records 
	 * @param showAvailable - check for number of matching available records 
	 * @return Top Lucene query results as a Result object
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public Result searchIndex(String querystring, int numRecords, boolean showAvailable) throws LuceneSearcherException, InvalidLuceneQueryException {
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		Query query;
		TopDocs documents;
		TotalHitCountCollector collector = null;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			query = queryParser.parse(querystring);
			logger.info("'" + querystring + "' ==> '" + query.toString() + "'");
			if(showAvailable){
				collector = new TotalHitCountCollector();
				indexSearcher.search(query, collector);
			}

			SortField sortField = new SortField("Population", SortField.Type.LONG, true);
			Sort sort = new Sort(sortField);
			documents = indexSearcher.search(query, numRecords, sort);

			//documents = indexSearcher.search(query, numRecords);
			List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
			for (ScoreDoc scoreDoc : documents.scoreDocs) {
				Document document = indexSearcher.doc(scoreDoc.doc);
				Map<String,String> docMap = new HashMap<String,String>();
				List<IndexableField> fields = document.getFields();
				for(IndexableField field : fields){
					docMap.put(field.name(), field.stringValue());
				}
				mapList.add(docMap);
			}
			Result result = new Result(mapList, 
					mapList.size(), 
					collector==null?(mapList.size() < numRecords?mapList.size():-1)
							:collector.getTotalHits());
			return result;
		} catch (ParseException pe) {
			throw new InvalidLuceneQueryException(pe.getMessage());
		} catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				logger.warning("Could not close IndexReader: "+ioe.getMessage()); 
			}
		}
	}

	/**
	 * Search Lucene Index for a location and return best matched record
	 * @param location - location in a string
	 * @param maxRecs - maximum records to be returned
	 * @param mode - search mode i.e. default, strict, full (more needs to be added)
	 * @return Top Lucene query result as a Result object
	 * @throws LuceneSearcherException 
	 * @throws InvalidLuceneQueryException 
	 */
	public Result searchLocation(String location, int maxRecs, String mode) throws LuceneSearcherException, InvalidLuceneQueryException {
		IndexReader reader = null;
		IndexSearcher indexSearcher = null;
		TopDocs documents;
		TotalHitCountCollector collector = null;
		try {
			reader = DirectoryReader.open(indexDirectory);
			indexSearcher = new IndexSearcher(reader);
			List<Map<String,String>> mapList = new LinkedList<Map<String,String>>();
			Result result = new Result(mapList, mapList.size(), 0);
			List<Query> queries = getQueries(location.trim(), mode);
			for (Query query : queries) {
				logger.info("'" + location + "' ==> '" + query.toString() + "'");
				collector = new TotalHitCountCollector();
				SortField sortField = new SortField("Population", SortField.Type.LONG, true);
				Sort sort = new Sort(sortField);
				indexSearcher.search(query, collector);
				int totalCounts = collector.getTotalHits();
				if (totalCounts > 0){
					int numRecords = Math.min(totalCounts, maxRecs);
					documents = indexSearcher.search(query, numRecords, sort);
					for (ScoreDoc scoreDoc : documents.scoreDocs) {
						Document document = indexSearcher.doc(scoreDoc.doc);
						Map<String,String> docMap = new HashMap<String,String>();
						List<IndexableField> fields = document.getFields();
						for(IndexableField field : fields){
							docMap.put(field.name(), field.stringValue());
						}
						mapList.add(docMap);
					}
					result = new Result(mapList, mapList.size(), totalCounts);
					// break if already found based on search mode
					break;
				}
			}
			return result;
		} catch (Exception e) {
			throw new LuceneSearcherException(e.getMessage());
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			}
			catch (IOException ioe) {
				logger.warning("Could not close IndexReader: "+ioe.getMessage()); 
			}
		}
	}

	/**
	 * Based on the location, a list of Lucene Query objects are retrieved arranged from strict to relaxed
	 * @param location - location in a string
	 * @param mode - search mode i.e. default, strict, full (more needs to be added)
	 * @return List of Lucene Query objects
	 */
	private List<Query> getQueries(String location, String mode) {
		List<Query> queries = new ArrayList<Query>();
		String queryString = "";
		String fullQueryString = "";
		boolean addFullQuery = false;
		Query query;
		FuzzyQuery fuzzyQuery;
		BooleanQuery.Builder boolQueryBuilder = null;
		// First check if they are in the custom map
		if (custMap.containsKey(location)){
			queryString = "GeonameId:\""+custMap.get(location) +"\"";
		} else {
			// Next check if there are commas and encode them as child, parent
			String[] locations = location.split(",");
			if (mode != null && mode.equalsIgnoreCase("full")){
				addFullQuery = true;
			}
			int termCount = 0;
			for(int i=0; i<locations.length; i++){
				String loc_part = locations[i].trim();
				if (!loc_part.isEmpty()){
					if (termCount == 0) {
						queryString = "Name:\""+ loc_part +"\"";
						boolQueryBuilder = new BooleanQuery.Builder();
						String[] loc_subparts = loc_part.split(" ");
						for(String loc_subpart: loc_subparts){
							fuzzyQuery = new FuzzyQuery(new Term("Name", loc_subpart.toLowerCase()), 1);
							boolQueryBuilder.add(fuzzyQuery, BooleanClause.Occur.MUST);
						}
					} else {
						if(!queryString.trim().isEmpty()){
							queryString += " AND AncestorsNames:\""+ loc_part +"\"";
							if (boolQueryBuilder != null) {
								int editDistance = (i==locations.length-1 ? 0 : 1);
								String[] loc_subparts = loc_part.split(" ");
								for(String loc_subpart: loc_subparts){
									fuzzyQuery = new FuzzyQuery(new Term("AncestorsNames", loc_subpart.toLowerCase()), editDistance);
									boolQueryBuilder.add(fuzzyQuery, BooleanClause.Occur.MUST);
								}
							}
						} else {
							// this part should never be executed so remove it anyway (??)
							queryString = "AncestorsNames:\""+ loc_part +"\"";
						}
					}
					if (addFullQuery) {
						fullQueryString += termCount==0 ? "FullHierarchy:\""+ loc_part +"\"" : " AND FullHierarchy:\""+ loc_part +"\"";
					}
					termCount++;
				}
			}
			if (locations.length > 1){
				// if more than one field, add full query in the hierarchy
				addFullQuery = true;
			}
			if(queryString.trim().isEmpty()){
				logger.warning("Empty query");
				queryString = "Name:NOTAVALIDLOCATIONNAME";
			}
		}
		try {
			query = queryParser.parse(queryString);
			// logger.info("Query:'" + location + "' ==> '" + queryString + "' ==> '" + query + "'");
			queries.add(query);
		} catch (ParseException e) {
			logger.warning("Error parsing query for:'" + location + "' ==> '" + queryString + "'");;
		}
		// Check and add full query string
		if (addFullQuery){
			try {
				query = queryParser.parse(fullQueryString);
				// logger.info("Full Query:'" + location + "' ==> '" + fullQueryString + "' ==> '" + query + "'");
				queries.add(query);
			} catch (Exception e) {
				logger.warning("Error parsing query for:'" + location + "' ==> '" + fullQueryString + "'");;
			}
			if (boolQueryBuilder != null) {
				BooleanQuery boolQuery = boolQueryBuilder.build();
				// logger.info("Fuzzy Query:'" + location + "' ==> '" + boolQuery.toString() + "'");
				queries.add(boolQuery);
			}
		}
		return queries;
	}

	private static Map<String, String> getCustomMap(String filename) {
		Map<String, String> map = new HashMap<String, String>();
		File geoFile = new File(filename);
		Scanner scan;
		try {
			scan = new Scanner(geoFile);
			while (scan.hasNext()) {
				String record = scan.nextLine().trim();
				if(record.split("\t").length==2){
					logger.info("Adding:'" + record.split("\t")[0].trim() +"':'"+ record.split("\t")[1].trim()+"'");
					map.put(record.split("\t")[0].trim(), record.split("\t")[1].trim());
				}
			}
			scan.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return map;
	}


}
