/* ==================================================================
 * CloudDatumStreamMappingConfigurationRowMapper.java - 16/10/2024 7:08:57 am
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

package net.solarnetwork.central.c2c.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;

/**
 * Row mapper for {@link CloudDatumStreamMappingConfiguration} entities.
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
 * <li>cname (TEXT)</li>
 * <li>int_id (BIGINT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamMappingConfigurationRowMapper
		implements RowMapper<CloudDatumStreamMappingConfiguration> {

	/** A default instance. */
	public static final RowMapper<CloudDatumStreamMappingConfiguration> INSTANCE = new CloudDatumStreamMappingConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CloudDatumStreamMappingConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CloudDatumStreamMappingConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CloudDatumStreamMappingConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = getTimestampInstant(rs, ++p);
		CloudDatumStreamMappingConfiguration conf = new CloudDatumStreamMappingConfiguration(userId,
				entityId, ts);
		conf.setModified(getTimestampInstant(rs, ++p));
		conf.setName(rs.getString(++p));
		conf.setIntegrationId(rs.getObject(++p, Long.class));

		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
