/* ==================================================================
 * DbTimeRangeTests.java - 14/11/2020 7:35:59 am
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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.ZonedStreamsTimeRangeRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ZonedStreamsTimeRange;

/**
 * Test cases for database procedures that compute time ranges for streams.
 * 
 * @author matt
 * @version 1.0
 */
public class DbTimeRangeTests extends BaseDatumJdbcTestSupport {

	private List<ZonedStreamsTimeRange> findTimeRanges(Long[] nodeIds, String[] sourceIds,
			LocalDateTime start, LocalDateTime end) {
		return jdbcTemplate.execute(new ConnectionCallback<List<ZonedStreamsTimeRange>>() {

			@Override
			public List<ZonedStreamsTimeRange> doInConnection(Connection con)
					throws SQLException, DataAccessException {
				log.debug("Finding zoned streams time ranges for nodes {} sources {}",
						Arrays.toString(nodeIds), Arrays.toString(sourceIds));
				List<ZonedStreamsTimeRange> result = new ArrayList<>();
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.time_ranges_local(?::bigint[],?::text[],?,?)}")) {
					Array array = con.createArrayOf("bigint", nodeIds);
					stmt.setArray(1, array);
					array.free();
					if ( sourceIds != null ) {
						array = con.createArrayOf("text", sourceIds);
						stmt.setArray(2, array);
						array.free();
					} else {
						stmt.setNull(2, Types.ARRAY);
					}
					stmt.setTimestamp(3, Timestamp.valueOf(start));
					stmt.setTimestamp(4, Timestamp.valueOf(end));
					if ( stmt.execute() ) {
						try (ResultSet rs = stmt.getResultSet()) {
							int i = 0;
							while ( rs.next() ) {
								result.add(ZonedStreamsTimeRangeRowMapper.INSTANCE.mapRow(rs, ++i));
							}
						}
					}
				}
				log.debug("Found zoned streams time ranges:\n{}",
						result.stream().map(Object::toString).collect(joining("\n")));
				return result;
			}
		});
	}

	private ObjectDatumStreamMetadata testStreamMetadata(Long nodeId, String sourceId) {
		return new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", ObjectDatumKind.Node, nodeId,
				sourceId, new String[] { "x", "y" }, new String[] { "w" }, null);
	}

	@Test
	public void findTimeRanges_noData() {
		// GIVEN

		// WHEN
		LocalDateTime start = LocalDateTime.of(2020, 6, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 7, 1, 0, 0);
		List<ZonedStreamsTimeRange> results = findTimeRanges(new Long[] { 1L }, new String[] { "a" },
				start, end);

		// THEN
		assertThat("No result from no data", results, hasSize(0));
	}

	private static void assertZonedStreamsTimeRange(String prefix, ZonedStreamsTimeRange result,
			ZonedStreamsTimeRange expected) {
		assertThat(prefix + " time zone expected", result.getTimeZoneId(),
				equalTo(expected.getTimeZoneId()));
		assertThat(prefix + " start date mapped to zone", result.getStartDate(),
				equalTo(expected.getStartDate()));
		assertThat(prefix + " end date mapped to zone", result.getEndDate(),
				equalTo(expected.getEndDate()));
		assertThat(prefix + " stream IDs for zone", result.getStreamIds(),
				arrayContainingInAnyOrder(expected.getStreamIds()));
	}

	private static void assertZonedStreamsTimeRange(String prefix, ZonedStreamsTimeRange result,
			String timeZoneId, LocalDateTime start, LocalDateTime end, UUID... streamIds) {
		ZoneId tz = ZoneId.of(timeZoneId);
		ZonedStreamsTimeRange expected = new ZonedStreamsTimeRange(start.atZone(tz).toInstant(),
				end.atZone(tz).toInstant(), timeZoneId, streamIds);
		assertZonedStreamsTimeRange(prefix, result, expected);
	}

	@Test
	public void findTimeRanges_oneZone() {
		// GIVEN
		setupTestLocation(TEST_LOC_ID, TEST_TZ);
		setupTestNode(1L, TEST_LOC_ID);
		setupTestNode(2L, TEST_LOC_ID);
		ObjectDatumStreamMetadata meta1 = testStreamMetadata(1L, "a");
		ObjectDatumStreamMetadata meta2 = testStreamMetadata(1L, "b");
		ObjectDatumStreamMetadata meta3 = testStreamMetadata(2L, "a");
		List<ObjectDatumStreamMetadata> metas = asList(meta1, meta2, meta3);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, metas);

		// WHEN
		LocalDateTime start = LocalDateTime.of(2020, 6, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 7, 1, 0, 0);
		List<ZonedStreamsTimeRange> results = findTimeRanges(new Long[] { 1L, 2L },
				new String[] { "a", "b" }, start, end);

		// THEN
		assertThat("All rows mapped to one time zone", results, hasSize(1));
		assertZonedStreamsTimeRange("All one zone", results.get(0), TEST_TZ, start, end,
				metas.stream().map(ObjectDatumStreamMetadata::getStreamId).toArray(UUID[]::new));
	}

	@Test
	public void findTimeRanges_twoZones() {
		// GIVEN
		final String altTzId = "America/Los_Angeles";
		setupTestLocation(1L, TEST_TZ);
		setupTestLocation(2L, altTzId);
		setupTestNode(1L, 1L);
		setupTestNode(2L, 2L);
		ObjectDatumStreamMetadata meta1 = testStreamMetadata(1L, "a");
		ObjectDatumStreamMetadata meta2 = testStreamMetadata(1L, "b");
		ObjectDatumStreamMetadata meta3 = testStreamMetadata(2L, "a");
		List<ObjectDatumStreamMetadata> metas = asList(meta1, meta2, meta3);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, metas);

		// WHEN
		LocalDateTime start = LocalDateTime.of(2020, 6, 1, 0, 0);
		LocalDateTime end = LocalDateTime.of(2020, 7, 1, 0, 0);
		List<ZonedStreamsTimeRange> results = findTimeRanges(new Long[] { 1L, 2L },
				new String[] { "a", "b" }, start, end);

		// THEN
		assertThat("Rows mapped to two time zones", results, hasSize(2));
		assertThat("Row zones",
				results.stream().map(ZonedStreamsTimeRange::getTimeZoneId).toArray(String[]::new),
				arrayContainingInAnyOrder(TEST_TZ, altTzId));

		ZonedStreamsTimeRange z1 = null;
		ZonedStreamsTimeRange z2 = null;
		for ( ZonedStreamsTimeRange z : results ) {
			if ( TEST_TZ.equals(z.getTimeZoneId()) ) {
				z1 = z;
			} else if ( altTzId.equals(z.getTimeZoneId()) ) {
				z2 = z;
			}
		}
		assertZonedStreamsTimeRange("Zone 1", z1, TEST_TZ, start, end,
				new UUID[] { meta1.getStreamId(), meta2.getStreamId() });
		assertZonedStreamsTimeRange("Zone 2", z2, altTzId, start, end,
				new UUID[] { meta3.getStreamId() });
	}

}
