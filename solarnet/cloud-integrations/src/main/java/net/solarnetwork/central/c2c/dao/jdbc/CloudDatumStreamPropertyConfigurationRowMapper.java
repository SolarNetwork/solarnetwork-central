/* ==================================================================
 * CloudDatumStreamPropertyConfigurationRowMapper.java - 4/10/2024 8:30:54 am
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * Row mapper for {@link CloudDatumStreamPropertyConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>ds_id (BIGINT)</li>
 * <li>idx (SMALLINT)
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>ptype (TEXT)</li>
 * <li>pname (TEXT)</li>
 * <li>vref (TEXT)</li>
 * <li>mult (NUMERIC)</li>
 * <li>scale (SMALLINT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamPropertyConfigurationRowMapper
		implements RowMapper<CloudDatumStreamPropertyConfiguration> {

	/** A default instance. */
	public static final RowMapper<CloudDatumStreamPropertyConfiguration> INSTANCE = new CloudDatumStreamPropertyConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CloudDatumStreamPropertyConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CloudDatumStreamPropertyConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CloudDatumStreamPropertyConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long dataSourceId = rs.getObject(++p, Long.class);
		Integer idx = rs.getObject(++p, Integer.class);
		Instant ts = getTimestampInstant(rs, ++p);
		CloudDatumStreamPropertyConfiguration conf = new CloudDatumStreamPropertyConfiguration(userId,
				dataSourceId, idx, ts);
		conf.setModified(getTimestampInstant(rs, ++p));
		conf.setEnabled(rs.getBoolean(++p));
		conf.setPropertyType(DatumSamplesType.fromValue(rs.getString(++p)));
		conf.setPropertyName(rs.getString(++p));
		conf.setValueReference(rs.getString(++p));
		conf.setMultiplier(rs.getBigDecimal(++p));
		conf.setScale(rs.getObject(++p, Integer.class));
		return conf;
	}

}
