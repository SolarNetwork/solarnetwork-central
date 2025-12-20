/* ==================================================================
 * CloudControlConfigurationRowMapper.java - 3/11/2025 7:42:09 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;

/**
 * Row mapper for {@link CloudControlConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>cname (TEXT)</li>
 * <li>sident (TEXT)</li>
 * <li>int_id (BIGINT)</li>
 * <li>node_id (BIGINT)</li>
 * <li>control_id (TEXT)</li>
 * <li>cref (TEXT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CloudControlConfigurationRowMapper implements RowMapper<CloudControlConfiguration> {

	/** A default instance. */
	public static final RowMapper<CloudControlConfiguration> INSTANCE = new CloudControlConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CloudControlConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CloudControlConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CloudControlConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = getTimestampInstant(rs, ++p);
		CloudControlConfiguration conf = new CloudControlConfiguration(userId, entityId, ts);
		conf.setModified(getTimestampInstant(rs, ++p));
		conf.setEnabled(rs.getBoolean(++p));
		conf.setName(rs.getString(++p));
		conf.setServiceIdentifier(rs.getString(++p));
		conf.setIntegrationId(rs.getObject(++p, Long.class));
		conf.setNodeId(rs.getObject(++p, Long.class));
		conf.setControlId(rs.getString(++p));
		conf.setControlReference(rs.getString(++p));

		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
