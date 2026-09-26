/* ==================================================================
 * SnAddress.java - 26 Sept 2026 11:09:55 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.account.domain;

import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * API for an account address in SolarNetwork.
 *
 * @author matt
 * @version 1.0
 */
public interface SnAddress<T extends SnAddress<T, K> & CopyingIdentity<T, K>, K extends UserRelatedCompositeKey<K>>
		extends UserIdRelated, CopyingIdentity<T, K>, Differentiable<T>, Serializable, Cloneable {

	/**
	 * Get the display name.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Get the email.
	 *
	 * @return the email
	 */
	String getEmail();

	/**
	 * Get the country.
	 *
	 * @return the country
	 */
	String getCountry();

	/**
	 * Get the time zone ID.
	 *
	 * @return the timeZoneId
	 */
	String getTimeZoneId();

	/**
	 * Get the region.
	 *
	 * @return the region
	 */
	@Nullable
	String getRegion();

	/**
	 * Set the state/province.
	 *
	 * @return the stateOrProvince
	 */
	@Nullable
	String getStateOrProvince();

	/**
	 * Get the locality (city).
	 *
	 * @return the locality
	 */
	@Nullable
	String getLocality();

	/**
	 * Get the postal code.
	 *
	 * @return the postalCode
	 */
	@Nullable
	String getPostalCode();

	/**
	 * Get the street list.
	 *
	 * @return the street
	 */
	String @Nullable [] getStreet();

	@Override
	default boolean hasId() {
		K id = getId();
		return (id != null && id.allKeyComponentsAreAssigned());
	}

	@Override
	default Long getUserId() {
		return nonnull(getId(), "id").getUserId();
	}

	/**
	 * Get the address time zone.
	 *
	 * @return the time zone, or the system default if not otherwise available
	 */
	default @Nullable ZoneId timeZone() {
		String tz = getTimeZoneId();
		if ( tz != null ) {
			try {
				return ZoneId.of(tz);
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		return ZoneId.systemDefault();
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
	default boolean isSameAs(@Nullable T other) {
		if ( other == null ) {
			return false;
		}
		// @formatter:off
		return Objects.equals(getCountry(), other.getCountry())
				&& Objects.equals(getEmail(), other.getEmail())
				&& Objects.equals(getLocality(), other.getLocality())
				&& Objects.equals(getName(), other.getName())
				&& Objects.equals(getPostalCode(), other.getPostalCode())
				&& Objects.equals(getRegion(), other.getRegion())
				&& Objects.equals(getStateOrProvince(), other.getStateOrProvince())
				&& Arrays.equals(getStreet(), other.getStreet())
				&& Objects.equals(getTimeZoneId(), other.getTimeZoneId());
		// @formatter:on
	}

	@Override
	default boolean differsFrom(@Nullable T other) {
		return !isSameAs(other);
	}

}
