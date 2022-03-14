/* ==================================================================
 * InvoiceItemUsageRecord.java - 30/08/2017 3:21:31 PM
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

/**
 * A usage record attached to an invoice item.
 * 
 * @author matt
 * @version 1.1
 */
public interface InvoiceItemUsageRecord {

	/**
	 * Get the usage unit type.
	 * 
	 * @return the usage unit type
	 */
	String getUnitType();

	/**
	 * Get the usage amount.
	 * 
	 * @return the amount
	 */
	BigDecimal getAmount();

	/**
	 * Get an associated usage cost.
	 * 
	 * @return the cost
	 * @since 1.1
	 */
	BigDecimal getCost();

	/**
	 * Get a break-down of this usage record into a list of tiers.
	 * 
	 * @return the usage tiers for this record, or an empty list if there are no
	 *         tiers
	 * @since 1.1
	 */
	List<NamedCost> getUsageTiers();

}
