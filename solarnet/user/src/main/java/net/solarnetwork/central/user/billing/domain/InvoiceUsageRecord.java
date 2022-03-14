/* ==================================================================
 * InvoiceUsageRecord.java - 23/05/2021 4:25:03 PM
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

package net.solarnetwork.central.user.billing.domain;

import java.util.List;

/**
 * A usage record associted with an invoice.
 * 
 * @param <T>
 *        the invoice key type
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface InvoiceUsageRecord<T> {

	/**
	 * Get a key for the list of associated usage records.
	 * 
	 * <p>
	 * A key might be a node ID, source ID, stream ID, etc.
	 * </p>
	 * 
	 * @return the usage key
	 */
	T getUsageKey();

	/**
	 * Get the usage records associated with the invoice.
	 * 
	 * @return the usage records
	 */
	List<InvoiceItemUsageRecord> getUsageRecords();

}
