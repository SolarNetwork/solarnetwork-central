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
import java.util.Locale;
import net.solarnetwork.central.user.billing.domain.InvoiceItemUsageRecord;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemUsageRecordInfo;

/**
 * Localized version of {@link InvoiceItemUsageRecord}.
 * 
 * @author matt
 * @version 1.0
 */
public class LocalizedInvoiceItemUsageRecord
		implements InvoiceItemUsageRecord, LocalizedInvoiceItemUsageRecordInfo {

	private final String localizedUnitType;
	private final InvoiceItemUsageRecord item;
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
	public static LocalizedInvoiceItemUsageRecord of(InvoiceItemUsageRecord item, Locale locale) {
		return new LocalizedInvoiceItemUsageRecord(item, locale, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param item
	 *        the item to localize
	 * @param locale
	 *        the locale to localize to
	 */
	public LocalizedInvoiceItemUsageRecord(InvoiceItemUsageRecord item, Locale locale,
			String localizedUnitType) {
		super();
		this.item = item;
		this.locale = locale;
		this.localizedUnitType = localizedUnitType;
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
	public String getUnitType() {
		return item.getUnitType();
	}

	@Override
	public BigDecimal getAmount() {
		return item.getAmount();
	}

}
