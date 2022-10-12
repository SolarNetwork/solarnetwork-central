/* ==================================================================
 * CapacityProviderConfigurationRowMapper.java - 12/08/2022 4:09:13 pm
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

import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;

/**
 * Row mapper for {@link CapacityProviderConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityProviderConfigurationRowMapper
		extends BaseExternalSystemConfigurationRowMapper<CapacityProviderConfiguration> {

	/** A default instance. */
	public static final RowMapper<CapacityProviderConfiguration> INSTANCE = new CapacityProviderConfigurationRowMapper();

	@Override
	protected CapacityProviderConfiguration createConfiguration(Long userId, Long entityId,
			Instant created) {
		return new CapacityProviderConfiguration(userId, entityId, created);
	}

}
