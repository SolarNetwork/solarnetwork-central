/* ==================================================================
 * LocalizedInvoiceItem.java - 30/08/2017 8:24:31 AM
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
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;
import net.solarnetwork.javax.money.MoneyUtils;

/**
 * Localized version of {@link InvoiceItem}.
 * 
 * @author matt
 * @version 1.0
 */
public class LocalizedInvoiceItem implements InvoiceItem, LocalizedInvoiceItemInfo {

	private final InvoiceItem item;
	private final Locale locale;

	/**
	 * Convenience builder.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 * @return the localized invoice
	 */
	public static LocalizedInvoiceItem of(InvoiceItem item, Locale locale) {
		return new LocalizedInvoiceItem(item, locale);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 */
	public LocalizedInvoiceItem(InvoiceItem item, Locale locale) {
		super();
		this.item = item;
		this.locale = locale;
	}

	@Override
	public String getLocalizedStartDate() {
		DateTimeFormatter fmt = DateTimeFormat.mediumDate().withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(DateTimeZone.forID(tz));
		}
		return fmt.print(getCreated());
	}

	@Override
	public String getLocalizedEndDate() {
		DateTimeFormatter fmt = DateTimeFormat.mediumDate().withLocale(locale);
		String tz = getTimeZoneId();
		if ( tz != null ) {
			fmt = fmt.withZone(DateTimeZone.forID(tz));
		}
		return fmt.print(getEnded());
	}

	@Override
	public String getLocalizedAmount() {
		return MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, getCurrencyCode(),
				getAmount());
	}

	@Override
	public DateTime getCreated() {
		return item.getCreated();
	}

	@Override
	public String getTimeZoneId() {
		return item.getTimeZoneId();
	}

	@Override
	public String getId() {
		return item.getId();
	}

	@Override
	public String getPlanName() {
		return item.getPlanName();
	}

	@Override
	public String getItemType() {
		return item.getItemType();
	}

	@Override
	public String getDescription() {
		return item.getDescription();
	}

	@Override
	public LocalDate getStartDate() {
		return item.getStartDate();
	}

	@Override
	public LocalDate getEndDate() {
		return item.getEndDate();
	}

	@Override
	public BigDecimal getAmount() {
		return item.getAmount();
	}

	@Override
	public String getCurrencyCode() {
		return item.getCurrencyCode();
	}

	@Override
	public DateTime getEnded() {
		return item.getEnded();
	}

	@Override
	public int compareTo(String o) {
		return item.compareTo(o);
	}

	@Override
	public List<InvoiceItemUsageRecord> getItemUsageRecords() {
		return item.getItemUsageRecords();
	}

	@Override
	public List<LocalizedInvoiceItemUsageRecordInfo> getLocalizedInvoiceItemUsageRecords() {
		List<InvoiceItemUsageRecord> recs = getItemUsageRecords();
		if ( recs == null ) {
			return null;
		} else if ( recs.isEmpty() ) {
			return Collections.emptyList();
		}
		return recs.stream().map(record -> {
			if ( record instanceof LocalizedInvoiceItemUsageRecordInfo ) {
				return (LocalizedInvoiceItemUsageRecordInfo) record;
			}
			return new LocalizedInvoiceItemUsageRecord(record, locale);
		}).collect(Collectors.toList());
	}

}
