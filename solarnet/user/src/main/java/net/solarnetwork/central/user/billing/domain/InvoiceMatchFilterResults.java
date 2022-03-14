/* ==================================================================
 * InvoiceMatchFilterResults.java - 29/05/2018 1:20:19 PM
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

import java.math.BigDecimal;
import net.solarnetwork.central.domain.FilterResults;

/**
 * API for a set of invoices returned from a filter query.
 * 
 * @author matt
 * @version 1.0
 */
public interface InvoiceMatchFilterResults extends FilterResults<InvoiceMatch> {

	/**
	 * Get the total amount charged across all invoices.
	 * 
	 * @return the amount
	 */
	BigDecimal getTotalAmount();

	/**
	 * Get the current total balance (unpaid amount) across all invoices.
	 * 
	 * <p>
	 * If this is positive then the invoice has outstanding payment due.
	 * </p>
	 * 
	 * @return the invoice balance
	 */
	BigDecimal getTotalBalance();

	/**
	 * Get the total amount of all tax invoice items in all invoices.
	 * 
	 * @return the total tax amount
	 */
	BigDecimal getTotalTaxAmount();

	/**
	 * Get the currency the amounts returned by other methods in this API is in,
	 * as a string currency code like {@literal NZD} or {@literal USD}.
	 * 
	 * @return the currency code
	 */
	String getCurrencyCode();

}
