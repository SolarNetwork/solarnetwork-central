/* ==================================================================
 * CapacityProviderDao.java - 23/08/2022 11:46:40 am
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

package net.solarnetwork.oscp.sim.cp.dao;

import java.util.UUID;
import net.solarnetwork.oscp.sim.cp.domain.SystemConfiguration;

/**
 * DAO API for Capacity Provider simulator.
 * 
 * @author matt
 * @version 1.0
 */
public interface CapacityProviderDao {

	/**
	 * Verify a request authorization token value.
	 * 
	 * @param reqToken
	 *        the request token
	 * @return the verified system's configuration
	 */
	SystemConfiguration verifyAuthToken(String reqToken);

	/**
	 * Get a system configuration.
	 * 
	 * @param id
	 *        the ID of the configuration to get
	 * @return
	 */
	SystemConfiguration systemConfiguration(UUID id);

	/**
	 * Save a system configuration.
	 * 
	 * @param conf
	 *        the configuration to save
	 */
	void saveSystemConfiguration(SystemConfiguration conf);

}
