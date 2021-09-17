/* ==================================================================
 * Invoice.java - 25/08/2017 2:30:33 PM
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
import java.time.YearMonth;
import java.util.List;
import net.solarnetwork.central.domain.Entity;

/**
 * API for an invoice.
 * 
 * @author matt
 * @version 1.3
 */
public interface Invoice extends Entity<String> {

	/**
	 * The invoice ID used for dry-run (draft) invoice generation.
	 * 
	 * @since 1.2
	 */
	String DRAFT_INVOICE_ID = "-23108249"; // -DRAFT base 36

	/**
	 * Test if this is a dry-run (draft) invoice.
	 * 
	 * @return {@literal true} if this is a draft invoice
	 */
	default boolean isDraft() {
		String id = getId();
		return DRAFT_INVOICE_ID.equals(id);
	}

	/**
	 * Get the month that represents the date range of this invoice, if the
	 * invoice represents a month period.
	 * 
	 * @return the month, or {@literal null} if the invoice is not for a month
	 *         period
	 * @since 1.2
	 */
	YearMonth getInvoiceMonth();

	/**
	 * Get the time zone this invoice was created in.
	 * 
	 * @return the time zone ID
	 */
	String getTimeZoneId();

	/**
	 * Get a reference invoice "number".
	 * 
	 * @return the invoice number
	 */
	String getInvoiceNumber();

	/**
	 * Get the amount charged on this invoice.
	 * 
	 * @return the amount
	 */
	BigDecimal getAmount();

	/**
	 * Get the current invoice balance (unpaid amount).
	 * 
	 * <p>
	 * If this is positive then the invoice has outstanding payment due.
	 * </p>
	 * 
	 * @return the invoice balance
	 */
	BigDecimal getBalance();

	/**
	 * Get the total amount of all tax invoice items.
	 * 
	 * @return the total tax amount
	 * @since 1.1
	 */
	BigDecimal getTaxAmount();

	/**
	 * Get the total amount of all credit invoice items.
	 * 
	 * @return the total credit amount, or {@literal null} if none
	 * @since 1.3
	 */
	BigDecimal getCreditAmount();

	/**
	 * Get the total amount of account credit remaining.
	 * 
	 * @return the total credit amount remaining, or {@literal null} if none
	 * @since 1.3
	 */
	BigDecimal getRemainingCreditAmount();

	/**
	 * Get the currency this invoice is in, as a string currency code like
	 * {@literal NZD} or {@literal USD}.
	 * 
	 * @return the currency code
	 */
	String getCurrencyCode();

	/**
	 * Get the invoice items.
	 * 
	 * @return the invoice items
	 */
	List<InvoiceItem> getInvoiceItems();

	/**
	 * Get the node usage records.
	 * 
	 * @return the records
	 * @since 1.2
	 */
	List<InvoiceUsageRecord<Long>> getNodeUsageRecords();

}
