/* ==================================================================
 * LocalizedInvoice.java - 30/08/2017 7:24:09 AM
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.javax.money.MoneyUtils;

/**
 * Localized version of {@link Invoice}.
 * 
 * @author matt
 * @version 1.0
 */
public class LocalizedInvoice implements Invoice, LocalizedInvoiceInfo {

	private final Invoice invoice;
	private final Locale locale;

	/**
	 * Convenience builder.
	 * 
	 * @param invoice
	 *        the invoice to localize
	 * @param locale
	 *        the locale to localize to
	 * @return the localized invoice
	 */
	public static LocalizedInvoice of(Invoice invoice, Locale locale) {
		return new LocalizedInvoice(invoice, locale);
	}

	/**
	 * Constructor.
	 * 
	 * @param invoice
	 *        the invoice to localize
	 * @param locale
	 *        the locale to localize to
	 */
	public LocalizedInvoice(Invoice invoice, Locale locale) {
		super();
		this.invoice = invoice;
		this.locale = locale;
	}

	@Override
	public String getLocalizedDate() {
		DateTimeFormatter fmt = DateTimeFormat.fullDate().withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(DateTimeZone.forID(tz));
		}
		return fmt.print(getCreated());
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
	public String getLocalizedTaxAmount() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getTaxAmount());
	}

	@Override
	public DateTime getCreated() {
		return invoice.getCreated();
	}

	@Override
	public String getTimeZoneId() {
		return invoice.getTimeZoneId();
	}

	@Override
	public String getInvoiceNumber() {
		return invoice.getInvoiceNumber();
	}

	@Override
	public BigDecimal getAmount() {
		return invoice.getAmount();
	}

	@Override
	public String getId() {
		return invoice.getId();
	}

	@Override
	public BigDecimal getBalance() {
		return invoice.getBalance();
	}

	@Override
	public String getCurrencyCode() {
		return invoice.getCurrencyCode();
	}

	@Override
	public int compareTo(String o) {
		return invoice.compareTo(o);
	}

	@Override
	public List<InvoiceItem> getInvoiceItems() {
		return invoice.getInvoiceItems();
	}

	@Override
	public BigDecimal getTaxAmount() {
		return invoice.getTaxAmount();
	}

	@Override
	public List<LocalizedInvoiceItemInfo> getLocalizedInvoiceItems() {
		List<InvoiceItem> items = getInvoiceItems();
		if ( items == null ) {
			return null;
		} else if ( items.isEmpty() ) {
			return Collections.emptyList();
		}
		return items.stream().map(item -> {
			if ( item instanceof LocalizedInvoiceItemInfo ) {
				return (LocalizedInvoiceItemInfo) item;
			}
			return new LocalizedInvoiceItem(item, locale);
		}).collect(Collectors.toList());
	}

	private Stream<InvoiceItem> getTaxInvoiceItemsStream() {
		List<InvoiceItem> items = getInvoiceItems();
		if ( items == null ) {
			items = Collections.emptyList();
		}
		return items.stream().filter(item -> InvoiceItem.TYPE_TAX.equals(item.getItemType()));
	}

	@Override
	public List<LocalizedInvoiceItemInfo> getLocalizedTaxInvoiceItemsGroupedByDescription() {
		List<InvoiceItem> taxItems = getTaxInvoiceItemsStream().collect(Collectors.toList());
		if ( taxItems.isEmpty() ) {
			return null;
		}

		// maintain ordering based on original invoice items
		List<String> ordering = taxItems.stream().map(item -> item.getId()).collect(Collectors.toList());

		// return list of AggregateInvoiceItem, grouped by InvoiceItem::getDescription
		return taxItems.stream()
				.collect(Collectors.groupingBy(InvoiceItem::getDescription,
						Collector.of(AggregateLocalizedInvoiceItem.itemOfLocale(locale),
								(agg, item) -> agg.addItem(item), (agg1, agg2) -> {
									return agg1.addItems(agg2);
								})))
				.values().stream().sorted(Comparator.comparing(item -> ordering.indexOf(item.getId())))
				.collect(Collectors.toList());
	}

}
