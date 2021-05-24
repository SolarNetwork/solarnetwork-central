/* ==================================================================
 * LocalizedInvoiceUsageRecord.java - 23/05/2021 4:37:01 PM
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

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.InvoiceUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceUsageRecordInfo;

/**
 * Localized version of {@link InvoiceUsageRecord}
 * 
 * @param <T>
 *        the usage record type
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class LocalizedInvoiceUsageRecord<T>
		implements InvoiceUsageRecord<T>, LocalizedInvoiceUsageRecordInfo {

	private final String localizedDescription;
	private final InvoiceUsageRecord<T> usage;
	private final Locale locale;
	private final String currencyCode;

	/**
	 * Convenience builder.
	 * 
	 * @param <T>
	 *        the usage record type
	 * @param usage
	 *        the usage to localize
	 * @param locale
	 *        the locale to localize to
	 * @param currencyCode
	 *        the currency code
	 * @return the localized invoice
	 */
	public static <T> LocalizedInvoiceUsageRecord<T> of(InvoiceUsageRecord<T> usage, Locale locale,
			String currencyCode) {
		return new LocalizedInvoiceUsageRecord<>(usage, locale, currencyCode);
	}

	/**
	 * Constructor.
	 * 
	 * @param usage
	 *        the usage to localize
	 * @param locale
	 *        the locale to localize to
	 * @param currencyCode
	 *        the currency code
	 */
	public LocalizedInvoiceUsageRecord(InvoiceUsageRecord<T> usage, Locale locale, String currencyCode) {
		this(usage, locale, null, currencyCode);
	}

	/**
	 * Constructor.
	 * 
	 * @param usage
	 *        the usage to localize
	 * @param locale
	 *        the locale to localize to
	 * @param localizedDescription
	 *        the localized description
	 * @param currencyCode
	 *        the currency code
	 */
	public LocalizedInvoiceUsageRecord(InvoiceUsageRecord<T> usage, Locale locale,
			String localizedDescription, String currencyCode) {
		super();
		this.usage = usage;
		this.locale = locale;
		this.localizedDescription = localizedDescription;
		this.currencyCode = currencyCode;
	}

	@Override
	public T getUsageKey() {
		return usage.getUsageKey();
	}

	@Override
	public List<InvoiceItemUsageRecord> getUsageRecords() {
		return usage.getUsageRecords();
	}

	@Override
	public String getLocalizedDescription() {
		return localizedDescription;
	}

	@Override
	public List<LocalizedInvoiceItemUsageRecordInfo> getLocalizedUsageRecords() {
		List<InvoiceItemUsageRecord> recs = getUsageRecords();
		if ( recs == null ) {
			return null;
		} else if ( recs.isEmpty() ) {
			return Collections.emptyList();
		}
		return recs.stream().map(record -> {
			if ( record instanceof LocalizedInvoiceItemUsageRecordInfo ) {
				return (LocalizedInvoiceItemUsageRecordInfo) record;
			}
			return LocalizedInvoiceItemUsageRecord.of(record, locale, currencyCode);
		}).collect(Collectors.toList());
	}

}
