/* ==================================================================
 * BaseExternalSystemAndGroupConfigurationRowMapper.java - 8/09/2022 11:20:15 am
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
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemAndGroup;

/**
 * Row mapper for external system and group.
 * 
 * @author matt
 * @version 1.0
 */
public class ExternalSystemAndGroupConfigurationRowMapper<C extends BaseOscpExternalSystemConfiguration<C>>
		implements RowMapper<ExternalSystemAndGroup<C>> {

	private final RowMapper<C> configurationMapper;
	private final RowMapper<CapacityGroupConfiguration> groupMapper;

	/**
	 * Constructor.
	 * 
	 * @param configurationMapper
	 *        the configuration row mapper
	 * @param groupMapper
	 *        the group row mapper
	 */
	public ExternalSystemAndGroupConfigurationRowMapper(RowMapper<C> configurationMapper,
			RowMapper<CapacityGroupConfiguration> groupMapper) {
		super();
		this.configurationMapper = requireNonNullArgument(configurationMapper, "configurationMapper");
		this.groupMapper = requireNonNullArgument(groupMapper, "groupMapper");
	}

	@Override
	public ExternalSystemAndGroup<C> mapRow(ResultSet rs, int rowNum) throws SQLException {
		C config = configurationMapper.mapRow(rs, rowNum);
		CapacityGroupConfiguration group = groupMapper.mapRow(rs, rowNum);
		return new ExternalSystemAndGroup<>(config, group);
	}

}
