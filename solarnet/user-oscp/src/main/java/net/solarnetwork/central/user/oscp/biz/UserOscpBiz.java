/* ==================================================================
 * UserOscpBiz.java - 15/08/2022 10:33:25 am
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

package net.solarnetwork.central.user.oscp.biz;

import java.util.Collection;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;

/**
 * Service API for SolarUser OSCP support.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserOscpBiz {

	/**
	 * List the available capacity provider configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityProviderConfiguration> capacityProvidersForUser(Long userId);

	/**
	 * List the available capacity optimizer configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityOptimizerConfiguration> capacityOptimizersForUser(Long userId);

	/**
	 * List the available capacity group configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<CapacityGroupConfiguration> capacityGroupsForUser(Long userId);

	/**
	 * List the available asset configurations for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get configurations for
	 * @return all available configurations; never {@literal null}
	 */
	Collection<AssetConfiguration> assetsForUser(Long userId);

}
