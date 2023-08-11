/* ==================================================================
 * ServerConfigurationRowMapper.java - 6/08/2023 2:33:35 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.dnp3.domain.MeasurementType;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;

/**
 * Row mapper for {@link ServerMeasurementConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>server_id (BIGINT)</li>
 * <li>idx (INTEGER)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>node_id (LONG)</li>
 * <li>source_id (TEXT)</li>
 * <li>pname (TEXT)</li>
 * <li>mtype (CHARACTER)</li>
 * <li>dmult (NUMERIC)</li>
 * <li>doffset (NUMERIC)</li>
 * <li>dscale (INTEGER)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class ServerMeasurementConfigurationRowMapper
		implements RowMapper<ServerMeasurementConfiguration> {

	/** A default instance. */
	public static final RowMapper<ServerMeasurementConfiguration> INSTANCE = new ServerMeasurementConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public ServerMeasurementConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 * 
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public ServerMeasurementConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public ServerMeasurementConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long serverId = rs.getObject(++p, Long.class);
		Integer entityId = rs.getObject(++p, Integer.class);
		Timestamp ts = rs.getTimestamp(++p);
		ServerMeasurementConfiguration conf = new ServerMeasurementConfiguration(userId, serverId,
				entityId, ts.toInstant());
		conf.setModified(rs.getTimestamp(++p).toInstant());
		conf.setEnabled(rs.getObject(++p, Boolean.class));
		conf.setNodeId(rs.getObject(++p, Long.class));
		conf.setSourceId(rs.getString(++p));
		conf.setProperty(rs.getString(++p));
		try {
			conf.setType(MeasurementType.forCode(rs.getString(++p).charAt(0)));
		} catch ( IllegalArgumentException e ) {
			// ignore, move on
		}
		conf.setMultiplier(rs.getBigDecimal(++p));
		conf.setOffset(rs.getBigDecimal(++p));
		conf.setScale(rs.getObject(++p, Integer.class));
		return conf;
	}

}
