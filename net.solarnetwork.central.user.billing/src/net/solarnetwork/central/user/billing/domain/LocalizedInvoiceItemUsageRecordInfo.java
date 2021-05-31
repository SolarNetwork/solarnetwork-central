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

import java.util.ArrayList;
import java.util.List;

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
	 * @since 1.1
	 */
	String getLocalizedCost();

	/**
	 * Get a break-down of this usage record into a list of tiers.
	 * 
	 * @return the localized usage tiers for this record, or an empty list if
	 *         there are no tiers
	 * @since 1.1
	 */
	List<LocalizedNamedCostInfo> getLocalizedUsageTiers();

	/**
	 * Get the first available localized usage tier.
	 * 
	 * @return the first available localized usage tier, or {@literal null}
	 */
	default LocalizedNamedCostInfo getFirstLocalizedUsageTier() {
		List<LocalizedNamedCostInfo> tiers = getLocalizedUsageTiers();
		return (tiers != null && !tiers.isEmpty() ? tiers.get(0) : null);
	}

	/**
	 * Get all the available localized usage tiers after the first one.
	 * 
	 * @return the localized usage tiers other than the first, or
	 *         {@literal null}
	 */
	default List<LocalizedNamedCostInfo> getLocalizedUsageTiersAfterFirst() {
		List<LocalizedNamedCostInfo> tiers = getLocalizedUsageTiers();
		int size = (tiers != null ? tiers.size() : 0);
		if ( size < 2 ) {
			return null;
		}
		List<LocalizedNamedCostInfo> result = new ArrayList<>(size - 1);
		for ( int i = 1; i < size; i++ ) {
			result.add(tiers.get(i));
		}
		return result;
	}

}
