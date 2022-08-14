/* ==================================================================
 * DaoUserOscpBiz.java - 15/08/2022 10:40:52 am
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

package net.solarnetwork.central.user.oscp.biz.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;

/**
 * DAO implementation of {@link UserOscpBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserOscpBiz implements UserOscpBiz {

	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final AssetConfigurationDao assetDao;

	/**
	 * Constructor.
	 * 
	 * @param capacityProviderDao
	 *        the capacity provider DAO
	 * @param capcityOptimizerDao
	 *        the capacity optimizer DAO
	 * @param capacityGroupDao
	 *        the capacity group DAO
	 * @param assetDao
	 *        the asset DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserOscpBiz(CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityGroupConfigurationDao capacityGroupDao, AssetConfigurationDao assetDao) {
		super();
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.assetDao = requireNonNullArgument(assetDao, "assetDao");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityProviderConfiguration> capacityProvidersForUser(Long userId) {
		return capacityProviderDao.findAll(userId, null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityOptimizerConfiguration> capacityOptimizersForUser(Long userId) {
		return capacityOptimizerDao.findAll(userId, null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityGroupConfiguration> capacityGroupsForUser(Long userId) {
		return capacityGroupDao.findAll(userId, null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<AssetConfiguration> assetsForUser(Long userId) {
		return assetDao.findAll(userId, null);
	}

}
