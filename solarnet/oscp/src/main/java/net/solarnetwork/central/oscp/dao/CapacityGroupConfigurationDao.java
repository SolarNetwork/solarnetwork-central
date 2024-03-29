/* ==================================================================
 * CapacityOptimizerConfigurationDao.java - 14/08/2022 7:32:46 am
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

import java.time.Instant;
import java.util.Collection;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * DAO API for {@link CapacityGroupConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface CapacityGroupConfigurationDao
		extends GenericCompositeKey2Dao<CapacityGroupConfiguration, UserLongCompositePK, Long, Long> {

	/**
	 * Find a group for a given capacity provider and group identifier.
	 * 
	 * @param userId
	 *        the ID of the user to get the group for
	 * @param capacityProviderId
	 *        the ID of the Capacity Provider to get the group for
	 * @param groupIdentifier
	 *        the identifier of the group to get
	 * @return the configuration, or {@literal null} if not found
	 */
	CapacityGroupConfiguration findForCapacityProvider(Long userId, Long capacityProviderId,
			String groupIdentifier);

	/**
	 * Find all groups for a given capacity provider.
	 * 
	 * @param userId
	 *        the ID of the user to get the group for
	 * @param capacityProviderId
	 *        the ID of the Capacity Provider to get the group for
	 * @return the configurations, never {@literal null}
	 */
	Collection<CapacityGroupConfiguration> findAllForCapacityProvider(Long userId,
			Long capacityProviderId);

	/**
	 * Find a group for a given capacity optimizer and group identifier.
	 * 
	 * @param userId
	 *        the ID of the user to get the group for
	 * @param capacityOptimizerId
	 *        the ID of the Capacity Optimizer to get the group for
	 * @param groupIdentifier
	 *        the identifier of the group to get
	 * @return the configuration, or {@literal null} if not found
	 */
	CapacityGroupConfiguration findForCapacityOptimizer(Long userId, Long capacityOptimizerId,
			String groupIdentifier);

	/**
	 * Find all groups for a given capacity optimizer.
	 * 
	 * @param userId
	 *        the ID of the user to get the group for
	 * @param capacityOptimizerId
	 *        the ID of the Capacity Optimizer to get the group for
	 * @return the configurations, never {@literal null}
	 */
	Collection<CapacityGroupConfiguration> findAllForCapacityOptimizer(Long userId,
			Long capacityOptimizerId);

	/**
	 * Compare and update the measurement date.
	 * 
	 * @param id
	 *        the primary key to save the settings for
	 * @param role
	 *        either {@link OscpRole#CapacityProvider} or
	 *        {@link OscpRole#CapacityOptimizer}
	 * @param expected
	 *        the expected value
	 * @param ts
	 *        the timestamp to set of {@code expected} matches the current value
	 */
	boolean compareAndSetMeasurement(UserLongCompositePK id, OscpRole role, Instant expected,
			Instant ts);

}
