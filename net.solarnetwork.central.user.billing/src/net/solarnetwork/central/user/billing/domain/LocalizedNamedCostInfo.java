/* ==================================================================
 * LocalizedNamedCostInfo.java - 31/05/2021 4:27:31 PM
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

/**
 * API for named cost information that has been localized.
 * 
 * <p>
 * This API does not provide a way to localize a named cost instance. Rather, it
 * is a marker for an instance that has already been localized. This is designed
 * to support APIs that can localize objects based on a requested locale.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface LocalizedNamedCostInfo {

	/**
	 * Get a localized description of the named cost.
	 * 
	 * @return the description
	 */
	String getLocalizedDescription();

	/**
	 * Get the quantity, as a formatted and localized string.
	 * 
	 * @return the amount
	 */
	String getLocalizedQuantity();

	/**
	 * Get the cost, as a formatted and localized string.
	 * 
	 * @return the localized cost
	 */
	String getLocalizedCost();

	/**
	 * Get the effective rate, as a formatted and localized string.
	 * 
	 * @return the localized effective rate
	 */
	String getLocalizedEffectiveRate();

}
