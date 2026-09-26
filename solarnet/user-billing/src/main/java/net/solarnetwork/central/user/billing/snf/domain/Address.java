/* ==================================================================
 * Address.java - 20/07/2020 11:14:28 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import static java.util.Objects.requireNonNullElse;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.account.domain.SnAddress;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;

/**
 * An address for billing.
 *
 * @author matt
 * @version 2.0
 */
public class Address extends BasicEntity<UserLongCompositePK>
		implements SnAddress<Address, UserLongCompositePK> {

	@Serial
	private static final long serialVersionUID = -357053992225144913L;

	private String name;
	private String email;
	private String country;
	private String timeZoneId;
	private @Nullable String region;
	private @Nullable String stateOrProvince;
	private @Nullable String locality;
	private @Nullable String postalCode;
	private String @Nullable [] street;

	// used by DAO insert to return assigned key
	private transient @Nullable Long configId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the name
	 * @param email
	 *        the email
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public Address(Long userId, String name, String email, String country, String timeZoneId) {
		this(null, null, userId, name, email, country, timeZoneId);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param userId
	 *        the user ID
	 * @param name
	 *        the name
	 * @param email
	 *        the email
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @throws IllegalArgumentException
	 *         if any argument except {@code id} and {@code created} is
	 *         {@code null}
	 */
	public Address(@Nullable Long id, @Nullable Instant created, Long userId, String name, String email,
			String country, String timeZoneId) {
		this(new UserLongCompositePK(userId, requireNonNullElse(id, UNASSIGNED_LONG_ID)), created, name,
				email, country, timeZoneId);
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param name
	 *        the name
	 * @param email
	 *        the email
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @throws IllegalArgumentException
	 *         if any argument except {@code id} and {@code created} is
	 *         {@code null}
	 * @since 2.0
	 */
	public Address(UserLongCompositePK id, @Nullable Instant created, String name, String email,
			String country, String timeZoneId) {
		super(requireNonNullArgument(id, "id"), created);
		this.name = requireNonNullArgument(name, "name");
		this.email = requireNonNullArgument(email, "email");
		this.country = requireNonNullArgument(country, "country");
		this.timeZoneId = requireNonNullArgument(timeZoneId, "timeZoneId");
	}

	@Override
	public final boolean hasId() {
		return id().entityIdIsAssigned() || configId != null;
	}

	/**
	 * Get the address ID.
	 *
	 * @return the address ID
	 */
	@JsonIgnore
	public Long getAddressId() {
		var pk = id();
		return (pk.entityIdIsAssigned() ? pk.getEntityId()
				: configId != null ? configId : pk.getEntityId());
	}

	@Override
	public Address clone() {
		return (Address) super.clone();
	}

	@Override
	public Address copyWithId(@Nullable UserLongCompositePK id) {
		var copy = new Address(requireNonNullArgument(id, "id"), created(), name, email, country,
				timeZoneId);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(@Nullable Address other) {
		if ( other == null ) {
			return;
		}
		other.region = region;
		other.stateOrProvince = stateOrProvince;
		other.locality = locality;
		other.postalCode = postalCode;
		other.street = (street != null ? Arrays.copyOf(street, street.length) : null);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Address{");
		if ( email != null ) {
			builder.append("email=");
			builder.append(email);
			builder.append(", ");
		}
		if ( country != null ) {
			builder.append("country=");
			builder.append(country);
			builder.append(", ");
		}
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the temporary entity ID.
	 *
	 * @return the configId
	 */
	@JsonIgnore
	public final @Nullable Long getConfigId() {
		return configId;
	}

	/**
	 * Set the temporary entity ID.
	 *
	 * @param configId
	 *        the configId to set
	 */
	public final void setConfigId(@Nullable Long configId) {
		this.configId = configId;
	}

	/**
	 * Get the display name.
	 *
	 * @return the name
	 */
	@Override
	public final String getName() {
		return name;
	}

	/**
	 * Set the display name.
	 *
	 * @param name
	 *        the name to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setName(String name) {
		this.name = requireNonNullArgument(name, "name");
	}

	/**
	 * Get the email.
	 *
	 * @return the email
	 */
	@Override
	public final String getEmail() {
		return email;
	}

	/**
	 * Set the email.
	 *
	 * @param email
	 *        the email to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setEmail(String email) {
		this.email = requireNonNullArgument(email, "email");
		;
	}

	/**
	 * Get the country.
	 *
	 * @return the country
	 */
	@Override
	public final String getCountry() {
		return country;
	}

	/**
	 * Set the country.
	 *
	 * @param country
	 *        the country to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setCountry(String country) {
		this.country = requireNonNullArgument(country, "country");
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the timeZoneId
	 */
	@Override
	public final String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 *
	 * @param timeZoneId
	 *        the timeZoneId to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = requireNonNullArgument(timeZoneId, "timeZoneId");
		;
	}

	/**
	 * Get the address time zone.
	 *
	 * @return the time zone, or {@code null} if not available
	 */
	@JsonIgnore
	public final @Nullable ZoneId getTimeZone() {
		String tz = getTimeZoneId();
		if ( tz != null ) {
			try {
				return ZoneId.of(tz);
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		return null;
	}

	/**
	 * Get the region.
	 *
	 * @return the region
	 */
	@Override
	public final @Nullable String getRegion() {
		return region;
	}

	/**
	 * Set the region.
	 *
	 * @param region
	 *        the region to set
	 */
	public final void setRegion(@Nullable String region) {
		this.region = region;
	}

	/**
	 * Set the state/province.
	 *
	 * @return the stateOrProvince
	 */
	@Override
	public final @Nullable String getStateOrProvince() {
		return stateOrProvince;
	}

	/**
	 * Get the state/province.
	 *
	 * @param stateOrProvince
	 *        the stateOrProvince to set
	 */
	public final void setStateOrProvince(@Nullable String stateOrProvince) {
		this.stateOrProvince = stateOrProvince;
	}

	/**
	 * Get the locality (city).
	 *
	 * @return the locality
	 */
	@Override
	public final @Nullable String getLocality() {
		return locality;
	}

	/**
	 * Set the locality (city).
	 *
	 * @param locality
	 *        the locality to set
	 */
	public final void setLocality(@Nullable String locality) {
		this.locality = locality;
	}

	/**
	 * Get the postal code.
	 *
	 * @return the postalCode
	 */
	@Override
	public final @Nullable String getPostalCode() {
		return postalCode;
	}

	/**
	 * Set the postal code.
	 *
	 * @param postalCode
	 *        the postalCode to set
	 */
	public final void setPostalCode(@Nullable String postalCode) {
		this.postalCode = postalCode;
	}

	/**
	 * Get the street list.
	 *
	 * @return the street
	 */
	@Override
	public final String @Nullable [] getStreet() {
		return street;
	}

	/**
	 * Set the street list.
	 *
	 * @param street
	 *        the street to set
	 */
	public final void setStreet(String @Nullable [] street) {
		this.street = street;
	}

}
