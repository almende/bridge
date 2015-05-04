/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.bridge.oldDataStructs;

/**
 * The Class Location.
 */
public class Location {
	private String	latitude	= null;
	private String	longitude	= null;
	private String	time		= null;
	
	/**
	 * Instantiates a new location.
	 */
	public Location(){}
	
	/**
	 * Instantiates a new location.
	 *
	 * @param latitude
	 *            the latitude
	 * @param longitude
	 *            the longitude
	 */
	public Location(String latitude, String longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	/**
	 * Instantiates a new location.
	 *
	 * @param latitude
	 *            the latitude
	 * @param longitude
	 *            the longitude
	 * @param time
	 *            the time
	 */
	public Location(String latitude, String longitude, String time) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.time = time;
	}
	
	/**
	 * Gets the latitude.
	 *
	 * @return the latitude
	 */
	public String getLatitude() {
		return latitude;
	}
	
	/**
	 * Sets the latitude.
	 *
	 * @param latitude
	 *            the new latitude
	 */
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	
	/**
	 * Gets the longitude.
	 *
	 * @return the longitude
	 */
	public String getLongitude() {
		return longitude;
	}
	
	/**
	 * Sets the longitude.
	 *
	 * @param longitude
	 *            the new longitude
	 */
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	/**
	 * Gets the time.
	 *
	 * @return the time
	 */
	public String getTime() {
		return time;
	}
	
	/**
	 * Sets the time.
	 *
	 * @param time
	 *            the new time
	 */
	public void setTime(String time) {
		this.time = time;
	}
}