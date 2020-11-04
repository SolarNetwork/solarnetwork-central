/* ==================================================================
 * AggregateDatumEntityRowMapper.java - 31/10/2020 8:39:11 am
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

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;

/**
 * Map rollup aggregate rows into {@link AggregateDatumEntity} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>data_i</li>
 * <li>data_a</li>
 * <li>data_s</li>
 * <li>data_t</li>
 * <li>stat_i</li>
 * <li>read_a</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class AggregateDatumEntityRowMapper implements RowMapper<AggregateDatumEntity> {

	/** A default instance. */
	public static final AggregateDatumEntityRowMapper INSTANCE = new AggregateDatumEntityRowMapper();

	@SuppressWarnings("unchecked")
	private static <T> T getArray(ResultSet rs, int colNum)
			/* , Class<T> type) */ throws SQLException {
		Array a = rs.getArray(colNum);
		if ( a == null ) {
			return null;
		}
		return (T) a.getArray();
	}

	@Override
	public AggregateDatumEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = UUID.fromString(rs.getString(1));
		Instant ts = rs.getTimestamp(2).toInstant();
		BigDecimal[] data_i = getArray(rs, 3);
		BigDecimal[] data_a = getArray(rs, 4);
		String[] data_s = getArray(rs, 5);
		String[] data_t = getArray(rs, 6);
		BigDecimal[][] stat_i = getArray(rs, 7);
		BigDecimal[][] stat_a = getArray(rs, 8);

		return new AggregateDatumEntity(streamId, ts,
				DatumProperties.propertiesOf(data_i, data_a, data_s, data_t),
				DatumPropertiesStatistics.statisticsOf(stat_i, stat_a));
	}

}
