/* ==================================================================
 * LocalizedInvoiceInfo.java - 30/08/2017 8:20:45 AM
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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * API for invoice information that has been localized.
 * 
 * <p>
 * This API does not provide a way to localize an invoice instance. Rather, it
 * is a marker for an instance that has already been localized. This is designed
 * to support APIs that can localize objects based on a requested locale.
 * </p>
 * 
 * @author matt
 * @version 1.5
 */
public interface LocalizedInvoiceInfo {

	/**
	 * Get the invoice date representing the date the invoice was generated, as
	 * a formatted and localized string.
	 * 
	 * @return the invoice creation date
	 */
	String getLocalizedDate();

	/**
	 * Get a localized date range representing the time period covered by the
	 * invoice, as a formatted and localized string.
	 * 
	 * @return the invoice date range
	 * @since 1.3
	 */
	String getLocalizedInvoiceDateRange();

	/**
	 * Get the amount charged on this invoice, as a formatted and localized
	 * string.
	 * 
	 * @return the amount
	 */
	String getLocalizedAmount();

	/**
	 * Get the current invoice balance (unpaid amount), as a formatted and
	 * localized string.
	 * 
	 * <p>
	 * If this is positive then the invoice has outstanding payment due.
	 * </p>
	 * 
	 * @return the invoice balance
	 */
	String getLocalizedBalance();

	/**
	 * Get the total non-tax amount (sum of all non-tax item amounts), as a
	 * formatted and localized string.
	 * 
	 * @return the formatted total non-tax amount
	 * @since 1.2
	 */
	String getLocalizedNonTaxAmount();

	/**
	 * Get the total tax amount (sum of all tax item amounts), as a formatted
	 * and localized string.
	 * 
	 * @return the formatted total tax amount
	 * @since 1.1
	 */
	String getLocalizedTaxAmount();

	/**
	 * Get the total amount of all credit invoice items, as a formatted and
	 * localized string..
	 * 
	 * @return the formatted total credit amount
	 * @since 1.3
	 */
	String getLocalizedCreditAmount();

	/**
	 * Get the total amount of account credit remaining, as a formatted and
	 * localized string..
	 * 
	 * @return the formatted total credit amount remaining
	 * @since 1.3
	 */
	String getLocalizedRemainingCreditAmount();

	/**
	 * Get the localized invoice items.
	 * 
	 * @return the localized items
	 */
	List<LocalizedInvoiceItemInfo> getLocalizedInvoiceItems();

	/**
	 * Get the non-tax localized invoice items.
	 * 
	 * @return the non-tax localized items
	 * @since 1.2
	 */
	List<LocalizedInvoiceItemInfo> getLocalizedNonTaxInvoiceItems();

	/**
	 * Get a set of localized tax invoice items that are grouped by their
	 * descriptions into aggregate items that sum the amount of the grouped
	 * items.
	 * 
	 * <p>
	 * This can be used to display a single line item of all similar tax items
	 * on the invoice. For example if there are two {@literal Sales Tax} items
	 * with respective amounts of {@literal 1.25} and {@literal 4.75} this
	 * method would return a single invoice item with an amount of
	 * {@literal 6.00}.
	 * </p>
	 * 
	 * @return tax invoice items grouped by description
	 * @since 1.1
	 */
	List<LocalizedInvoiceItemInfo> getLocalizedTaxInvoiceItemsGroupedByDescription();

	/**
	 * Get the localized invoice usage records.
	 * 
	 * @return the localized usage records
	 * @since 1.3
	 */
	List<LocalizedInvoiceUsageRecordInfo> getLocalizedInvoiceUsageRecords();

	/**
	 * Get the first available localized invoice usage record.
	 * 
	 * @return the first available usage record, or {@literal null}
	 * @since 1.3
	 */
	@JsonIgnore
	default LocalizedInvoiceUsageRecordInfo getFirstLocalizedInvoiceUsageRecord() {
		List<LocalizedInvoiceUsageRecordInfo> records = getLocalizedInvoiceUsageRecords();
		return (records != null && !records.isEmpty() ? records.get(0) : null);
	}

}
