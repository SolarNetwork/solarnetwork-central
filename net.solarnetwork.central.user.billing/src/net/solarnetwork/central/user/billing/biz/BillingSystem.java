/* ==================================================================
 * BillingSystem.java - 25/08/2017 3:10:36 PM
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

package net.solarnetwork.central.user.billing.biz;

import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;

/**
 * API for interacting with a billing system.
 * 
 * @author matt
 * @version 1.0
 */
public interface BillingSystem {

	/**
	 * Get a unique system key for the accounting functions of this system.
	 * 
	 * @return a unique key
	 */
	String getAccountingSystemKey();

	/**
	 * Test if an accounting key is supported by this system.
	 * 
	 * @param key
	 *        the key to test
	 * @return {@literal true} if the key is supported
	 */
	boolean supportsAccountingSystemKey(String key);

	/**
	 * Get information about this system.
	 * 
	 * @param locale
	 *        the desired locale of the information, or {@literal null} for the
	 *        default locale
	 * @return the info
	 */
	BillingSystemInfo getInfo(Locale locale);

	/**
	 * Search for invoices.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 */
	FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max);
}
