package edu.asu.zoophy.geonames.indexer.geotree;

import java.util.Set;

public class GeoNameLocation {
	private int id;
	private String name;
	private String asciiname;
	private Set<String> alternatenames;
	private double latitude;
	private double longitude;
	private String typeClass;
	private String typeCode;
	private String countrycode;
	private String population;
	private Adm state;
	private Adm county;
	private Country country;

	public GeoNameLocation(int id, String name, String asciiname, Set<String> alternatenames, double latitude,
			double longitude, String typeClass, String typeCode, String countrycode, String population) {
		this.id = id;
		this.name = name;
		this.asciiname = asciiname;
		this.alternatenames = alternatenames;
		this.latitude = latitude;
		this.longitude = longitude;
		this.typeClass = typeClass;
		this.typeCode = typeCode;
		this.countrycode = countrycode;
		this.population = population;
		this.state = null;
		this.county = null;
		this.country = null;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getAsciiname() {
		return asciiname;
	}

	public Set<String>  getAlternatenames() {
		return alternatenames;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public String getTypeClass() {
		return typeClass;
	}

	public String getTypeCode() {
		return typeCode;
	}

	public String getCountrycode() {
		return countrycode;
	}

	public String getPopulation() {
		return population;
	}

	public Adm getState() {
		return state;
	}

	public Adm getCounty() {
		return county;
	}

	public Country getCountry() {
		return country;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAsciiname(String asciiname) {
		this.asciiname = asciiname;
	}

	public void setAlternatenames(Set<String>  alternatenames) {
		this.alternatenames = alternatenames;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public void setTypeClass(String typeClass) {
		this.typeClass = typeClass;
	}

	public void setTypeCode(String typeCode) {
		this.typeCode = typeCode;
	}

	public void setCountrycode(String countrycode) {
		this.countrycode = countrycode;
	}

	public void setPopulation(String population) {
		this.population = population;
	}

	public void setState(Adm state) {
		this.state = state;
	}

	public void setCounty(Adm county) {
		this.county = county;
	}

	public void setCountry(Country country) {
		this.country = country;
	}

	@Override
	public String toString() {
		return "GeoNameLocation [id=" + id + ", name=" + name + ", asciiname=" + asciiname + ", alternatenames="
				+ String.join(",", alternatenames) + ", latitude=" + latitude + ", longitude=" + longitude
				+ ", typeClass=" + typeClass + ", typeCode=" + typeCode + ", countrycode=" + countrycode
				+ ", population=" + population + ", state=" + state + ", county=" + county + ", country=" + country
				+ "]";
	}
	
	
}