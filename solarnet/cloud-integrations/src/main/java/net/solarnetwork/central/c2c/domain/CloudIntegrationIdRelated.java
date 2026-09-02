/* ==================================================================
 * CloudIntegrationIdRelated.java - 22/07/2026 4:28:35 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import net.solarnetwork.central.domain.EntityConstants;

/**
 * API for objects related to an {@link CloudIntegrationConfiguration} entity by
 * way of a configuration ID.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudIntegrationIdRelated {

	/**
	 * Get the integration ID.
	 *
	 * @return the integration ID
	 */
	Long getIntegrationId();

	/**
	 * Test if a datum stream ID is available.
	 *
	 * @return {@code true} if a datum stream ID is available
	 */
	default boolean hasIntegrationId() {
		return EntityConstants.isAssigned(getIntegrationId());
	}

	/**
	 * Get the integration ID.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasIntegrationId()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return the integration ID (presumed non-null)
	 */
	@SuppressWarnings("NullAway")
	default Long integrationId() {
		return getIntegrationId();
	}

}
