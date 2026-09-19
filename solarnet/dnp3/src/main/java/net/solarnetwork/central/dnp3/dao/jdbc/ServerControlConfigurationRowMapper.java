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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.dnp3.domain.ControlType;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;

/**
 * Row mapper for {@link ServerControlConfiguration} entities.
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
 * <li>control_id (TEXT)</li>
 * <li>pname (TEXT)</li>
 * <li>ctype (CHARACTER)</li>
 * <li>dmult (NUMERIC)</li>
 * <li>doffset (NUMERIC)</li>
 * <li>dscale (INTEGER)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class ServerControlConfigurationRowMapper implements RowMapper<ServerControlConfiguration> {

	/** A default instance. */
	public static final RowMapper<ServerControlConfiguration> INSTANCE = new ServerControlConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public ServerControlConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public ServerControlConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public ServerControlConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long serverId = rs.getObject(++p, Long.class);
		Integer entityId = rs.getObject(++p, Integer.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		Boolean enabled = rs.getObject(++p, Boolean.class);
		Long nodeId = rs.getObject(++p, Long.class);
		String controlId = rs.getString(++p);
		String property = rs.getString(++p);
		ControlType type = ControlType.forCode(rs.getString(++p).charAt(0));

		final var conf = new ServerControlConfiguration(userId, serverId, entityId, ts, nodeId,
				controlId, type);
		conf.setModified(mod);
		conf.setEnabled(enabled);
		conf.setProperty(property);
		conf.setMultiplier(rs.getBigDecimal(++p));
		conf.setOffset(rs.getBigDecimal(++p));
		conf.setScale(rs.getObject(++p, Integer.class));
		return conf;
	}

}
