/* ==================================================================
 * DbFindAggDowTests.java - 10/12/2020 9:12:05 pm
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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonAggregateDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertAggregateDatum;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.io.IOException;
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
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@literal solardatm.find_agg_dow} database stored
 * procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class DbFindAggDowTests extends BaseDatumJdbcTestSupport {

	private List<AggregateDatum> findAggDow(UUID streamId, Instant start, Instant end) {
		return jdbcTemplate.execute(new ConnectionCallback<List<AggregateDatum>>() {

			@Override
			public List<AggregateDatum> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding agg DOW for stream {} from {} - {}", streamId, start, end);
				List<AggregateDatum> result = new ArrayList<>();
				RowMapper<AggregateDatum> mapper = new AggregateDatumEntityRowMapper(
						Aggregation.DayOfWeek);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.find_agg_datm_dow(?,?,?)}")) {
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
	public void find_dow_typical() throws IOException {
		// GIVEN
		ObjectDatumStreamMetadata meta = testStreamMetadata();
		List<AggregateDatum> datums = loadJsonAggregateDatumResource("test-agg-day-datum-02.txt",
				getClass(), staticProvider(singleton(meta)));
		insertAggregateDatum(log, jdbcTemplate, datums);
		UUID streamId = meta.getStreamId();

		// WHEN
		List<AggregateDatum> results = findAggDow(streamId, Instant.EPOCH, Instant.now());

		// THEN
		ZonedDateTime date = ZonedDateTime.of(2001, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		assertThat("7 day results for one stream", results, hasSize(7));
		assertAggregateDatum("Monday result", results.get(0),
				new AggregateDatumEntity(streamId, date.toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.6", "6.1"), decimalArray("500"), null, null),
						statisticsOf(
								new BigDecimal[][] { decimalArray("18", "1.1", "3.1"),
										decimalArray("18", "2.0", "7.1") },
								new BigDecimal[][] { decimalArray(null, "100", "800") })));
		assertAggregateDatum("Tuesday result", results.get(1),
				new AggregateDatumEntity(streamId, date.plusDays(1).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.4", "4.1"), decimalArray("600"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.2", "3.2"),
								decimalArray("18", "2.1", "7.2") }, null)));
		assertAggregateDatum("Wednesday result", results.get(2),
				new AggregateDatumEntity(streamId, date.plusDays(2).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.5", "5.1"), decimalArray("400"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.3", "3.3"),
								decimalArray("18", "2.2", "7.3") }, null)));
		assertAggregateDatum("Thursday result", results.get(3),
				new AggregateDatumEntity(streamId, date.plusDays(3).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.3", "3.1"), decimalArray("500"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.4", "3.4"),
								decimalArray("18", "2.3", "7.4") }, null)));
		assertAggregateDatum("Friday result", results.get(4),
				new AggregateDatumEntity(streamId, date.plusDays(4).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.4", "4.1"), decimalArray("300"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.5", "3.5"),
								decimalArray("18", "2.4", "7.5") }, null)));
		assertAggregateDatum("Saturday result", results.get(5),
				new AggregateDatumEntity(streamId, date.plusDays(5).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.5", "5.1"), decimalArray("400"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.6", "3.6"),
								decimalArray("18", "2.5", "7.6") }, null)));
		assertAggregateDatum("Sunday result", results.get(6),
				new AggregateDatumEntity(streamId, date.plusDays(6).toInstant(), Aggregation.DayOfWeek,
						propertiesOf(decimalArray("1.6", "6.1"), decimalArray("500"), null, null),
						statisticsOf(new BigDecimal[][] { decimalArray("18", "1.7", "3.7"),
								decimalArray("18", "2.6", "7.7") }, null)));
	}

}
