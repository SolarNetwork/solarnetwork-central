/* ==================================================================
 * ChargeSessionTransactionCriteria.java - 10/12/2022 11:09:38 am
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

/**
 * Search criteria for charge session transaction related data.
 * 
 * @author matt
 * @version 1.0
 */
public interface ChargeSessionTransactionCriteria {

	/**
	 * Get the first transaction ID.
	 * 
	 * <p>
	 * This returns the first available transaction ID from the
	 * {@link #getTransactionIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first transaction ID, or {@literal null} if not available
	 */
	Integer getTransactionId();

	/**
	 * Get an array of transaction IDs.
	 * 
	 * @return array of transaction IDs (may be {@literal null})
	 */
	Integer[] getTransactionIds();

	/**
	 * Test if this filter has any transaction criteria.
	 * 
	 * @return {@literal true} if the transaction ID is non-null
	 */
	default boolean hasTransactionCriteria() {
		return getTransactionId() != null;
	}

}
