/* ==================================================================
 * LocalizedInvoiceMatchFilterResultsInfo.java - 29/05/2018 1:25:02 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

/**
 * API for a set of invoice information that has been localized.
 * 
 * <p>
 * This API does not provide a way to localize an invoice instance. Rather, it
 * is a marker for an instance that has already been localized. This is designed
 * to support APIs that can localize objects based on a requested locale.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface LocalizedInvoiceMatchFilterResultsInfo {

	/**
	 * Get the total amount charged across all invoices, as a formatted and
	 * localized string.
	 * 
	 * @return the formatted total amount
	 */
	String getLocalizedTotalAmount();

	/**
	 * Get the current total balance (unpaid amount) across all invoices, as a
	 * formatted and localized string.
	 * 
	 * <p>
	 * If this is positive then overall an outstanding payment is due.
	 * </p>
	 * 
	 * @return the formatted total balance
	 */
	String getLocalizedTotalBalance();

	/**
	 * Get the total tax amount (sum of all tax item amounts) across all
	 * invoices, as a formatted and localized string.
	 * 
	 * @return the formatted total tax amount
	 */
	String getLocalizedTotalTaxAmount();

	/**
	 * Get the localized invoices.
	 * 
	 * @return the localized invoices
	 */
	Iterable<LocalizedInvoiceMatchInfo> getLocalizedInvoices();

}
