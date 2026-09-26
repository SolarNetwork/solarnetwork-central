/* ==================================================================
 * Account.java - 20/07/2020 11:14:01 AM
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
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.account.domain.SnAccount;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.dao.BasicEntity;

/**
 * Billing account entity.
 *
 * @author matt
 * @version 2.0
 */
public class Account extends BasicEntity<UserLongCompositePK>
		implements SnAccount<Account, UserLongCompositePK, Address> {

	@Serial
	private static final long serialVersionUID = 717659020214827158L;

	private String currencyCode;
	private String locale;
	private @Nullable Address address;

	// used by DAO insert to return assigned key
	private transient @Nullable Long configId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param currencyCode
	 *        the currency code
	 * @param locale
	 *        the locale
	 * @throws IllegalArgumentException
	 *         if {@code currencyCode} or {@code locale} is {@code null}
	 */
	public Account(UserLongCompositePK id, Instant created, String currencyCode, String locale) {
		super(id, created);
		this.currencyCode = requireNonNullArgument(currencyCode, "currencyCode");
		this.locale = requireNonNullArgument(locale, "locale");
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the long ID
	 * @param userId
	 *        the user ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if {@code currencyCode} or {@code locale} is {@code null}
	 */
	public Account(Long id, Long userId, Instant created, String currencyCode, String locale) {
		this(new UserLongCompositePK(userId, id), created, currencyCode, locale);
	}

	@Override
	public final boolean hasId() {
		return id().entityIdIsAssigned() || configId != null;
	}

	/**
	 * Get the account ID.
	 *
	 * @return the account ID
	 */
	@JsonIgnore
	public Long getAccountId() {
		var pk = id();
		return (pk.entityIdIsAssigned() ? pk.getEntityId()
				: configId != null ? configId : pk.getEntityId());
	}

	@Override
	public Account clone() {
		return (Account) super.clone();
	}

	@Override
	public Account copyWithId(@Nullable UserLongCompositePK id) {
		var copy = new Account(requireNonNullArgument(id, "id"), created(), currencyCode, locale);
		copyTo(copy);
		return copy;
	}

	@Override
	public void copyTo(@Nullable Account other) {
		if ( other == null ) {
			return;
		}
		other.address = (address != null ? address.clone() : null);
		other.currencyCode = currencyCode;
		other.locale = locale;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Account{");
		if ( getId() != null ) {
			builder.append("id=");
			builder.append(getId());
			builder.append(", ");
		}
		if ( address != null ) {
			builder.append("address=");
			builder.append(address);
			builder.append(", ");
		}
		if ( currencyCode != null ) {
			builder.append("currencyCode=");
			builder.append(currencyCode);
			builder.append(", ");
		}
		if ( locale != null ) {
			builder.append("locale=");
			builder.append(locale);
			builder.append(", ");
		}
		builder.append("}");
		return builder.toString();
	}

	/**
	 * Get the invoice time zone.
	 *
	 * @return the time zone, or {@code null} if not available
	 */
	@JsonIgnore
	public @Nullable ZoneId getTimeZone() {
		Address addr = getAddress();
		if ( addr != null && addr.getTimeZoneId() != null ) {
			try {
				return ZoneId.of(addr.getTimeZoneId());
			} catch ( DateTimeException e ) {
				// ignore
			}
		}
		return null;
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
	 * Get the address.
	 *
	 * @return the address
	 */
	@Override
	public @Nullable Address getAddress() {
		return address;
	}

	/**
	 * Set the address.
	 *
	 * @param address
	 *        the address to set
	 */
	public void setAddress(@Nullable Address address) {
		this.address = address;
	}

	/**
	 * Get the currency code.
	 *
	 * @return the currencyCode
	 */
	@Override
	public String getCurrencyCode() {
		return currencyCode;
	}

	/**
	 * Set the currency code.
	 *
	 * @param currencyCode
	 *        the currencyCode to set
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public void setCurrencyCode(String currencyCode) {
		this.currencyCode = requireNonNullArgument(currencyCode, "currencyCode");
	}

	/**
	 * Get the locale.
	 *
	 * @return the locale, as a BCP 47 language tag
	 */
	@Override
	public String getLocale() {
		return locale;
	}

	/**
	 * Set the locale.
	 *
	 * @param locale
	 *        the locale to set, as a BCP 47 language tag
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public void setLocale(String locale) {
		this.locale = requireNonNullArgument(locale, "locale");
	}

	/**
	 * Get the locale.
	 *
	 * @return the locale as represented by the {@link #getLocale()} language
	 *         tag, or {@link Locale#US} if not available
	 */
	@Override
	public Locale locale() {
		String s = getLocale();
		if ( s == null || s.isEmpty() ) {
			return Locale.US;
		}
		Locale l = Locale.forLanguageTag(s);
		return (l != null ? l : Locale.US);
	}

}
