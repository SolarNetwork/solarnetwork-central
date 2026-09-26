/* ==================================================================
 * SnAccount.java - 26 Sept 2026 10:54:18 am
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
import java.util.Locale;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * API for an account in SolarNetwork.
 *
 * @author matt
 * @version 1.0
 */
public interface SnAccount<T extends SnAccount<T, K, A> & CopyingIdentity<T, K>, K extends UserRelatedCompositeKey<K>, A extends SnAddress<?, ?>>
		extends UserIdRelated, CopyingIdentity<T, K>, Differentiable<T>, Serializable, Cloneable {

	/**
	 * Get the address.
	 *
	 * @return the address
	 */
	@Nullable
	A getAddress();

	/**
	 * Get the currency code.
	 *
	 * @return the currencyCode
	 */
	String getCurrencyCode();

	/**
	 * Get the locale.
	 *
	 * @return the locale, as a BCP 47 language tag
	 */
	String getLocale();

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
	 * Get the invoice time zone.
	 *
	 * @return the time zone, or the system default if not otherwise available
	 */
	default ZoneId timeZone() {
		final var addr = getAddress();
		if ( addr != null && addr.getTimeZoneId() != null ) {
			try {
				return ZoneId.of(addr.getTimeZoneId());
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		return ZoneId.systemDefault();
	}

	/**
	 * Get the locale.
	 *
	 * @return the locale as represented by the {@link #getLocale()} language
	 *         tag, or {@link Locale#US} if not available
	 */
	default Locale locale() {
		String s = getLocale();
		if ( s == null || s.isEmpty() ) {
			return Locale.US;
		}
		Locale l = Locale.forLanguageTag(s);
		return (l != null ? l : Locale.US);
	}

	/**
	 * Test if the properties of another entity are the same as in this
	 * instance.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared by this
	 * method.
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
		return Objects.equals(getAddress(), other.getAddress())
				&& Objects.equals(getCurrencyCode(), other.getCurrencyCode())
				&& Objects.equals(getLocale(), other.getLocale());
		// @formatter:on
	}

	@Override
	default boolean differsFrom(@Nullable T other) {
		return !isSameAs(other);
	}

}
