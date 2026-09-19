/* ==================================================================
 * ChargePointCriteria.java - 16/11/2022 5:34:39 pm
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

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for connector related data.
 * 
 * @author matt
 * @version 1.2
 */
public interface ChargePointConnectorCriteria {

	/**
	 * Get the first connector ID.
	 * 
	 * <p>
	 * This returns the first available connector ID from the
	 * {@link #getConnectorIds()} array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first connector ID, or {@code null} if not available
	 */
	default @Nullable Integer getConnectorId() {
		final var array = getConnectorIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of connector IDs.
	 * 
	 * @return array of connector IDs (may be {@code null})
	 */
	Integer @Nullable [] getConnectorIds();

	/**
	 * Test if this filter has any connector criteria.
	 * 
	 * @return {@literal true} if the connector ID is non-null
	 */
	default boolean hasConnectorCriteria() {
		return getConnectorId() != null;
	}

	/**
	 * Get the first EVSE ID.
	 * 
	 * <p>
	 * This returns the first available EVSE ID from the {@link #getEvseIds()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first EVSE ID, or {@code null} if not available
	 * @since 1.2
	 */
	default @Nullable Integer getEvseId() {
		final var array = getEvseIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of EVSE IDs.
	 * 
	 * @return array of EVSE IDs (may be {@code null})
	 * @since 1.2
	 */
	Integer @Nullable [] getEvseIds();

	/**
	 * Test if this filter has any EVSE criteria.
	 * 
	 * @return {@literal true} if the EVSE ID is non-null
	 * @since 1.2
	 */
	default boolean hasEvseCriteria() {
		return getEvseId() != null;
	}

}
