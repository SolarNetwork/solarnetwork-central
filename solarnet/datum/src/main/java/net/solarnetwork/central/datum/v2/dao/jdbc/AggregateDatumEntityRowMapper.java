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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumJdbcUtils.getArray;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;

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
public class AggregateDatumEntityRowMapper implements RowMapper<AggregateDatum> {

	/** A default instance for null aggregates. */
	public static final RowMapper<AggregateDatum> INSTANCE = new AggregateDatumEntityRowMapper(null);

	/** A default instance for hourly aggregates. */
	public static final RowMapper<AggregateDatum> HOUR_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Hour);

	/** A default instance for daily aggregates. */
	public static final RowMapper<AggregateDatum> DAY_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Day);

	/** A default instance for monthly aggregates. */
	public static final RowMapper<AggregateDatum> MONTH_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Month);

	/** A default reading instance for null aggregates. */
	public static final RowMapper<AggregateDatum> READING_INSTANCE = new AggregateDatumEntityRowMapper(
			null, true);

	/** A default reading instance for hourly aggregates. */
	public static final RowMapper<AggregateDatum> READING_HOUR_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Hour, true);

	/** A default reading instance for daily aggregates. */
	public static final RowMapper<AggregateDatum> READING_DAY_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Day, true);

	/** A default reading instance for monthly aggregates. */
	public static final RowMapper<AggregateDatum> READING_MONTH_INSTANCE = new AggregateDatumEntityRowMapper(
			Aggregation.Month, true);

	private final Aggregation aggregation;
	private final boolean readingMode;

	/**
	 * Constructor.
	 * 
	 * @param aggregation
	 *        the aggregation kind to assign
	 */
	public AggregateDatumEntityRowMapper(Aggregation aggregation) {
		this(aggregation, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param aggregation
	 *        the aggregation kind to assign
	 * @param readingMode
	 *        {@literal true} to create {@link ReadingDatumEntity} instances
	 */
	public AggregateDatumEntityRowMapper(Aggregation aggregation, boolean readingMode) {
		super();
		this.aggregation = aggregation;
		this.readingMode = readingMode;
	}

	/**
	 * Get a mapper for an aggregation type.
	 * 
	 * @param kind
	 *        the kind of aggregation
	 * @param readingMode
	 *        {@literal true} to create {@link ReadingDatumEntity} instances
	 * @return the mapper, never {@literal null}
	 */
	public static RowMapper<AggregateDatum> mapperForAggregate(Aggregation kind, boolean readingMode) {
		RowMapper<AggregateDatum> mapper;
		switch (kind) {
			case Hour:
				mapper = readingMode ? READING_HOUR_INSTANCE : HOUR_INSTANCE;
				break;

			case Day:
				mapper = readingMode ? READING_DAY_INSTANCE : DAY_INSTANCE;
				break;

			case Month:
				mapper = readingMode ? READING_MONTH_INSTANCE : MONTH_INSTANCE;
				break;

			default:
				mapper = new AggregateDatumEntityRowMapper(kind, readingMode);
		}
		return mapper;
	}

	@Override
	public AggregateDatumEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = DatumJdbcUtils.getUuid(rs, 1);
		Instant ts = rs.getTimestamp(2).toInstant();
		BigDecimal[] data_i = getArray(rs, 3);
		BigDecimal[] data_a = getArray(rs, 4);
		String[] data_s = getArray(rs, 5);
		String[] data_t = getArray(rs, 6);
		BigDecimal[][] stat_i = getArray(rs, 7);
		BigDecimal[][] stat_a = getArray(rs, 8);

		DatumProperties props = DatumProperties.propertiesOf(data_i, data_a, data_s, data_t);
		DatumPropertiesStatistics stats = DatumPropertiesStatistics.statisticsOf(stat_i, stat_a);
		if ( readingMode ) {
			return new ReadingDatumEntity(streamId, ts, aggregation, null, props, stats);
		}
		return new AggregateDatumEntity(streamId, ts, aggregation, props, stats);
	}

}
