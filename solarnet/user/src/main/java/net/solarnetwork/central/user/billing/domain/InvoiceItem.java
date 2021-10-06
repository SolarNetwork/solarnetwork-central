/* ==================================================================
 * InvoiceItem.java - 30/08/2017 7:26:58 AM
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

package net.solarnetwork.central.user.billing.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import net.solarnetwork.dao.Entity;

/**
 * A line item on an invoice.
 * 
 * <p>
 * The {@link #getCreated()} value on this API represents the start date of the
 * invoice item, i.e. the time-zone specific equivalent of
 * {@link #getStartDate()}.
 * </p>
 * 
 * @author matt
 * @version 1.3
 */
public interface InvoiceItem extends Entity<String> {

	/**
	 * The invoice item type for tax items.
	 * 
	 * @since 1.1
	 */
	public static final String TYPE_TAX = "TAX";

	/**
	 * The invoice item type for credit items.
	 * 
	 * @since 1.3
	 */
	public static final String TYPE_CREDIT = "CREDIT";

	/**
	 * Get metadata associated with this item.
	 * 
	 * @return the metadata, or {@literal null} if none
	 */
	Map<String, Object> getMetadata();

	/**
	 * Get the time zone of the invoice item.
	 * 
	 * @return the time zone ID
	 */
	String getTimeZoneId();

	/**
	 * Get the plan name.
	 * 
	 * @return the plan name
	 */
	String getPlanName();

	/**
	 * Get the item type.
	 * 
	 * @return the item type
	 */
	String getItemType();

	/**
	 * Get the description.
	 * 
	 * @return the description
	 */
	String getDescription();

	/**
	 * Get the start date.
	 * 
	 * @return the start date
	 */
	LocalDate getStartDate();

	/**
	 * Get the ended date.
	 * 
	 * @return the ended date
	 */
	LocalDate getEndDate();

	/**
	 * Get the amount.
	 * 
	 * @return the amount
	 */
	BigDecimal getAmount();

	/**
	 * Get the currency code.
	 * 
	 * @return the currencyCode
	 */
	String getCurrencyCode();

	/**
	 * Get the item ended date (with time zone).
	 * 
	 * @return the ended date
	 */
	DateTime getEnded();

	/**
	 * Get the total usage amount, as a formatted and localized string.
	 * 
	 * @return the formatted usage amount
	 * @since 1.1
	 */
	BigDecimal getTotalUsageAmount();

	/**
	 * Get any usage records associated with this invoice.
	 * 
	 * @return the usage records
	 */
	List<InvoiceItemUsageRecord> getItemUsageRecords();

}
