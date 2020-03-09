package edu.asu.zoophy.geonames.indexer.geotree;

import java.util.Set;

public class Country {
	private String iso;
	private String iso3;
	private String name;
	private double area;
	private int population;
	private int id;
	private String contName;
	private int contId;
	private Set<String> alternatenames;
	
	public Country(String iso, String iso3, String name, double area, int population,
					int id, String contName, int contId, Set<String> alternatenames) {
		this.iso = iso;
		this.iso3 = iso3;
		this.name = name;
		this.area = area;
		this.population = population;
		this.id = id;
		this.contName = contName;
		this.contId = contId;
		this.alternatenames = alternatenames;
	}

	public String getIso() {
		return iso;
	}

	public String getIso3() {
		return iso3;
	}

	public String getName() {
		return name;
	}

	public double getArea() {
		return area;
	}

	public int getPopulation() {
		return population;
	}

	public int getId() {
		return id;
	}
	
	public String getContinentName() {
		return contName;
	}
	
	public int getContinentId() {
		return contId;
	}

	public Set<String> getAlternatenames() {
		return alternatenames;
	}
}
