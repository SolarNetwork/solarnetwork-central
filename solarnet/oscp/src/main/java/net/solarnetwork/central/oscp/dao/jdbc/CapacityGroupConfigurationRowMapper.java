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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementPeriod;
import net.solarnetwork.codec.jackson.JsonUtils;

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
 * <li>cp_meas_secs (INTEGER)</li>
 * <li>co_meas_secs (INTEGER)</li>
 * <li>cp_id (BIGINT)</li>
 * <li>co_id (BIGINT)</li>
 * <li>sprops (TEXT)</li>
 * <li>cp_meas_at (TIMESTAMP)</li>
 * <li>co_meas_at (TIMESTAMP)</li>
 * </ol>
 *
 *
 * @author matt
 * @version 1.0
 */
public class CapacityGroupConfigurationRowMapper implements RowMapper<CapacityGroupConfiguration> {

	/** A default instance. */
	public static final RowMapper<CapacityGroupConfiguration> INSTANCE = new CapacityGroupConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CapacityGroupConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CapacityGroupConfigurationRowMapper(int columnOffset) {
		super();
		this.columnOffset = columnOffset;
	}

	@Override
	public CapacityGroupConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;

		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		Long userId = rs.getObject(++p, Long.class);
		boolean enabled = rs.getBoolean(++p);
		String name = rs.getString(++p);
		String identifier = rs.getString(++p);
		MeasurementPeriod cpMeasurementPeriod = MeasurementPeriod.forCode(rs.getInt(++p));
		MeasurementPeriod coMeasurementPeriod = MeasurementPeriod.forCode(rs.getInt(++p));
		Long cpId = rs.getObject(++p, Long.class);
		Long coId = rs.getObject(++p, Long.class);

		final var conf = new CapacityGroupConfiguration(userId, entityId, ts, name, identifier, cpId,
				coId, cpMeasurementPeriod, coMeasurementPeriod);
		conf.setModified(mod);
		conf.setEnabled(enabled);
		conf.setServiceProps(JsonUtils.getStringMap(rs.getString(++p)));

		ts = getTimestampInstant(rs, ++p);
		if ( ts != null ) {
			conf.setCapacityProviderMeasurementDate(ts);
		}
		ts = getTimestampInstant(rs, ++p);
		if ( ts != null ) {
			conf.setCapacityOptimizerMeasurementDate(ts);
		}

		return conf;
	}

}
