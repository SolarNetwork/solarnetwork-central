/* ==================================================================
 * MostRecentAggDatumHourlyStoredProcedureTests.java - 4/11/2019 11:09:53 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.jdbc.test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.springframework.util.StringUtils;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for the daily aggregate data "most recent" stored procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class MostRecentAggDatumDailyStoredProcedureTests extends BaseDatumJdbcTestSupport {

	private List<Map<String, Object>> queryForMostRecentDatumDaily(Long[] nodeIds, String[] sourceIds) {
		return jdbcTemplate.queryForList(
				"select * from solaragg.find_most_recent_daily(?::bigint[],?::text[])",
				"{" + StringUtils.arrayToCommaDelimitedString(nodeIds) + "}",
				sourceIds != null && sourceIds.length > 0
						? "{" + StringUtils.arrayToCommaDelimitedString(sourceIds) + "}"
						: null);
	}

	private Map<NodeSourcePK, Long> populateMostRecentTestDataSet(Iterable<NodeSourcePK> pks,
			int rowCount) {
		int jitter = 0;
		GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
		s.putInstantaneousSampleValue("foo", 123);
		s.putAccumulatingSampleValue("bar", 234L);
		Map<NodeSourcePK, Long> result = new LinkedHashMap<>(8);
		for ( NodeSourcePK pk : pks ) {
			final long max = System.currentTimeMillis() - jitter;
			result.put(pk, Instant.ofEpochMilli(max).atZone(ZoneId.of("UTC"))
					.truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli());
			for ( int i = 0; i < rowCount; i++ ) {
				long ts = max - TimeUnit.DAYS.toMillis(i);
				insertDatum(ts, pk.getNodeId(), pk.getSourceId(), s);
			}
			jitter += 200;
		}
		processAggregateStaleData();
		return result;
	}

	@Test
	public void mostRecent_oneNode_allSources_noData() {
		// GIVEN
		// nothing

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(new Long[] { TEST_NODE_ID }, null);

		// THEN
		assertThat("No rows found", rows, hasSize(0));
	}

	@Test
	public void mostRecent_oneNode_allSources_oneMatch() {
		// GIVEN
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(
				asList(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID),
						new NodeSourcePK(TEST_NODE_ID - 1L, TEST_SOURCE_ID)),
				5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(new Long[] { TEST_NODE_ID }, null);

		// THEN
		assertThat("Single datum found", rows, hasSize(1));
		Map<String, Object> row = rows.get(0);
		assertThat("Result node ID matches query", row, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source matches query", row, hasEntry("source_id", TEST_SOURCE_ID));
		assertThat("Result date is most recent", row,
				hasEntry("ts_start", new Timestamp(mrData.values().iterator().next())));
	}

	@Test
	public void mostRecent_oneNode_allSources_multiMatch() {
		// GIVEN
		List<NodeSourcePK> pks = asList(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID + ".2"));
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(pks, 5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(new Long[] { TEST_NODE_ID }, null);

		// THEN
		assertThat("Single datum found", rows, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			NodeSourcePK pk = pks.get(i);
			Map<String, Object> row = rows.get(i);
			assertThat("Result " + i + " node ID matches query", row,
					hasEntry("node_id", pk.getNodeId()));
			assertThat("Result " + i + " source matches query", row,
					hasEntry("source_id", pk.getSourceId()));
			assertThat("Result " + i + " date is most recent", row,
					hasEntry("ts_start", new Timestamp(mrData.get(pk))));
		}
	}

	@Test
	public void mostRecent_oneNode_oneSource_oneMatch() {
		// GIVEN
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(
				asList(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID),
						new NodeSourcePK(TEST_NODE_ID - 1L, TEST_SOURCE_ID)),
				5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });

		// THEN
		assertThat("Single datum found", rows, hasSize(1));
		Map<String, Object> row = rows.get(0);
		assertThat("Result node ID matches query", row, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source matches query", row, hasEntry("source_id", TEST_SOURCE_ID));
		assertThat("Result date is most recent", row,
				hasEntry("ts_start", new Timestamp(mrData.values().iterator().next())));
	}

	@Test
	public void mostRecent_oneNode_oneSource_oneMatch_altTz() {
		// GIVEN
		// set up time zone for node
		final Long locId = -2L;
		final ZoneId utc = ZoneId.of("UTC");
		final ZoneId tz = ZoneId.of("America/Los_Angeles");
		setupTestLocation(locId, tz.getId());
		setupTestNode(TEST_NODE_ID, locId);

		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(
				asList(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)), 5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });

		// THEN
		assertThat("Single datum found", rows, hasSize(1));
		Map<String, Object> row = rows.get(0);
		assertThat("Result node ID matches query", row, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source matches query", row, hasEntry("source_id", TEST_SOURCE_ID));
		assertThat("Result date is most recent", row,
				hasEntry("ts_start",
						new Timestamp(Instant.ofEpochMilli(mrData.values().iterator().next()).atZone(utc)
								.toLocalDateTime().atZone(tz).toInstant().toEpochMilli())));
	}

	@Test
	public void mostRecent_multiNodes_allSources_multiMatch() {
		// GIVEN
		List<NodeSourcePK> pks = asList(new NodeSourcePK(TEST_NODE_ID - 1L, TEST_SOURCE_ID + ".2"),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID));
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(pks, 5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(
				new Long[] { TEST_NODE_ID, TEST_NODE_ID - 1L }, null);

		// THEN
		assertThat("Single datum found", rows, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			NodeSourcePK pk = pks.get(i);
			Map<String, Object> row = rows.get(i);
			assertThat("Result " + i + " node ID matches query", row,
					hasEntry("node_id", pk.getNodeId()));
			assertThat("Result " + i + " source matches query", row,
					hasEntry("source_id", pk.getSourceId()));
			assertThat("Result " + i + " date is most recent", row,
					hasEntry("ts_start", new Timestamp(mrData.get(pk))));
		}
	}

	@Test
	public void mostRecent_multiNodes_multiSources_multiMatch() {
		// GIVEN
		List<NodeSourcePK> pks = asList(new NodeSourcePK(TEST_NODE_ID - 1L, TEST_SOURCE_ID + ".2"),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID));
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(pks, 5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(
				new Long[] { TEST_NODE_ID, TEST_NODE_ID - 1L },
				new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID + ".2" });

		// THEN
		assertThat("Single datum found", rows, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			NodeSourcePK pk = pks.get(i);
			Map<String, Object> row = rows.get(i);
			assertThat("Result " + i + " node ID matches query", row,
					hasEntry("node_id", pk.getNodeId()));
			assertThat("Result " + i + " source matches query", row,
					hasEntry("source_id", pk.getSourceId()));
			assertThat("Result " + i + " date is most recent", row,
					hasEntry("ts_start", new Timestamp(mrData.get(pk))));
		}
	}

	@Test
	public void mostRecent_multiNodes_oneSource_multiMatch() {
		// GIVEN
		List<NodeSourcePK> pks = asList(new NodeSourcePK(TEST_NODE_ID - 1L, TEST_SOURCE_ID),
				new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID));
		Map<NodeSourcePK, Long> mrData = populateMostRecentTestDataSet(pks, 5);

		// WHEN
		List<Map<String, Object>> rows = queryForMostRecentDatumDaily(
				new Long[] { TEST_NODE_ID, TEST_NODE_ID - 1L }, new String[] { TEST_SOURCE_ID });

		// THEN
		assertThat("Single datum found", rows, hasSize(2));
		for ( int i = 0; i < 2; i++ ) {
			NodeSourcePK pk = pks.get(i);
			Map<String, Object> row = rows.get(i);
			assertThat("Result " + i + " node ID matches query", row,
					hasEntry("node_id", pk.getNodeId()));
			assertThat("Result " + i + " source matches query", row,
					hasEntry("source_id", pk.getSourceId()));
			assertThat("Result " + i + " date is most recent", row,
					hasEntry("ts_start", new Timestamp(mrData.get(pk))));
		}
	}

}
