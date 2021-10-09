/* ==================================================================
 * LocalizedInvoiceMatch.java - 28/08/2017 2:44:02 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceMatchInfo;

/**
 * Localized version of {@link InvoiceMatch}.
 * 
 * @author matt
 * @version 1.1
 */
public class LocalizedInvoiceMatch implements InvoiceMatch, LocalizedInvoiceMatchInfo {

	private final InvoiceMatch match;
	private final Locale locale;

	/**
	 * Convenience builder.
	 * 
	 * @param match
	 *        the match to localize
	 * @param locale
	 *        the locale to localize to
	 * @return the localized match
	 */
	public static LocalizedInvoiceMatch of(InvoiceMatch match, Locale locale) {
		return new LocalizedInvoiceMatch(match, locale);
	}

	/**
	 * Constructor.
	 * 
	 * @param match
	 *        the match to localize
	 * @param locale
	 *        the locale to localize to
	 */
	public LocalizedInvoiceMatch(InvoiceMatch match, Locale locale) {
		super();
		this.match = match;
		this.locale = locale;
	}

	@Override
	public String getLocalizedDate() {
		DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(ZoneId.of(tz));
		}
		return fmt.format(getCreated());
	}

	@Override
	public String getLocalizedAmount() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getAmount());
	}

	@Override
	public String getLocalizedBalance() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getBalance());
	}

	@Override
	public Instant getCreated() {
		return match.getCreated();
	}

	@Override
	public String getTimeZoneId() {
		return match.getTimeZoneId();
	}

	@Override
	public String getInvoiceNumber() {
		return match.getInvoiceNumber();
	}

	@Override
	public BigDecimal getAmount() {
		return match.getAmount();
	}

	@Override
	public String getId() {
		return match.getId();
	}

	@Override
	public BigDecimal getBalance() {
		return match.getBalance();
	}

	@Override
	public BigDecimal getTaxAmount() {
		return match.getTaxAmount();
	}

	@Override
	public String getCurrencyCode() {
		return match.getCurrencyCode();
	}

	@Override
	public int compareTo(String o) {
		return match.compareTo(o);
	}

}
