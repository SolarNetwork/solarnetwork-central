/* ==================================================================
 * LocalizedNamedCost.java - 31/05/2021 4:32:10 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import net.solarnetwork.central.user.billing.domain.LocalizedNamedCostInfo;
import net.solarnetwork.central.user.billing.domain.NamedCost;
import net.solarnetwork.javax.money.MoneyUtils;

/**
 * Localized version of {@link NamedCost}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class LocalizedNamedCost implements LocalizedNamedCostInfo, NamedCost {

	private final NamedCost namedCost;
	private final String localizedDescription;
	private final Locale locale;
	private final String currencyCode;

	/**
	 * Constructor.
	 * 
	 * @param namedCost
	 *        the named cost to delgate to
	 * @param locale
	 *        the desired locale
	 * @param localizedDescription
	 *        the localized description
	 * @param currencyCode
	 *        the currency code
	 */
	public LocalizedNamedCost(NamedCost namedCost, Locale locale, String localizedDescription,
			String currencyCode) {
		super();
		this.namedCost = namedCost;
		this.localizedDescription = localizedDescription;
		this.locale = locale;
		this.currencyCode = currencyCode;
	}

	@Override
	public String getName() {
		return namedCost.getName();
	}

	@Override
	public BigInteger getQuantity() {
		return namedCost.getQuantity();
	}

	@Override
	public BigDecimal getCost() {
		return namedCost.getCost();
	}

	@Override
	public BigDecimal getEffectiveRate() {
		return namedCost.getEffectiveRate();
	}

	@Override
	public String getLocalizedDescription() {
		return localizedDescription;
	}

	@Override
	public String getLocalizedQuantity() {
		NumberFormat fmt = DecimalFormat.getNumberInstance(locale);
		return fmt.format(getQuantity());
	}

	@Override
	public String getLocalizedCost() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, currencyCode,
				getCost());
	}

	@Override
	public String getLocalizedEffectiveRate() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, currencyCode,
				getEffectiveRate());
	}

}
