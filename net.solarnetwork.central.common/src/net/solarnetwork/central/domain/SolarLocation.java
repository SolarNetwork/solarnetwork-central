/* ==================================================================
 * SolarLocation.java - Apr 30, 2011 11:20:51 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import net.solarnetwork.util.SerializeIgnore;

/**
 * A location entity.
 * 
 * @author matt
 * @version $Revision$
 */
public class SolarLocation extends BaseEntity implements Cloneable, Serializable, Location {

	private static final long serialVersionUID = 5597249684004944213L;

	private String name;
	private String country;
	private String region;
	private String stateOrProvince;
	private String locality;
	private String postalCode;
	private String street;
	private Double latitude;
	private Double longitude;
	private String timeZoneId;
	
	@Override
	@SerializeIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<String, Object>();
		if ( name != null ) {
			filter.put("name", name);
		}
		if ( country != null ) {
			filter.put("c", country);
		}
		if ( region != null ) {
			filter.put("region", region);
		}
		if ( stateOrProvince != null ) {
			filter.put("st", stateOrProvince);
		}
		if ( postalCode != null ) {
			filter.put("postalCode", postalCode);
		}
		if ( locality != null ) {
			filter.put("l", locality);
		}
		if ( street != null ) {
			filter.put("street", street);
		}
		if ( latitude != null  ) {
			filter.put("latitude", latitude);
		}
		if ( longitude != null ) {
			filter.put("longitude", longitude);
		}
		if ( timeZoneId != null ) {
			filter.put("tz", timeZoneId);
		}
		return filter;
	}
	
	@Override
	public String toString() {
		return "SolarLocation{id=" +(getId() == null ? "" : getId())
				+",name=" +(name == null ? "" : name)
				+'}';
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return the region
	 */
	public String getRegion() {
		return region;
	}
	/**
	 * @param region the region to set
	 */
	public void setRegion(String region) {
		this.region = region;
	}
	/**
	 * @return the stateOrProvince
	 */
	public String getStateOrProvince() {
		return stateOrProvince;
	}
	/**
	 * @param stateOrProvince the stateOrProvince to set
	 */
	public void setStateOrProvince(String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}
	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}
	/**
	 * @param locality the locality to set
	 */
	public void setLocality(String locality) {
		this.locality = locality;
	}
	/**
	 * @return the postalCode
	 */
	public String getPostalCode() {
		return postalCode;
	}
	/**
	 * @param postalCode the postalCode to set
	 */
	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}
	/**
	 * @return the street
	 */
	public String getStreet() {
		return street;
	}
	/**
	 * @param street the street to set
	 */
	public void setStreet(String street) {
		this.street = street;
	}
	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	/**
	 * @return the timeZoneId
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}
	/**
	 * @param timeZoneId the timeZoneId to set
	 */
	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}
	
}
