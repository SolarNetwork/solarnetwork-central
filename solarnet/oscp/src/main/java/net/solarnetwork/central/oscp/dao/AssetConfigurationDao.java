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

import java.util.Collection;
import java.util.List;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link AssetConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface AssetConfigurationDao
		extends GenericCompositeKey2Dao<AssetConfiguration, UserLongPK, Long, Long> {

	/**
	 * Find all assets for a user and capacity group.
	 * 
	 * <p>
	 * The {@code sortDescriptors} parameter can be {@literal null}, in which
	 * case the sort order is not defined and implementation specific.
	 * </p>
	 * 
	 * @param userId
	 *        the ID of the user to restrict the results to
	 * @param capacityGroupId
	 *        the ID of the capacity group to restrict the results to
	 * @param sorts
	 *        list of sort descriptors to sort the results by
	 * @return list of all assets, or empty list if none available
	 */
	Collection<AssetConfiguration> findAllForCapacityGroup(Long userId, Long capacityGroupId,
			List<SortDescriptor> sorts);

}
