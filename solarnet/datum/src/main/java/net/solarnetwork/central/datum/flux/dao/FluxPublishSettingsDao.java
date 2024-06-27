/* ==================================================================
 * FluxPublishSettingsDao.java - 26/06/2024 2:25:52 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.flux.dao;

import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;

/**
 * API for user SolarFlux publish settings.
 *
 * @author matt
 * @version 1.0
 */
public interface FluxPublishSettingsDao {

	/**
	 * Get the publish configuration to use for a given node and source ID
	 * combination.
	 *
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @return the configuration to use, never {@literal null}
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	FluxPublishSettings nodeSourcePublishConfiguration(Long userId, Long nodeId, String sourceId);

}
