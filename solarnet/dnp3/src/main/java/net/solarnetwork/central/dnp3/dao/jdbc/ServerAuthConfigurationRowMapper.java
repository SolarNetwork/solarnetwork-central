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
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;

/**
 * Row mapper for {@link ServerAuthConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>server_id (BIGINT)</li>
 * <li>ident (TEXT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>cname (TEXTLONG)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class ServerAuthConfigurationRowMapper implements RowMapper<ServerAuthConfiguration> {

	/** A default instance. */
	public static final RowMapper<ServerAuthConfiguration> INSTANCE = new ServerAuthConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public ServerAuthConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 * 
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public ServerAuthConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public ServerAuthConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long serverId = rs.getObject(++p, Long.class);
		String entityId = rs.getString(++p);
		Timestamp ts = rs.getTimestamp(++p);
		ServerAuthConfiguration conf = new ServerAuthConfiguration(userId, serverId, entityId,
				ts.toInstant());
		conf.setModified(rs.getTimestamp(++p).toInstant());
		conf.setEnabled(rs.getObject(++p, Boolean.class));
		conf.setName(rs.getString(++p));
		return conf;
	}

}
