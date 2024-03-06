/* ==================================================================
 * EndpointConfigurationDao.java - 21/02/2024 2:56:21 pm
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

package net.solarnetwork.central.din.dao;

import java.util.UUID;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link EndpointConfiguration} entities.
 *
 * @author matt
 * @version 1.1
 */
public interface EndpointConfigurationDao
		extends GenericCompositeKey2Dao<EndpointConfiguration, UserUuidPK, Long, UUID>,
		FilterableDao<EndpointConfiguration, UserUuidPK, EndpointFilter>,
		UserModifiableEnabledStatusDao<EndpointFilter> {

	/**
	 * Look up an endpoint for a specific endpoint ID.
	 *
	 * @param endpointId
	 *        the endpoint ID
	 * @return the configuration, or {@literal null} if not found
	 */
	EndpointConfiguration getForEndpointId(UUID endpointId);

}
