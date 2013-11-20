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
 */

package net.solarnetwork.central.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.util.SerializeIgnore;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.util.StringUtils;

/**
 * A location entity.
 * 
 * @author matt
 * @version 1.2
 */
public class SolarLocation extends BaseEntity implements Cloneable, Serializable, Location,
		LocationMatch {

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

	/**
	 * Default constructor.
	 */
	public SolarLocation() {
		super();
	}

	/**
	 * Copy constructor for {@link Location} objects.
	 * 
	 * @param loc
	 *        the location to copy
	 */
	public SolarLocation(Location loc) {
		super();
		setName(loc.getName());
		setCountry(loc.getCountry());
		setRegion(loc.getRegion());
		setStateOrProvince(loc.getStateOrProvince());
		setLocality(loc.getLocality());
		setPostalCode(loc.getPostalCode());
		setStreet(loc.getStreet());
		setLatitude(loc.getLatitude());
		setLongitude(loc.getLongitude());
	}

	/**
	 * Change values that are non-null but empty to null.
	 * 
	 * <p>
	 * This method is helpful for web form submission, to remove filter values
	 * that are empty and would otherwise try to match on empty string values.
	 * </p>
	 */
	public void removeEmptyValues() {
		if ( !StringUtils.hasText(country) ) {
			country = null;
		}
		if ( !StringUtils.hasText(locality) ) {
			locality = null;
		}
		if ( !StringUtils.hasText(name) ) {
			name = null;
		}
		if ( !StringUtils.hasText(postalCode) ) {
			postalCode = null;
		}
		if ( !StringUtils.hasText(region) ) {
			region = null;
		}
		if ( !StringUtils.hasText(stateOrProvince) ) {
			stateOrProvince = null;
		}
		if ( !StringUtils.hasText(street) ) {
			street = null;
		}
		if ( !StringUtils.hasText(timeZoneId) ) {
			timeZoneId = null;
		}
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
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
		if ( latitude != null ) {
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
		return "SolarLocation{id=" + (getId() == null ? "" : getId()) + ",name="
				+ (name == null ? "" : name) + '}';
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	@Override
	public String getStateOrProvince() {
		return stateOrProvince;
	}

	public void setStateOrProvince(String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	@Override
	public String getLocality() {
		return locality;
	}

	public void setLocality(String locality) {
		this.locality = locality;
	}

	@Override
	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	@Override
	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	@Override
	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	@Override
	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
