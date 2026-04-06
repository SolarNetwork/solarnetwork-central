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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.SerializeIgnore;

/**
 * A user domain object.
 *
 * @author matt
 * @version 2.1
 */
public class User extends BaseEntity implements UserInfo {

	@Serial
	private static final long serialVersionUID = -1968822608256484455L;

	private String email;
	private @Nullable String name;
	private @Nullable String password;
	private @Nullable Boolean enabled;
	private @Nullable Map<String, Object> internalData;
	private @Nullable Long locationId;

	private @Nullable String internalDataJson;
	private @Nullable SolarLocation location;

	private @Nullable Set<String> roles;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Sets an emtpy email.
	 * </p>
	 */
	public User() {
		this("");
	}

	/**
	 * Constructor.
	 * 
	 * @param email
	 *        the email
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public User(String email) {
		super();
		this.email = requireNonNullArgument(email, "email");
	}

	/**
	 * Construct with values.
	 *
	 * @param userId
	 *        the user ID
	 * @param email
	 *        the email
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public User(Long userId, String email) {
		this(email);
		setId(requireNonNullArgument(userId, "userId"));
	}

	@Override
	public User clone() {
		return (User) super.clone();
	}

	@Override
	public String toString() {
		return "User{email=" + email + '}';
	}

	/**
	 * Get a {@link TimeZone} instance from this user's location.
	 *
	 * <p>
	 * This will return a {@code TimeZone} for the configured location's
	 * {@link SolarLocation#getTimeZoneId()}.
	 * <p>
	 *
	 * @return the TimeZone, or {@code null} if none available
	 * @since 1.4
	 */
	public @Nullable TimeZone getTimeZone() {
		return (this.location != null && this.location.getTimeZoneId() != null
				? TimeZone.getTimeZone(this.location.getTimeZoneId())
				: null);
	}

	@Override
	public final @Nullable String getName() {
		return name;
	}

	public final void setName(@Nullable String name) {
		this.name = name;
	}

	@Override
	public final String getEmail() {
		return email;
	}

	public final void setEmail(String email) {
		this.email = requireNonNullArgument(email, "email");
	}

	@JsonIgnore
	@SerializeIgnore
	public final @Nullable String getPassword() {
		return password;
	}

	public final void setPassword(@Nullable String password) {
		this.password = password;
	}

	@Override
	public final @Nullable Boolean getEnabled() {
		return enabled;
	}

	public final void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
	}

	public final @Nullable Set<String> getRoles() {
		return roles;
	}

	public final void setRoles(@Nullable Set<String> roles) {
		this.roles = roles;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @since 1.4
	 */
	@Override
	public final @Nullable Long getLocationId() {
		return locationId;
	}

	/**
	 * Set the user's location ID.
	 *
	 * <p>
	 * If the provided {@code locationId} differs from the configured
	 * {@code location} ID, {@code location} will be set to {@code null}.
	 *
	 * @param locationId
	 *        the location ID to set
	 * @since 1.4
	 */
	@SuppressWarnings("InvalidParam")
	public final void setLocationId(@Nullable Long locationId) {
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
	 * @return the location, or {@code null}
	 * @since 1.4
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable SolarLocation getLocation() {
		return location;
	}

	/**
	 * Set the user location.
	 *
	 * <p>
	 * The {@code locationId} property will be replaced by the provided
	 * location's {@code id} if that is not {@code null}.
	 * </p>
	 *
	 * @param location
	 *        the location to set
	 * @since 1.4
	 */
	@SuppressWarnings("InvalidParam")
	public final void setLocation(@Nullable SolarLocation location) {
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
	public final @Nullable Map<String, Object> getInternalData() {
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
	public final void setInternalData(@Nullable Map<String, Object> internalData) {
		this.internalData = internalData;
	}

	/**
	 * Get an internal data property.
	 *
	 * @param key
	 *        the key of the internal data property to get
	 * @return the value, or {@code null} if not available
	 * @since 1.4
	 */
	public final @Nullable Object getInternalDataValue(String key) {
		Map<String, Object> map = getInternalData();
		return (map != null ? map.get(key) : null);
	}

	/**
	 * Add or remove one internal data element.
	 *
	 * @param key
	 *        the key to update
	 * @param data
	 *        the data to store, or if {@code null} the key to delete
	 * @return the value previously associated with {@code key}, or {@code null}
	 *         if none
	 * @since 1.4
	 */
	public final @Nullable Object putInternalDataValue(String key, @Nullable Object data) {
		Map<String, Object> map = getInternalData();
		if ( map == null ) {
			if ( data == null ) {
				return null;
			}
			map = new LinkedHashMap<>(4);
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
	 * @return a JSON encoded string, or {@code null}
	 * @since 1.4
	 */
	@SerializeIgnore
	@JsonIgnore
	public final @Nullable String getInternalDataJson() {
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
	public final void setInternalDataJson(@Nullable String json) {
		internalDataJson = json;
		internalData = null;
	}

}
