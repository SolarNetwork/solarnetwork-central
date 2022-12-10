/* ==================================================================
 * ChargeSessionEndReasonCriteria.java - 10/12/2022 2:29:32 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.ocpp.domain.ChargeSessionEndReason;

/**
 * Search criteria for charge session end reasons.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionEndReasonCriteria {

	/**
	 * Get the first end reason.
	 * 
	 * <p>
	 * This returns the first available end reason from the
	 * {@link #getEndReasons()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the first end reason, or {@literal null} if not available
	 */
	ChargeSessionEndReason getEndReason();

	/**
	 * Get an array of end reasons.
	 * 
	 * @return array of end reasons (may be {@literal null})
	 */
	ChargeSessionEndReason[] getEndReasons();

	/**
	 * Get an array of end reason code values.
	 * 
	 * @return array of end reason code values (may be {@literal null})
	 */
	@JsonIgnore
	default Integer[] getEndReasonCodes() {
		final ChargeSessionEndReason[] a = getEndReasons();
		int len = (a != null ? a.length : 0);
		if ( len < 1 ) {
			return null;
		}
		Integer[] codes = new Integer[len];
		for ( int i = 0; i < len; i++ ) {
			codes[i] = a[i].getCode();
		}
		return codes;
	}

	/**
	 * Test if this filter has any end reason criteria.
	 * 
	 * @return {@literal true} if the end reason is non-null
	 */
	default boolean hasChargeSessionEndReasonCriteria() {
		return getEndReason() != null;
	}

}
