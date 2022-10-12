/* ==================================================================
 * ExternalSystemAuthTokenDao.java - 16/08/2022 4:08:49 pm
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

package net.solarnetwork.central.oscp.dao;

import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * API for a DAO that manages external system authorization tokens.
 * 
 * <p>
 * This API is used to manage tokens external system configurations, like
 * Capacity Provider and Capacity Optimizer configurations. The tokens are
 * provided by the external systems themselves during the Register API flow. The
 * tokens are not meant to be shown to humans; they are required when
 * SolarNetwork makes OSCP API requests to the external system.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemAuthTokenDao {

	/**
	 * Save an external authorization token for a given configuration ID.
	 * 
	 * <p>
	 * Calling this will replace any existing token for the given
	 * {@code configurationId}, or create a new one if none already exists.
	 * </p>
	 * 
	 * @param configurationId
	 *        the ID of the external system configuration to save the
	 *        authorization token for, e.g. a Capacity Provider or Capacity
	 *        Optimizer
	 * @param token
	 *        the authorization token to save
	 */
	void saveExternalSystemAuthToken(UserLongCompositePK configurationId, String token);

	/**
	 * Get an external authorization token for a given configuration ID.
	 * 
	 * @param configurationId
	 *        the ID of the external system configuration to get the
	 *        authorization token for
	 * @return the authorization token, or {@literal null} if one is not
	 *         available
	 */
	String getExternalSystemAuthToken(UserLongCompositePK configurationId);
}
