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

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A location entity.
 *
 * @author matt
 * @version 2.1
 */
public class SolarLocation extends BaseEntity
		implements Cloneable, Serializable, Location, LocationMatch {

	@Serial
	private static final long serialVersionUID = -3752573628286835489L;

	private String name;
	private String country;
	private String region;
	private String stateOrProvince;
	private String locality;
	private String postalCode;
	private String street;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private BigDecimal elevation;
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
		setElevation(loc.getElevation());
		setTimeZoneId(loc.getTimeZoneId());
	}

	@Override
	public SolarLocation clone() {
		return (SolarLocation) super.clone();
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

	/**
	 * Return a new SolarLocation with normalized values from another Location.
	 *
	 * @param loc
	 *        the location to normalize
	 * @return the normalized location
	 * @since 1.3
	 */
	public static SolarLocation normalizedLocation(Location loc) {
		assert loc != null;
		SolarLocation norm = new SolarLocation();
		if ( loc.getName() != null ) {
			String name = loc.getName().trim();
			if ( !name.isEmpty() ) {
				norm.setName(name);
			}
		}
		if ( loc.getCountry() != null && loc.getCountry().length() >= 2 ) {
			String country = loc.getCountry();
			if ( country.length() > 2 ) {
				country = country.substring(0, 2);
			}
			norm.setCountry(country.toUpperCase(Locale.ENGLISH));
		}
		if ( loc.getTimeZoneId() != null ) {
			TimeZone tz = TimeZone.getTimeZone(loc.getTimeZoneId());
			if ( tz != null ) {
				norm.setTimeZoneId(tz.getID());
			}
		}
		if ( loc.getRegion() != null ) {
			String region = loc.getRegion().trim();
			if ( !region.isEmpty() ) {
				norm.setRegion(region);
			}
		}
		if ( loc.getStateOrProvince() != null ) {
			String state = loc.getStateOrProvince().trim();
			if ( !state.isEmpty() ) {
				norm.setStateOrProvince(state);
			}
		}
		if ( loc.getLocality() != null ) {
			String locality = loc.getLocality().trim();
			if ( !locality.isEmpty() ) {
				norm.setLocality(locality);
			}
		}
		if ( loc.getPostalCode() != null ) {
			String postalCode = loc.getPostalCode().trim().toUpperCase(Locale.ENGLISH);
			if ( !postalCode.isEmpty() ) {
				norm.setPostalCode(postalCode);
			}
		}
		if ( loc.getStreet() != null ) {
			String street = loc.getStreet().trim();
			if ( !street.isEmpty() ) {
				norm.setStreet(street);
			}
		}
		norm.setLatitude(loc.getLatitude());
		norm.setLongitude(loc.getLongitude());
		norm.setElevation(loc.getElevation());
		return norm;
	}

	@Override
	@SerializeIgnore
	@JsonIgnore
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<>();
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
		if ( elevation != null ) {
			filter.put("elevation", elevation);
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
	public BigDecimal getLatitude() {
		return latitude;
	}

	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}

	/**
	 * Set the latitude.
	 *
	 * <p>
	 * This method is an alias for {@link #setLatitude(BigDecimal)} and is
	 * provided for compatibility with the JSON serialized form of
	 * {@link Location}.
	 * </p>
	 *
	 * @param latitude
	 *        the latitude to set
	 * @since 2.1
	 */
	public void setLat(BigDecimal latitude) {
		setLatitude(latitude);
	}

	@Override
	public BigDecimal getLongitude() {
		return longitude;
	}

	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}

	/**
	 * Set the longitude.
	 *
	 * <p>
	 * This method is an alias for {@link #setLongitude(BigDecimal)} and is
	 * provided for compatibility with the JSON serialized form of
	 * {@link Location}.
	 * </p>
	 *
	 * @param longitude
	 *        the longitude to set
	 * @since 2.1
	 */
	public void setLon(BigDecimal longitude) {
		setLongitude(longitude);
	}

	@Override
	public String getTimeZoneId() {
		return timeZoneId;
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 *
	 * <p>
	 * This method is an alias for {@link #setTimeZoneId(String)} and is
	 * provided for compatibility with the JSON serialized form of
	 * {@link Location}.
	 * </p>
	 *
	 * @param timeZoneId
	 *        the zone ID to set
	 * @since 2.1
	 */
	public void setZone(String timeZoneId) {
		setTimeZoneId(timeZoneId);
	}

	@Override
	public BigDecimal getElevation() {
		return elevation;
	}

	public void setElevation(BigDecimal elevation) {
		this.elevation = elevation;
	}

	/**
	 * Set the elevation.
	 *
	 * <p>
	 * This method is an alias for {@link #setElevation(BigDecimal)} and is
	 * provided for compatibility with the JSON serialized form of
	 * {@link Location}.
	 * </p>
	 *
	 * @param elevation
	 *        the elevation to set
	 * @since 2.1
	 */
	public void setEl(BigDecimal elevation) {
		setElevation(elevation);
	}

}
