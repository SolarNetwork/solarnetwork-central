/* ==================================================================
 * SnAccountInput.java - 26 Sept 2026 3:03:49 pm
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

package net.solarnetwork.central.user.account.domain;

import static net.solarnetwork.util.StringUtils.nonEmptyString;
import java.io.Serial;
import java.io.Serializable;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.domain.validation.ValidLanguageTag;

/**
 * DTO for account configuration.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class SnAccountInput implements Serializable {

	@Serial
	private static final long serialVersionUID = -6337069674618723091L;

	@NotNull
	private @Nullable CurrencyUnit currency;

	@NotNull
	@ValidLanguageTag
	private @Nullable String locale;

	/**
	 * Constructor.
	 */
	public SnAccountInput() {
		super();
	}

	/**
	 * Create an input instance from an account entity.
	 * 
	 * @param acct
	 *        the account to create an input for
	 * @return the input
	 */
	public static SnAccountInput forAccount(@Nullable SnAccount<?, ?, ?> acct) {
		final var result = new SnAccountInput();
		if ( acct != null ) {
			result.setCurrency(Monetary.getCurrency(acct.getCurrencyCode()));
			result.setLocale(acct.getLocale());
		}
		return result;
	}

	/**
	 * Get the currency.
	 *
	 * @return the currency
	 */
	public final @Nullable CurrencyUnit getCurrency() {
		return currency;
	}

	/**
	 * Set the currency.
	 *
	 * @param currency
	 *        the currency to set
	 */
	public final void setCurrency(@Nullable CurrencyUnit currency) {
		this.currency = currency;
	}

	/**
	 * Get the locale.
	 *
	 * @return the locale, as a BCP 47 language tag
	 */
	public final @Nullable String getLocale() {
		return locale;
	}

	/**
	 * Set the locale.
	 *
	 * @param locale
	 *        the locale to set, as a BCP 47 language tag
	 */
	public final void setLocale(@Nullable String locale) {
		this.locale = nonEmptyString(locale);
	}

}
