/* ==================================================================
 * NamedCost.java - 31/05/2021 4:23:14 PM
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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * A named cost, such as a tier break-down within a usage item.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface NamedCost {

	/**
	 * Get the resource name.
	 * 
	 * @return the name, never {@literal null}
	 */
	String getName();

	/**
	 * Get the resource quantity.
	 * 
	 * @return the quantity, never {@literal null}
	 */
	BigInteger getQuantity();

	/**
	 * Get the cost.
	 * 
	 * @return the cost, never {@literal null}
	 */
	BigDecimal getCost();

	/**
	 * Get the effective rate, derived from the quantity and cost.
	 * 
	 * <p>
	 * If {@code quantity} is {@literal 0} then {@code cost} is returned.
	 * </p>
	 * 
	 * @return the effective rate
	 */
	default BigDecimal getEffectiveRate() {
		BigInteger quantity = getQuantity();
		if ( BigInteger.ZERO.compareTo(quantity) == 0 ) {
			return getCost();
		}
		return getCost().divide(new BigDecimal(quantity));
	}

}
