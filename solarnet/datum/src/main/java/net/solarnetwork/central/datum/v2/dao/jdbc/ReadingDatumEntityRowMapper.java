/* ==================================================================
 * ReadingDatumEntityRowMapper.java - 17/11/2020 4:26:39 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getUuid;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;

/**
 * Map reading datum rows into {@link ReadingDatum} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>stream_id</li>
 * <li>ts_start</li>
 * <li>ts_end</li>
 * <li>data_i</li>
 * <li>data_a</li>
 * <li>data_s</li>
 * <li>data_t</li>
 * <li>stat_i</li>
 * <li>read_a</li>
 * </ol>
 * 
 * @author matt
 * @version 1.2
 * @since 3.8
 */
public class ReadingDatumEntityRowMapper implements RowMapper<ReadingDatum> {

	/** A default instance for null aggregates. */
	public static final RowMapper<ReadingDatum> INSTANCE = new ReadingDatumEntityRowMapper(null);

	/** A default instance for hourly aggregates. */
	public static final RowMapper<ReadingDatum> HOUR_INSTANCE = new ReadingDatumEntityRowMapper(
			Aggregation.Hour);

	/** A default instance for daily aggregates. */
	public static final RowMapper<ReadingDatum> DAY_INSTANCE = new ReadingDatumEntityRowMapper(
			Aggregation.Day);

	/** A default instance for monthly aggregates. */
	public static final RowMapper<ReadingDatum> MONTH_INSTANCE = new ReadingDatumEntityRowMapper(
			Aggregation.Month);

	private final Aggregation aggregation;

	/**
	 * Constructor.
	 * 
	 * @param aggregation
	 *        the aggregation kind to assign
	 */
	public ReadingDatumEntityRowMapper(Aggregation aggregation) {
		super();
		this.aggregation = aggregation;
	}

	@SuppressWarnings("unchecked")
	private static <T> T getArray(ResultSet rs, int colNum) throws SQLException {
		Array a = rs.getArray(colNum);
		if ( a == null ) {
			return null;
		}
		return (T) a.getArray();
	}

	@Override
	public ReadingDatum mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = getUuid(rs, 1);
		Timestamp ts = rs.getTimestamp(2);
		Timestamp tsEnd = rs.getTimestamp(3);
		BigDecimal[] data_i = getArray(rs, 4);
		BigDecimal[] data_a = getArray(rs, 5);
		String[] data_s = getArray(rs, 6);
		String[] data_t = getArray(rs, 7);
		BigDecimal[][] stat_i = getArray(rs, 8);
		BigDecimal[][] stat_a = getArray(rs, 9);

		return new ReadingDatumEntity(streamId, ts != null ? ts.toInstant() : null, aggregation,
				tsEnd != null ? tsEnd.toInstant() : null,
				DatumProperties.propertiesOf(data_i, data_a, data_s, data_t),
				DatumPropertiesStatistics.statisticsOf(stat_i, stat_a));
	}

}
