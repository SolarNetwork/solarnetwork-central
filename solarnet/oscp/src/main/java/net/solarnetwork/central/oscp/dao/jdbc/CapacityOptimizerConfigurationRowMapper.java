/* ==================================================================
 * CapacityOptimizerConfigurationRowMapper.java - 12/08/2022 4:09:13 pm
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;

/**
 * Row mapper for {@link CapacityOptimizerConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is that of
 * {@link BaseExternalSystemConfigurationRowMapper} with the following
 * additions:
 * </p>
 * 
 * <ol>
 * <li>pub_in (BOOLEAN)</li>
 * <li>pub_flux (BOOLEAN)</li>
 * <li>source_id_tmpl (STRING)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityOptimizerConfigurationRowMapper
		extends BaseExternalSystemConfigurationRowMapper<CapacityOptimizerConfiguration> {

	/** A default instance. */
	public static final RowMapper<CapacityOptimizerConfiguration> INSTANCE = new CapacityOptimizerConfigurationRowMapper();

	/** The number of columns mapped by this mapper. */
	public static final int COLUMN_COUNT = 18;

	@Override
	protected CapacityOptimizerConfiguration createConfiguration(Long userId, Long entityId,
			Instant created) {
		return new CapacityOptimizerConfiguration(userId, entityId, created);
	}

	@Override
	protected void populateConfiguration(ResultSet rs, int rowNum, CapacityOptimizerConfiguration conf)
			throws SQLException {
		super.populateConfiguration(rs, rowNum, conf);
		conf.setPublishToSolarIn(rs.getBoolean(16));
		conf.setPublishToSolarFlux(rs.getBoolean(17));
		conf.setSourceIdTemplate(rs.getString(18));
	}

	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}

}
