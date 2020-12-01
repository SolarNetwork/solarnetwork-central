/* ==================================================================
 * DbMarkStaleTests.java - 12/11/2020 3:39:54 pm
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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;

/**
 * Test cases for the "mark stale" stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class DbMarkStaleTests extends BaseDatumJdbcTestSupport {

	public void callMarkStaleDateRange(UUID[] streamIds, Instant from, Instant to) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.mark_stale_datm_hours(?,?,?)}")) {
					log.debug("Marking streams {} stale from {} to {}", Arrays.toString(streamIds), from,
							to);
					Array array = con.createArrayOf("uuid", streamIds);
					stmt.setArray(1, array);
					array.free();

					stmt.setTimestamp(2, Timestamp.from(from));
					stmt.setTimestamp(3, Timestamp.from(to));
					stmt.execute();
					return null;
				}
			}
		});
	}

	@Test
	public void markStale_dateRange_empty() {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

		// WHEN
		callMarkStaleDateRange(new UUID[] { UUID.randomUUID() }, start.toInstant(), Instant.now());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange_noMatchingStream() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		insertDatumStream(log, jdbcTemplate, datums, "UTC");

		// WHEN
		callMarkStaleDateRange(new UUID[] { UUID.randomUUID() }, start.toInstant(), Instant.now());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange_noMatchingDates() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID[] streamIds = meta.values().stream().map(ObjectDatumStreamMetadata::getStreamId)
				.toArray(UUID[]::new);

		// WHEN
		callMarkStaleDateRange(streamIds, start.minusYears(1).toInstant(), start.toInstant());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("Nothing stale", stales, hasSize(0));
	}

	@Test
	public void markStale_dateRange() throws IOException {
		// GIVEN
		ZonedDateTime start = ZonedDateTime.of(2020, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				"UTC");
		UUID[] streamIds = meta.values().stream().map(ObjectDatumStreamMetadata::getStreamId)
				.toArray(UUID[]::new);

		// WHEN
		callMarkStaleDateRange(streamIds, start.toInstant(), start.plusYears(1).toInstant());

		// THEN
		List<StaleAggregateDatum> stales = listStaleAggregateDatum(jdbcTemplate);
		assertThat("All matching hours marked stale", stales, hasSize(2));
		Set<Instant> staleDates = stales.stream().map(StaleAggregateDatum::getTimestamp)
				.collect(Collectors.toSet());
		assertThat("Stale hours from data", staleDates,
				containsInAnyOrder(start.toInstant(), start.plusHours(1).toInstant()));
	}
}
