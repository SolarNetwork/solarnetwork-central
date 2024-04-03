/* ==================================================================
 * EndpointAuthConfigurationRowMapper.java - 21/02/2024 4:11:40 pm
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

package net.solarnetwork.central.inin.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.inin.domain.EndpointAuthConfiguration;

/**
 * Row mapper for {@link EndpointAuthConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>endpoint_id (LONG)</li>
 * <li>cred_id (LONG)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class EndpointAuthConfigurationRowMapper implements RowMapper<EndpointAuthConfiguration> {

	/** A default instance. */
	public static final RowMapper<EndpointAuthConfiguration> INSTANCE = new EndpointAuthConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public EndpointAuthConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public EndpointAuthConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public EndpointAuthConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		UUID endpointId = CommonJdbcUtils.getUuid(rs, ++p);
		Long credentialId = rs.getObject(++p, Long.class);
		Timestamp ts = rs.getTimestamp(++p);
		EndpointAuthConfiguration conf = new EndpointAuthConfiguration(userId, endpointId, credentialId,
				ts.toInstant());
		conf.setModified(rs.getTimestamp(++p).toInstant());
		conf.setEnabled(rs.getBoolean(++p));
		return conf;
	}

}
