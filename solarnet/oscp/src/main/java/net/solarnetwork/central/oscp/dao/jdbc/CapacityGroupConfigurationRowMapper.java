/* ==================================================================
 * CapacityGroupConfigurationRowMapper.java - 12/08/2022 4:09:13 pm
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
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.codec.JsonUtils;

/**
 * Row mapper for {@link CapacityGroupConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>user_id (BIGINT)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>cname (TEXT)</li>
 * <li>ident (TEXT)</li>
 * <li>meas_secs (INTEGER)</li>
 * <li>cp_id (BIGINT)</li>
 * <li>co_id (BIGINT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 * 
 * 
 * @author matt
 * @version 1.0
 */
public class CapacityGroupConfigurationRowMapper implements RowMapper<CapacityGroupConfiguration> {

	/** A default instance. */
	public static final RowMapper<CapacityGroupConfiguration> INSTANCE = new CapacityGroupConfigurationRowMapper();

	@Override
	public CapacityGroupConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long entityId = rs.getLong(1);
		Timestamp created = rs.getTimestamp(2);
		Long userId = rs.getLong(4);
		CapacityGroupConfiguration conf = new CapacityGroupConfiguration(userId, entityId,
				created.toInstant());
		conf.setModified(rs.getTimestamp(3).toInstant());
		conf.setEnabled(rs.getBoolean(5));
		conf.setName(rs.getString(6));
		conf.setIdentifier(rs.getString(7));
		conf.setMeasurementPeriod(MeasurementPeriod.forCode(rs.getInt(8)));
		conf.setCapacityProviderId(rs.getLong(9));
		conf.setCapacityOptimizerId(rs.getLong(10));
		conf.setServiceProps(JsonUtils.getStringMap(rs.getString(11)));
		return conf;
	}

}
