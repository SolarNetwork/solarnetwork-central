/* ==================================================================
 * CloudDatumStreamConfigurationRowMapper.java - 3/10/2024 1:12:26 pm
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Row mapper for {@link CloudDatumStreamConfiguration} entities.
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
 * <li>kind (CHARACTER)</li>
 * <li>obj_id (BIGINT)</li>
 * <li>source_id (TEXT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamConfigurationRowMapper implements RowMapper<CloudDatumStreamConfiguration> {

	/** A default instance. */
	public static final RowMapper<CloudDatumStreamConfiguration> INSTANCE = new CloudDatumStreamConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CloudDatumStreamConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CloudDatumStreamConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CloudDatumStreamConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = getTimestampInstant(rs, ++p);
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(userId, entityId, ts);
		conf.setModified(getTimestampInstant(rs, ++p));
		conf.setEnabled(rs.getBoolean(++p));
		conf.setName(rs.getString(++p));
		conf.setServiceIdentifier(rs.getString(++p));
		conf.setIntegrationId(rs.getObject(++p, Long.class));

		String kind = rs.getString(++p);
		if ( kind != null ) {
			conf.setKind(ObjectDatumKind.forKey(kind));
		}

		conf.setObjectId(rs.getObject(++p, Long.class));
		conf.setSourceId(rs.getString(++p));

		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
