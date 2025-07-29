/* ==================================================================
 * TypedDatumEntityRowMapper.java - 17/11/2020 7:49:40 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getArray;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getUuid;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.TypedDatumEntity;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Map datum rows into {@link TypedDatumEntity} instances.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>stream_id</li>
 * <li>ts</li>
 * <li>data_i</li>
 * <li>data_a</li>
 * <li>data_s</li>
 * <li>data_t</li>
 * <li>rtype</li>
 * </ol>
 *
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class TypedDatumEntityRowMapper implements RowMapper<TypedDatumEntity> {

	/** A default instance for null aggregates. */
	public static final RowMapper<TypedDatumEntity> INSTANCE = new TypedDatumEntityRowMapper();

	@Override
	public TypedDatumEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = getUuid(rs, 1);
		Instant ts = rs.getTimestamp(2).toInstant();
		BigDecimal[] data_i = getArray(rs, 3);
		BigDecimal[] data_a = getArray(rs, 4);
		String[] data_s = getArray(rs, 5);
		String[] data_t = getArray(rs, 6);
		int type = rs.getInt(7);

		return new TypedDatumEntity(streamId, ts, type,
				DatumProperties.propertiesOf(data_i, data_a, data_s, data_t));
	}

}
