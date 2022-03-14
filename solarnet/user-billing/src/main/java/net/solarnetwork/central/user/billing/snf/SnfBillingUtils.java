/* ==================================================================
 * SnfBillingUtils.java - 1/11/2021 1:17:02 PM
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

package net.solarnetwork.central.user.billing.snf;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.DEFAULT_ITEM_ORDER;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.META_AVAILABLE_CREDIT;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NamedCost;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.support.MoneyUtils;

/**
 * Utilities for SNF billing.
 * 
 * @author matt
 * @version 1.0
 */
public final class SnfBillingUtils {

	/**
	 * Generate usage metadata for a set of usage data.
	 * 
	 * @param usageData
	 *        the usage data
	 * @param tierData
	 *        the tier data
	 * @param tierKey
	 *        the tier key
	 * @return the metadata object
	 */
	public static Map<String, Object> usageMetadata(Map<String, UsageInfo> usageData,
			Map<String, List<NamedCost>> tierData, String tierKey) {
		Map<String, Object> result = new LinkedHashMap<>(2);
		if ( usageData.containsKey(tierKey) ) {
			result.put(SnfInvoiceItem.META_USAGE, usageData.get(tierKey).toMetadata());
		}
		if ( tierData.containsKey(tierKey) ) {
			List<Map<String, Object>> tierMeta = tierData.get(tierKey).stream().map(e -> e.toMetadata())
					.collect(toList());
			result.put(SnfInvoiceItem.META_TIER_BREAKDOWN, tierMeta);
		}
		return result;
	}

	/**
	 * Create a {@link net.solarnetwork.central.user.billing.domain.Invoice}
	 * from a {@link SnfInvoice}.
	 * 
	 * @param invoice
	 *        the input invoice entity
	 * @param messageSource
	 *        the message source
	 * @param locale
	 *        the locale
	 * @return the invoice, never {@literal null}
	 */
	public static InvoiceImpl invoiceForSnfInvoice(SnfInvoice invoice, MessageSource messageSource,
			Locale locale) {
		List<InvoiceItem> invoiceItems = null;
		if ( locale != null ) {
			invoiceItems = invoice.getItems().stream().sorted(DEFAULT_ITEM_ORDER).map(e -> {
				String desc = messageSource.getMessage(e.getKey() + ".item", null, null, locale);
				InvoiceItemImpl item;
				UsageInfo usageInfo = e.getUsageInfo();
				if ( usageInfo != null ) {
					List<net.solarnetwork.central.user.billing.domain.NamedCost> tiers = usageInfo
							.getUsageTiers();
					String[] tierDescriptions = null;
					if ( tiers != null && !tiers.isEmpty() ) {
						tierDescriptions = new String[tiers.size()];
						for ( int i = 0; i < tiers.size(); i++ ) {
							tierDescriptions[i] = String.format("%s %d",
									messageSource.getMessage("invoiceItemTier", null, null, locale),
									i + 1);
						}
					}
					String unitTypeDesc = messageSource.getMessage(usageInfo.getUnitType() + ".unit",
							null, null, locale);
					LocalizedInvoiceItemUsageRecord locInfo = new LocalizedInvoiceItemUsageRecord(
							usageInfo, locale, unitTypeDesc, invoice.getCurrencyCode(),
							tierDescriptions);
					item = new InvoiceItemImpl(invoice, e, singletonList(locInfo));
				} else {
					if ( e.getItemType() == InvoiceItemType.Credit && e.getMetadata() != null
							&& e.getMetadata().containsKey(META_AVAILABLE_CREDIT) ) {
						Object availCreditVal = e.getMetadata().get(META_AVAILABLE_CREDIT);
						BigDecimal availCredit = (availCreditVal instanceof BigDecimal
								? (BigDecimal) availCreditVal
								: new BigDecimal(availCreditVal.toString()));
						e.getMetadata().put("localizedAvailableCredit",
								MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale,
										invoice.getCurrencyCode(), availCredit));
					}
					item = new InvoiceItemImpl(invoice, e);
				}
				return new net.solarnetwork.central.user.billing.support.LocalizedInvoiceItem(item,
						locale, desc);

			}).collect(toList());
		} else {
			invoiceItems = invoice.getItems().stream().sorted(DEFAULT_ITEM_ORDER).map(e -> {
				return new InvoiceItemImpl(invoice, e);
			}).collect(toList());
		}

		return new InvoiceImpl(invoice, invoiceItems);
	}
}
