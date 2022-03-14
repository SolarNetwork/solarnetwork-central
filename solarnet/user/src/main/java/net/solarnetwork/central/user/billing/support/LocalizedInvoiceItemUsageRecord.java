/* ==================================================================
 * LocalizedInvoiceItemUsageRecord.java - 30/08/2017 3:23:56 PM
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedNamedCostInfo;
import net.solarnetwork.central.user.billing.domain.NamedCost;

/**
 * Localized version of {@link InvoiceItemUsageRecord}.
 * 
 * @author matt
 * @version 2.0
 */
public class LocalizedInvoiceItemUsageRecord
		implements InvoiceItemUsageRecord, LocalizedInvoiceItemUsageRecordInfo {

	private final String localizedUnitType;
	private final InvoiceItemUsageRecord item;
	private final Locale locale;
	private final String currencyCode;
	private final String[] localizedUsageTierDescriptions;

	/**
	 * Convenience builder.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param currencyCode
	 *        the currency code
	 * @return the localized invoice
	 */
	public static LocalizedInvoiceItemUsageRecord of(InvoiceItemUsageRecord item, Locale locale,
			String currencyCode) {
		return new LocalizedInvoiceItemUsageRecord(item, locale, null, currencyCode);
	}

	/**
	 * Convenience builder.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param currencyCode
	 *        the currency code
	 * @param localizedUsageTierDescriptions
	 *        the localized usage tier descriptions to use for the
	 *        {@link InvoiceItemUsageRecord#getUsageTiers()} named costs
	 * @return the localized invoice
	 * @since 1.1
	 */
	public static LocalizedInvoiceItemUsageRecord of(InvoiceItemUsageRecord item, Locale locale,
			String currencyCode, String[] localizedUsageTierDescriptions) {
		return new LocalizedInvoiceItemUsageRecord(item, locale, null, currencyCode,
				localizedUsageTierDescriptions);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param localizedUnitType
	 *        the localized unit type name
	 * @param currencyCode
	 *        the currency code
	 */
	public LocalizedInvoiceItemUsageRecord(InvoiceItemUsageRecord item, Locale locale,
			String localizedUnitType, String currencyCode) {
		this(item, locale, localizedUnitType, currencyCode, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @param localizedUnitType
	 *        the localized unit type name
	 * @param currencyCode
	 *        the currency code
	 * @param localizedUsageTierDescriptions
	 *        the localized usage tier descriptions to use for the
	 *        {@link InvoiceItemUsageRecord#getUsageTiers()} named costs
	 * @since 1.1
	 */
	public LocalizedInvoiceItemUsageRecord(InvoiceItemUsageRecord item, Locale locale,
			String localizedUnitType, String currencyCode, String[] localizedUsageTierDescriptions) {
		super();
		this.item = item;
		this.locale = locale;
		this.localizedUnitType = localizedUnitType;
		this.currencyCode = currencyCode;
		this.localizedUsageTierDescriptions = localizedUsageTierDescriptions;
	}

	@Override
	public String getLocalizedUnitType() {
		return localizedUnitType;
	}

	@Override
	public String getLocalizedAmount() {
		NumberFormat fmt = DecimalFormat.getNumberInstance(locale);
		return fmt.format(getAmount());
	}

	@Override
	public String getLocalizedCost() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, currencyCode,
				getCost());
	}

	@Override
	public List<LocalizedNamedCostInfo> getLocalizedUsageTiers() {
		List<NamedCost> tiers = getUsageTiers();
		if ( tiers == null || tiers.isEmpty() ) {
			return Collections.emptyList();
		}
		final int len = tiers.size();
		List<LocalizedNamedCostInfo> result = new ArrayList<>(len);
		for ( int i = 0; i < len; i++ ) {
			String desc = (localizedUsageTierDescriptions != null
					&& i < localizedUsageTierDescriptions.length ? localizedUsageTierDescriptions[i]
							: null);
			result.add(new LocalizedNamedCost(tiers.get(i), locale, desc, currencyCode));
		}
		return result;
	}

	@Override
	public String getUnitType() {
		return item.getUnitType();
	}

	@Override
	public BigDecimal getAmount() {
		return item.getAmount();
	}

	@Override
	public BigDecimal getCost() {
		return item.getCost();
	}

	@Override
	public List<NamedCost> getUsageTiers() {
		return item.getUsageTiers();
	}

}
