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

import static net.solarnetwork.central.user.billing.support.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceUsageRecordInfo;
import net.solarnetwork.domain.Identity;

/**
 * Localized version of {@link Invoice}.
 *
 * @author matt
 * @version 2.0
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
		DateTimeFormatter fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(ZoneId.of(tz));
		}
		return fmt.format(getCreated());
	}

	@Override
	public String getLocalizedInvoiceDateRange() {
		YearMonth month = invoice.getInvoiceMonth();
		if ( month == null ) {
			// no range available
			return null;
		}
		// @formatter:off
		DateTimeFormatter fmt = new DateTimeFormatterBuilder()
				.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
				.appendLiteral(' ')
				.appendValue(ChronoField.YEAR)
				.toFormatter(locale);
		// @formatter:on
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(ZoneId.of(tz));
		}
		return fmt.format(month);
	}

	@Override
	public String getLocalizedAmount() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(), getAmount());
	}

	@Override
	public String getLocalizedBalance() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getBalance());
	}

	@Override
	public String getLocalizedNonTaxAmount() {
		BigDecimal taxAmount = getTaxAmount();
		BigDecimal total = getAmount();
		BigDecimal credits = getCreditAmount();
		BigDecimal nonTaxAmount = total.subtract(taxAmount);
		if ( credits != null ) {
			nonTaxAmount = nonTaxAmount.subtract(credits);
		}
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				nonTaxAmount);
	}

	@Override
	public String getLocalizedTaxAmount() {
		return formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getTaxAmount());
	}

	@Override
	public BigDecimal getCreditAmount() {
		return invoice.getCreditAmount();
	}

	@Override
	public BigDecimal getRemainingCreditAmount() {
		return invoice.getRemainingCreditAmount();
	}

	@Override
	public String getLocalizedCreditAmount() {
		BigDecimal d = getCreditAmount();
		return (d != null
				? formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(), d)
				: null);
	}

	@Override
	public String getLocalizedRemainingCreditAmount() {
		BigDecimal d = getRemainingCreditAmount();
		return (d != null
				? formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(), d)
				: null);
	}

	@Override
	public Instant getCreated() {
		return invoice.getCreated();
	}

	@Override
	public YearMonth getInvoiceMonth() {
		return invoice.getInvoiceMonth();
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
	public List<InvoiceUsageRecord<Long>> getNodeUsageRecords() {
		return invoice.getNodeUsageRecords();
	}

	@Override
	public List<LocalizedInvoiceItemInfo> getLocalizedInvoiceItems() {
		List<InvoiceItem> items = getInvoiceItems();
		if ( items == null ) {
			return null;
		} else if ( items.isEmpty() ) {
			return Collections.emptyList();
		}
		return localizedItems(items.stream());
	}

	private Stream<InvoiceItem> getTaxInvoiceItemsStream() {
		List<InvoiceItem> items = getInvoiceItems();
		if ( items == null ) {
			items = Collections.emptyList();
		}
		return items.stream().filter(item -> InvoiceItem.TYPE_TAX.equals(item.getItemType()));
	}

	private Stream<InvoiceItem> getNonTaxInvoiceItemsStream() {
		List<InvoiceItem> items = getInvoiceItems();
		if ( items == null ) {
			items = Collections.emptyList();
		}
		return items.stream().filter(item -> !InvoiceItem.TYPE_TAX.equals(item.getItemType())
				&& !InvoiceItem.TYPE_CREDIT.equals(item.getItemType()));
	}

	private List<LocalizedInvoiceItemInfo> localizedItems(Stream<InvoiceItem> items) {
		return items.map(item -> {
			if ( item instanceof LocalizedInvoiceItemInfo ) {
				return (LocalizedInvoiceItemInfo) item;
			}
			return new LocalizedInvoiceItem(item, locale);
		}).collect(Collectors.toList());
	}

	@Override
	public List<LocalizedInvoiceItemInfo> getLocalizedNonTaxInvoiceItems() {
		return localizedItems(getNonTaxInvoiceItemsStream());
	}

	@Override
	public List<LocalizedInvoiceItemInfo> getLocalizedTaxInvoiceItemsGroupedByDescription() {
		List<InvoiceItem> taxItems = getTaxInvoiceItemsStream().toList();
		if ( taxItems.isEmpty() ) {
			return null;
		}

		// maintain ordering based on original invoice items
		List<String> ordering = taxItems.stream().map(Identity::getId).toList();

		// return list of AggregateInvoiceItem, grouped by InvoiceItem::getDescription
		return taxItems.stream()
				.collect(Collectors.groupingBy(InvoiceItem::getDescription,
						Collector.of(AggregateLocalizedInvoiceItem.itemOfLocale(locale),
								AggregateLocalizedInvoiceItem::addItem,
								AggregateLocalizedInvoiceItem::addItems)))
				.values().stream().sorted(Comparator.comparing(item -> ordering.indexOf(item.getId())))
				.collect(Collectors.toList());
	}

	@Override
	public List<LocalizedInvoiceUsageRecordInfo> getLocalizedInvoiceUsageRecords() {
		List<InvoiceUsageRecord<Long>> recs = getNodeUsageRecords();
		if ( recs == null ) {
			return null;
		}
		return recs.stream().map(r -> LocalizedInvoiceUsageRecord.of(r, locale, getCurrencyCode()))
				.collect(Collectors.toList());
	}

}
