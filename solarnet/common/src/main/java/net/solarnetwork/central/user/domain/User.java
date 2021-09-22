/* ==================================================================
 * User.java - Dec 11, 2009 8:27:28 PM
 * 
 * Copyright 2007-2009 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A user domain object.
 * 
 * @author matt
 * @version 2.0
 */
public class User extends BaseEntity implements UserInfo {

	private static final long serialVersionUID = -1968822608256484455L;

	private String name;
	private String email;
	private String password;
	private Boolean enabled;
	private Map<String, Object> internalData;
	private Long locationId = null;

	private String internalDataJson;
	private SolarLocation location;

	private Set<String> roles;

	/**
	 * Default constructor.
	 */
	public User() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param userId
	 *        the user ID
	 * @param email
	 *        the email
	 */
	public User(Long userId, String email) {
		super();
		setId(userId);
		setEmail(email);
	}

	@Override
	public String toString() {
		return "User{email=" + email + '}';
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@JsonIgnore
	@SerializeIgnore
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	/**
	 * Get a {@link TimeZone} instance from this user's location.
	 * 
	 * <p>
	 * This will return a {@code TimeZone} for the configured location's
	 * {@link SolarLocation#getTimeZoneId()}.
	 * <p>
	 * 
	 * @return the TimeZone, or {@literal null} if none available
	 * @since 1.4
	 */
	public TimeZone getTimeZone() {
		return (this.location != null && this.location.getTimeZoneId() != null
				? TimeZone.getTimeZone(this.location.getTimeZoneId())
				: null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.4
	 */
	@Override
	public Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the user's location ID.
	 * 
	 * <p>
	 * If the provided {@code locationId} differs from the configured
	 * {@code location} ID, {@code location} will be set to {@literal null}.
	 * 
	 * @param locationId
	 *        the location ID to set
	 * @since 1.4
	 */
	public void setLocationId(Long locationId) {
		this.locationId = locationId;
		if ( (locationId == null && this.location != null) || (locationId != null
				&& this.location != null && !locationId.equals(location.getId())) ) {
			this.location = null;
		}
	}

	/**
	 * Get the user location.
	 * 
	 * <p>
	 * This object may not be available, even if {@link #locationId} returns a
	 * value.
	 * </p>
	 * 
	 * @return the location, or {@literal null}
	 * @since 1.4
	 */
	@SerializeIgnore
	@JsonIgnore
	public SolarLocation getLocation() {
		return location;
	}

	/**
	 * Set the user location.
	 * 
	 * <p>
	 * The {@code locationId} property will be replaced by the provided
	 * location's {@code id} if that is not {@literal null}.
	 * </p>
	 * 
	 * @param location
	 *        the location to set
	 * @since 1.4
	 */
	public void setLocation(SolarLocation location) {
		this.location = location;
		if ( location != null && location.getId() != null ) {
			this.locationId = location.getId();
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.4
	 */
	@Override
	@JsonIgnore
	@SerializeIgnore
	public Map<String, Object> getInternalData() {
		if ( internalData == null && internalDataJson != null ) {
			internalData = JsonUtils.getStringMap(internalDataJson);
			internalDataJson = null;
		}
		return internalData;
	}

	/**
	 * Set the internal data.
	 * 
	 * @param internalData
	 *        the internal data to set
	 * @since 1.4
	 */
	public void setInternalData(Map<String, Object> internalData) {
		this.internalData = internalData;
	}

	/**
	 * Get an internal data property.
	 * 
	 * @param key
	 *        the key of the internal data property to get
	 * @return the value, or {@literal null} if not available
	 * @since 1.4
	 */
	public Object getInternalDataValue(String key) {
		Map<String, Object> map = getInternalData();
		return (map != null ? map.get(key) : null);
	}

	/**
	 * Add or remove one internal data element.
	 * 
	 * @param key
	 *        the key to update
	 * @param data
	 *        the data to store, or if {@literal null} the key to delete
	 * @return the value previously associated with {@code key}, or
	 *         {@literal null} if none
	 * @since 1.4
	 */
	public Object putInternalDataValue(String key, Object data) {
		Map<String, Object> map = getInternalData();
		if ( map == null ) {
			if ( data == null ) {
				return null;
			}
			map = new LinkedHashMap<String, Object>(4);
			setInternalData(map);
		}
		if ( data == null ) {
			return map.remove(key);
		} else {
			return map.put(key, data);
		}
	}

	/**
	 * Get the internal data as a JSON string.
	 * 
	 * @return a JSON encoded string, or {@literal null}
	 * @since 1.4
	 */
	@SerializeIgnore
	@JsonIgnore
	public String getInternalDataJson() {
		if ( internalDataJson == null ) {
			internalDataJson = JsonUtils.getJSONString(internalData, null);
		}
		return internalDataJson;
	}

	/**
	 * Set the internal data object via a JSON string.
	 * 
	 * <p>
	 * This method will remove any previously created {@code internalData} value
	 * and replace it with the values parsed from the provided JSON. The JSON is
	 * expected to be a JSON object with string keys.
	 * </p>
	 * 
	 * @param json
	 *        the internal data to set
	 * @since 1.4
	 */
	@JsonProperty
	// @JsonProperty needed because of @JsonIgnore on getter
	public void setInternalDataJson(String json) {
		internalDataJson = json;
		internalData = null;
	}

}
