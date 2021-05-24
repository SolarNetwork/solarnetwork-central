/* ==================================================================
 * LocalizedInvoiceItemUsageRecordInfo.java - 30/08/2017 3:22:54 PM
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

/**
 * API for invoice item information that has been localized.
 * 
 * <p>
 * This API does not provide a way to localize an invoice instance. Rather, it
 * is a marker for an instance that has already been localized. This is designed
 * to support APIs that can localize objects based on a requested locale.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface LocalizedInvoiceItemUsageRecordInfo {

	/**
	 * Get a localized unit type of this usage record.
	 * 
	 * @return the localized unit type
	 */
	String getLocalizedUnitType();

	/**
	 * Get the usage amount, as a formatted and localized string.
	 * 
	 * @return the amount
	 */
	String getLocalizedAmount();

	/**
	 * Get the usage cost, as a formatted and localized string.
	 * 
	 * @return the usage cost
	 */
	String getLocalizedCost();

}
