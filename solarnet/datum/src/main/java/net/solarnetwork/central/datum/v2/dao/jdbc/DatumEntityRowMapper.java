/* ==================================================================
 * DatumEntityRowMapper.java - 13/11/2020 10:22:21 am
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumJdbcUtils.getArray;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Map datum rows into {@link DatumEntity} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts</li>
 * <li>received</li>
 * <li>data_i</li>
 * <li>data_a</li>
 * <li>data_s</li>
 * <li>data_t</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class DatumEntityRowMapper implements RowMapper<Datum> {

	/** A default instance for null aggregates. */
	public static final RowMapper<Datum> INSTANCE = new DatumEntityRowMapper();

	@Override
	public Datum mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = UUID.fromString(rs.getString(1));
		Instant ts = rs.getTimestamp(2).toInstant();
		Instant recv = rs.getTimestamp(3).toInstant();
		BigDecimal[] data_i = getArray(rs, 4);
		BigDecimal[] data_a = getArray(rs, 5);
		String[] data_s = getArray(rs, 6);
		String[] data_t = getArray(rs, 7);

		return new DatumEntity(streamId, ts, recv,
				DatumProperties.propertiesOf(data_i, data_a, data_s, data_t));
	}

}
