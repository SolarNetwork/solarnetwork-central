/* ==================================================================
 * JdbcExternalSystemSupportDao.java - 21/08/2022 4:12:29 pm
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

package net.solarnetwork.central.oscp.dao.jdbc;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * JDBC implementation of {@link ExternalSystemSupportDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcExternalSystemSupportDao implements ExternalSystemSupportDao {

	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;

	/**
	 * Constructor.
	 * 
	 * @param capacityProviderDao
	 *        the capacity provider DAO
	 * @param capacityOptimizerDao
	 *        the capacity optimizer DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public JdbcExternalSystemSupportDao(CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao) {
		super();
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
	}

	@Override
	public ExternalSystemConfiguration externalSystemConfiguration(OscpRole role,
			UserLongCompositePK id) {
		return switch (role) {
			case CapacityProvider -> capacityProviderDao.get(id);
			case CapacityOptimizer -> capacityOptimizerDao.get(id);
			default -> throw new UnsupportedOperationException("Role %s not supported".formatted(role));
		};
	}

}
