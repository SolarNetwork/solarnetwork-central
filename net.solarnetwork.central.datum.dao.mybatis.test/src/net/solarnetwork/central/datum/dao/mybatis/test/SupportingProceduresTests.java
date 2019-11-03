/* ==================================================================
 * SupportingProceduresTests.java - 24/11/2018 6:01:15 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.StringUtils;

/**
 * Tests for some supporting database procedures.
 * 
 * @author matt
 * @version 1.0
 */
public class SupportingProceduresTests extends AbstractMyBatisDaoTestSupport {

	private static final Long TEST_NODE_ID2 = -2L;
	private static final String TEST_SOURCE_ID = "test.source";

	private MyBatisGeneralNodeDatumDao datumDao;

	@Before
	public void setup() {
		datumDao = new MyBatisGeneralNodeDatumDao();
		datumDao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private List<Map<String, Object>> nlt(Long[] nodeIds, String[] sourceIds, LocalDateTime s,
			LocalDateTime e) {
		return jdbcTemplate.queryForList(
				"SELECT time_zone, ts_start, ts_end, array_to_string(node_ids,',') as node_ids, "
						+ "array_to_string(source_ids,',') as source_ids "
						+ "FROM solarnet.node_source_time_ranges_local(?::bigint[], ?::text[], ?::timestamp, ?::timestamp) "
						+ "ORDER BY ts_start",
				"{" + StringUtils.commaDelimitedStringFromCollection(Arrays.asList(nodeIds)) + "}",
				"{" + Arrays.stream(sourceIds).collect(Collectors.joining(",")) + "}", s.toString(),
				e.toString());
	}

	@Test
	public void nodeLocalTimesMultipleNodes() {
		final DateTimeZone tz2 = DateTimeZone.forID("America/Los_Angeles");
		setupTestLocation(-9889L, tz2.getID());
		setupTestNode(TEST_NODE_ID2, -9889L);

		LocalDateTime s = new LocalDateTime(2018, 11, 1, 0, 0);
		LocalDateTime e = new LocalDateTime(2018, 12, 1, 0, 0);
		List<Map<String, Object>> nlt = nlt(new Long[] { TEST_NODE_ID, TEST_NODE_ID2 },
				new String[] { TEST_SOURCE_ID }, s, e);

		assertThat("Time ranges count", nlt, hasSize(2));
		assertThat("Range 1 zone", nlt.get(0).get("time_zone"), equalTo(TEST_TZ));
		assertThat("Range 1 start", nlt.get(0).get("ts_start"),
				equalTo(new Timestamp(s.toDateTime(DateTimeZone.forID(TEST_TZ)).getMillis())));
		assertThat("Range 1 nodes", nlt.get(0).get("node_ids"), equalTo(TEST_NODE_ID.toString()));
		assertThat("Range 1 sources", nlt.get(0).get("source_ids"), equalTo(TEST_SOURCE_ID));

		assertThat("Range 2 zone", nlt.get(1).get("time_zone"), equalTo(tz2.getID()));
		assertThat("Range 2 start", nlt.get(1).get("ts_start"),
				equalTo(new Timestamp(s.toDateTime(tz2).getMillis())));
		assertThat("Range 2 nodes", nlt.get(1).get("node_ids"), equalTo(TEST_NODE_ID2.toString()));
		assertThat("Range 2 sources", nlt.get(1).get("source_ids"), equalTo(TEST_SOURCE_ID));
	}

	private List<Map<String, Object>> datumRows() {
		return jdbcTemplate
				.queryForList("select * from solardatum.da_datum order by node_id, source_id, ts");
	}

	private List<Map<String, Object>> diffSum(Long[] nodeIds, String[] sourceIds) {
		return jdbcTemplate.queryForList(
		// @formatter:off
				"SELECT node_id, source_id, solarcommon.jsonb_diffsum_object(jdata_a ORDER BY ts)::text AS jdata_a "
						+ "FROM solardatum.da_datum "
						+ "WHERE node_id = ANY(?::bigint[]) "
						+ "AND source_id = ANY(?::text[]) "
						+ "GROUP BY node_id, source_id "
						+ "ORDER BY node_id, source_id",
				// @formatter:on
				"{" + StringUtils.commaDelimitedStringFromCollection(Arrays.asList(nodeIds)) + "}",
				"{" + Arrays.stream(sourceIds).collect(Collectors.joining(",")) + "}");
	}

	@Test
	public void diffSumNoData() {
		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		assertThat("Empty result", data, hasSize(0));
	}

	private interface SamplePopulator {

		void populateSample(GeneralNodeDatumSamples sample, int i);
	}

	private long populateDiffSumDatum(long start, int count, int firstReading, int step) {
		return populateDiffSumDatum(start, count, firstReading, step, null);
	}

	private long populateDiffSumDatum(long start, int count, int firstReading, int step,
			SamplePopulator populator) {
		long date = start;
		for ( long i = 0; i < count; i++ ) {
			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			s.putAccumulatingSampleValue("foo", ((i * step) + firstReading));
			if ( populator != null ) {
				populator.populateSample(s, (int) i);
			}
			insertDatum(date, TEST_NODE_ID, TEST_SOURCE_ID, s);
			date += 1000L;
		}
		return date;
	}

	@Test
	public void diffSumOneRow() {
		long start = System.currentTimeMillis();
		populateDiffSumDatum(start, 1, 1, 1);

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(3));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 0));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 1));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 1));
	}

	@Test
	public void diffSumTwoRows() {
		long start = System.currentTimeMillis();
		populateDiffSumDatum(start, 2, 1, 1);

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(3));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 1));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 1));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 2));
	}

	@Test
	public void diffSumThreeRows() {
		long start = System.currentTimeMillis();
		populateDiffSumDatum(start, 3, 1, 1);

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(3));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 1));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 1));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 3));
	}

	@Test
	public void diffSumFourRows() {
		long start = System.currentTimeMillis();
		start = populateDiffSumDatum(start, 2, 1, 1); // 1-2, diff 1
		populateDiffSumDatum(start, 2, 1000, 100); // 1000-1100, diff of 100

		log.debug("Raw data: {}", StringUtils.delimitedStringFromCollection(datumRows(), ",\n"));

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(3));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 101));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 1));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 1100));
	}

	@Test
	public void diffSumTwoRowsIntroducedProp() {
		long start = System.currentTimeMillis();
		populateDiffSumDatum(start, 2, 0, 1, new SamplePopulator() {

			@Override
			public void populateSample(GeneralNodeDatumSamples sample, int i) {
				if ( i > 0 ) {
					sample.putAccumulatingSampleValue("bar", 20L);
				}

			}
		});

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(6));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 1));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 0));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 1));
		assertThat("Result jdata_a bar", a, hasEntry("bar", 0));
		assertThat("Result jdata_a bar_start", a, hasEntry("bar_start", 20));
		assertThat("Result jdata_a bar_end", a, hasEntry("bar_end", 20));
	}

	@Test
	public void diffSumFourRowsIntroducedPropSecondPair() {
		long start = System.currentTimeMillis();
		start = populateDiffSumDatum(start, 2, 0, 1); // 0-1, diff 1

		populateDiffSumDatum(start, 2, 100, 2, new SamplePopulator() {

			// foo: 100-102, diff 2
			// bar: 20-40, diff 20

			@Override
			public void populateSample(GeneralNodeDatumSamples sample, int i) {
				sample.putAccumulatingSampleValue("bar", 20L + (i * 20));
			}
		});

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(6));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 3));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 0));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 102));
		assertThat("Result jdata_a bar", a, hasEntry("bar", 20));
		assertThat("Result jdata_a bar_start", a, hasEntry("bar_start", 20));
		assertThat("Result jdata_a bar_end", a, hasEntry("bar_end", 40));
	}

	@Test
	public void diffSumFourRowsChangePropSecondPair() {
		long start = System.currentTimeMillis();
		start = populateDiffSumDatum(start, 2, 0, 1); // 0-1, diff 1

		populateDiffSumDatum(start, 2, 100, 2, new SamplePopulator() {

			// bar: 20-40, diff 20

			@Override
			public void populateSample(GeneralNodeDatumSamples sample, int i) {
				sample.putAccumulatingSampleValue("foo", null);
				sample.putAccumulatingSampleValue("bar", 20L + (i * 20));
			}
		});

		List<Map<String, Object>> data = diffSum(new Long[] { TEST_NODE_ID },
				new String[] { TEST_SOURCE_ID });
		log.debug("Got data: {}", data);
		assertThat("Got result", data, hasSize(1));

		Map<String, Object> r = data.get(0);
		assertThat("Result prop count", r.entrySet(), hasSize(3));
		assertThat("Result node", r, hasEntry("node_id", TEST_NODE_ID));
		assertThat("Result source", r, hasEntry("source_id", TEST_SOURCE_ID));

		Map<String, Object> a = JsonUtils.getStringMap((String) r.get("jdata_a"));
		assertThat("Result jdata_a prop count", a.entrySet(), hasSize(6));
		assertThat("Result jdata_a foo", a, hasEntry("foo", 1));
		assertThat("Result jdata_a foo_start", a, hasEntry("foo_start", 0));
		assertThat("Result jdata_a foo_end", a, hasEntry("foo_end", 1));
		assertThat("Result jdata_a bar", a, hasEntry("bar", 20));
		assertThat("Result jdata_a bar_start", a, hasEntry("bar_start", 20));
		assertThat("Result jdata_a bar_end", a, hasEntry("bar_end", 40));
	}

}
