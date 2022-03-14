/* ==================================================================
 * MoneyUtils.java - 28/08/2017 5:27:14 PM
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
import java.util.Locale;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryAmountFactory;
import javax.money.format.AmountFormatQuery;
import javax.money.format.AmountFormatQueryBuilder;
import javax.money.format.MonetaryAmountFormat;
import javax.money.format.MonetaryFormats;
import org.javamoney.moneta.format.CurrencyStyle;

/**
 * Utilities for helping with {@code javax.money}.
 * 
 * @author matt
 * @version 1.0
 */
public final class MoneyUtils {

	private MoneyUtils() {
		// don't construct me
	}

	/**
	 * Invoke the {@link Bootstrap#init(javax.money.spi.ServiceProvider)} method
	 * with the internal {@link ClassLoaderServiceProvider}.
	 * 
	 * <p>
	 * This is called automatically when deployed in an OSGi runtime. Outside
	 * OSGi this method can be called to ensure the {@code javax.money} runtime
	 * uses the same {@code ServiceProvider} as under OSGi.
	 * </p>
	 */
	//public static void bootstrap() {
	//	javax.money.spi.Bootstrap.init(new ClassLoaderServiceProvider(MoneyUtils.class.getClassLoader()));
	//}

	/**
	 * Get a monetary amount format that uses currency symbols.
	 * 
	 * @param locale
	 *        the desired locale of the format
	 * @return the format
	 */
	public static MonetaryAmountFormat moneyAmountFormatWithSymbolCurrencyStyle(Locale locale) {
		AmountFormatQuery afq = ofSymbolCurrencyStyle(locale).build();
		return MonetaryFormats.getAmountFormat(afq);
	}

	/**
	 * Format a value as a monetary amount, using a currency symbol style.
	 * 
	 * @param locale
	 *        the desired locale of the format
	 * @param currencyCode
	 *        the currency code
	 * @param value
	 *        the value to format
	 * @return the formatted value
	 */
	public static String formattedMoneyAmountFormatWithSymbolCurrencyStyle(Locale locale,
			String currencyCode, BigDecimal value) {
		MonetaryAmountFormat format = moneyAmountFormatWithSymbolCurrencyStyle(locale);
		MonetaryAmountFactory<?> f = Monetary.getDefaultAmountFactory();
		MonetaryAmount amount = f.setCurrency(currencyCode).setNumber(value).create();
		return format.format(amount);
	}

	/**
	 * Get a amount format query builder that uses currency symbols.
	 * 
	 * <p>
	 * This method exists because Moneta requires an implementation-specific
	 * {@link CurrencySymbol} class to get the desired format.
	 * </p>
	 * 
	 * @param locale
	 *        the desired locale of the builder
	 * @return the builder
	 * @see https://github.com/JavaMoney/javamoney-examples/issues/25
	 */
	public static AmountFormatQueryBuilder ofSymbolCurrencyStyle(Locale locale) {
		return AmountFormatQueryBuilder.of(locale).set(CurrencyStyle.SYMBOL);
	}

}
