package edu.asu.zoophy.geonames.indexer.geotree;

import java.util.Set;

public class Adm {
	private String code;
	private String name;
	private String asciiname;
	private int id;
	private Set<String> alternatenames;


	public Adm(String code, String name, String asciiname, int id, Set<String> alternatenames) {
		this.code = code;
		this.name = name;
		this.asciiname = asciiname;
		this.id = id;
		this.alternatenames = alternatenames;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getAsciiname() {
		return asciiname;
	}

	public int getId() {
		return id;
	}
	
	public Set<String> getAlternatenames() {
		return alternatenames;
	}
}
