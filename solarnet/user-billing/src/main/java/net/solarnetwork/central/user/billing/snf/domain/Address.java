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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * An address for billing.
 *
 * @author matt
 * @version 1.3
 */
public class Address extends BasicLongEntity
		implements UserRelatedEntity<Long>, Differentiable<Address>, CopyingIdentity<Address, Long> {

	@Serial
	private static final long serialVersionUID = -8287306387880683563L;

	private Long userId;
	private String name;
	private String email;
	private String country;
	private String timeZoneId;
	private @Nullable String region;
	private @Nullable String stateOrProvince;
	private @Nullable String locality;
	private @Nullable String postalCode;
	private String @Nullable [] street;

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
		super(id, created);
		this.userId = requireNonNullArgument(userId, "userId");
		this.name = requireNonNullArgument(name, "name");
		this.email = requireNonNullArgument(email, "email");
		this.country = requireNonNullArgument(country, "country");
		this.timeZoneId = requireNonNullArgument(timeZoneId, "timeZoneId");
	}

	@Override
	public Address clone() {
		return (Address) super.clone();
	}

	@Override
	public Address copyWithId(@Nullable Long id) {
		Address copy = new Address(requireNonNullArgument(id, "id"), getCreated(), userId, name, email,
				country, timeZoneId);
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

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id}, {@code userId}, and {@code created} properties are not
	 * compared by this method.
	 * </p>
	 *
	 * @param other
	 *        the other entity to compare to
	 * @return {@literal true} if the properties of this instance are equal to
	 *         the other
	 */
	public boolean isSameAs(@Nullable Address other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(country, other.country)
				&& Objects.equals(email, other.email)
				&& Objects.equals(locality, other.locality)
				&& Objects.equals(name, other.name)
				&& Objects.equals(postalCode, other.postalCode)
				&& Objects.equals(region, other.region)
				&& Objects.equals(stateOrProvince, other.stateOrProvince)
				&& Arrays.equals(street, other.street)
				&& Objects.equals(timeZoneId, other.timeZoneId);
		// @formatter:on
	}

	@Override
	public boolean differsFrom(@Nullable Address other) {
		return !isSameAs(other);
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

	@Override
	public final Long getUserId() {
		return userId;
	}

	/**
	 * Set the associated user ID.
	 *
	 * @param userId
	 *        the user ID to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public final void setUserId(Long userId) {
		this.userId = requireNonNullArgument(userId, "userId");
	}

	/**
	 * Get the display name.
	 *
	 * @return the name
	 */
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
		;
	}

	/**
	 * Get the email.
	 *
	 * @return the email
	 */
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
		;
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the timeZoneId
	 */
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
