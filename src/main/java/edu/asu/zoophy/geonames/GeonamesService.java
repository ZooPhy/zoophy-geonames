package edu.asu.zoophy.geonames;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import java.util.logging.Logger;

import edu.asu.zoophy.geonames.indexer.Indexer;
import edu.asu.zoophy.geonames.downloader.Downloader;

@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})

@SpringBootApplication
public class GeonamesService {

	private static Logger log = Logger.getLogger("GeonamesService");

    public static void main(String[] args) {
    	if(args.length > 0){
			if (args[0].equalsIgnoreCase("create")){
				Indexer.createIndex();
			} else if (args[0].equalsIgnoreCase("download")) {
				Downloader.downloadGeonamesFiles();
			} else {
				log.info("Invalid argument:'" + args[0] + "'. Please check the documentation for valid arguments.");
			}
    	} else {
    		SpringApplication.run(GeonamesService.class, args);
    	}
    }
}
