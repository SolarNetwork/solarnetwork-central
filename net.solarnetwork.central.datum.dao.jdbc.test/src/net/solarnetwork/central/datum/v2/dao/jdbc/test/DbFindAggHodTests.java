/* ==================================================================
 * DbFindAggHodTests.java - 10/12/2020 9:12:05 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Test cases for the {@literal solardatm.find_agg_dow} database stored
 * procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindAggHodTests extends BaseDatumJdbcTestSupport {

	private List<AggregateDatum> findAggHod(UUID streamId, Instant start, Instant end) {
		return jdbcTemplate.execute(new ConnectionCallback<List<AggregateDatum>>() {

			@Override
			public List<AggregateDatum> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding agg DOW for stream {} from {} - {}", streamId, start, end);
				List<AggregateDatum> result = new ArrayList<>();
				RowMapper<AggregateDatum> mapper = new AggregateDatumEntityRowMapper(
						Aggregation.DayOfWeek);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_agg_datm_hod(?,?,?)}")) {
					stmt.setObject(1, streamId, Types.OTHER);
					stmt.setTimestamp(2, Timestamp.from(start));
					stmt.setTimestamp(3, Timestamp.from(end));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								AggregateDatum d = mapper.mapRow(rs, ++i);
								result.add(d);
							}
						}
					}
				}
				log.debug("Found agg DOW for stream:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	private ObjectDatumStreamMetadata testStreamMetadata() {
		return testStreamMetadata(1L, "a");
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", ObjectDatumKind.Node, nodeId,
				sourceId, new String[] { "x", "y" }, new String[] { "w" }, null);
	}

	@Test
	public void find_hod_typical() {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();

		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<AggregateDatum> datums = new ArrayList<>(3 * 24);
		for ( int day = 0; day < 3; day++ ) {
			for ( int hour = 0; hour < 24; hour++ ) {
				Instant ts = start.plusDays(day).plusHours(hour).toInstant();
				DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(day + hour) },
						new BigDecimal[] { new BigDecimal((day + 1) * (hour + 1)) }, null, null);
				DatumPropertiesStatistics stats = statisticsOf(
						new BigDecimal[][] { new BigDecimal[] { new BigDecimal(6),
								new BigDecimal(day + hour - 1), new BigDecimal(day + hour + 1) } },
						new BigDecimal[][] { new BigDecimal[] { new BigDecimal(0), new BigDecimal(0),
								new BigDecimal(0) } });

				datums.add(new AggregateDatumEntity(meta.getStreamId(), ts, Aggregation.Hour, props,
						stats));
			}
		}
		DatumDbUtils.insertAggregateDatum(log, jdbcTemplate, datums);

		// WHEN
		List<AggregateDatum> results = findAggHod(meta.getStreamId(), Instant.EPOCH, Instant.now());

		// THEN
		assertThat("24 hour results for one stream", results, hasSize(24));
		ZonedDateTime date = ZonedDateTime.of(2001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		for ( int i = 0; i < 24; i++ ) {
			AggregateDatum d = results.get(i);
			DatumProperties props = propertiesOf(new BigDecimal[] { new BigDecimal(i + 1) },
					new BigDecimal[] { new BigDecimal((i + 1) * 2) }, null, null);
			DatumPropertiesStatistics stats = statisticsOf(
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(18), new BigDecimal(i - 1),
							new BigDecimal(i + 3) } },
					new BigDecimal[][] { new BigDecimal[] { new BigDecimal(i + 1),
							new BigDecimal(3 * (i + 1)), null } });
			assertAggregateDatum("Hour " + i, d, new AggregateDatumEntity(meta.getStreamId(),
					date.plusHours(i).toInstant(), Aggregation.HourOfDay, props, stats));
		}
	}

}
