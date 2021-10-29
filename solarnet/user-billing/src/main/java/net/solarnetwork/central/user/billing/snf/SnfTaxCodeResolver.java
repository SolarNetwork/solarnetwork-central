/* ==================================================================
 * SnfTaxCodeResolver.java - 24/07/2020 9:27:21 AM
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf;

import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;

/**
 * API for resolving a filter of applicable tax codes for a given invoice.
 * 
 * @author matt
 * @version 1.0
 */
public interface SnfTaxCodeResolver {

	/**
	 * Resolve a filter for tax codes for a given invoice.
	 * 
	 * <p>
	 * The returned filter should at a minimum contain the proper {@code zones}
	 * and {@code date} properties that apply to the given invoice. If either of
	 * this properties are {@code null} or empty then no tax codes apply to the
	 * given invoice.
	 * </p>
	 * 
	 * @param invoice
	 *        the invoice to resolve taxes for; this will be fully populated
	 *        with an appropriate account, date range, and item details
	 * @return the filter to use to resolve tax codes with
	 */
	TaxCodeFilter taxCodeFilterForInvoice(SnfInvoice invoice);

}
