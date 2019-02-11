/* ==================================================================
 * AggregateStoredProcedureTests.java - 24/07/2018 2:54:14 PM
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for various helper stored procedures dealing with aggregate
 * calculations.
 * 
 * @author matt
 * @version 1.1
 * @since 2.10
 */
public class AggregateStoredProcedureTests extends MyBatisGeneralNodeDatumDaoTestSupport {

	private final static String TEST_SOURCE_ID = "test.source";

	private void insertAggDatumHourlyRow(long ts, Long nodeId, String sourceId,
			Map<String, Object> iData, Map<String, Object> aData, Map<String, Object> jMeta) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_hourly (ts_start,local_date,node_id,source_id,jdata_i,jdata_a,jmeta)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null));
	}

	private void insertAggDatumDailyRow(long ts, Long nodeId, String sourceId, Map<String, Object> iData,
			Map<String, Object> aData, Map<String, Object> jMeta) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_daily (ts_start,local_date,node_id,source_id,jdata_i,jdata_a,jmeta)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null));
	}

	@Test
	public void calcAggDatumAggHourlyToDayNoData() {
		DateTime start = new DateTime(2018, 7, 1, 0, 0, DateTimeZone.UTC);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.calc_agg_datum_agg(?,ARRAY[?],?,?, 'h')", TEST_NODE_ID,
				TEST_SOURCE_ID, new Timestamp(start.getMillis()),
				new Timestamp(start.plusDays(1).getMillis()));
		assertThat("Result rows", rows, hasSize(0));
	}

	@Test
	public void calcAggDatumAggHourlyToDay() {
		DateTime start = new DateTime(2018, 7, 1, 0, 0, 0, DateTimeZone.UTC);
		Map<String, Object> iData = new HashMap<String, Object>(2);
		iData.put("foo", 1.234f);
		iData.put("bar", 123.4f);
		Map<String, Object> aData = new HashMap<String, Object>(2);
		aData.put("bim", 123L);
		aData.put("bam", 456L);
		Map<String, Object> jMeta = new HashMap<String, Object>();
		Map<String, Object> iMeta = new HashMap<String, Object>();
		iMeta.put("count", 30);
		iMeta.put("min", 0.12f);
		iMeta.put("max", 2.3f);
		jMeta.put("i", iMeta);

		// put in >1 day of data, just to verify we only pull back 1 day
		for ( int i = 0; i < 30; i += 2 ) {
			DateTime ts = start.plusHours(i);
			iData.put("foo", 1.234f + i); // varies
			iData.put("bar", 123.4f); // static
			insertAggDatumHourlyRow(ts.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, iData, aData, jMeta);

		}
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT ts_start,node_id,source_id,jdata::text,jmeta::text FROM solaragg.calc_agg_datum_agg(?,ARRAY[?],?,?, 'h')",
				TEST_NODE_ID, TEST_SOURCE_ID, new Timestamp(start.getMillis()),
				new Timestamp(start.plusDays(1).getMillis()));
		assertThat("Result rows", rows, hasSize(1));
		Map<String, Object> row = rows.get(0);
		Map<String, Object> expectedData = JsonUtils.getStringMap(
				"{\"a\": {\"bam\": 5472, \"bim\": 1476}, \"i\": {\"bar\": 123.4, \"foo\": 12.234, \"foo_min\": 1.234, \"foo_max\": 23.234}}");
		assertThat("jdata", JsonUtils.getStringMap((String) row.get("jdata")), equalTo(expectedData));
		Map<String, Object> expectedMeta = JsonUtils.getStringMap(
				"{\"i\": {\"bar\": {\"count\": 12}, \"foo\": {\"max\": 23.234, \"min\": 1.234, \"count\": 12}}}");
		assertThat("jmeta", JsonUtils.getStringMap((String) row.get("jmeta")), equalTo(expectedMeta));
	}

	@Test
	public void calcAggDatumAggDailyToMonthNoData() {
		DateTime start = new DateTime(2018, 7, 1, 0, 0, DateTimeZone.UTC);
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.calc_agg_datum_agg(?,ARRAY[?],?,?, 'd')", TEST_NODE_ID,
				TEST_SOURCE_ID, new Timestamp(start.getMillis()),
				new Timestamp(start.plusMonths(1).getMillis()));
		assertThat("Result rows", rows, hasSize(0));
	}

	@Test
	public void calcAggDatumAggDailyToMonth() {
		DateTime start = new DateTime(2018, 7, 1, 0, 0, 0, DateTimeZone.UTC);
		DateTime end = null;
		Map<String, Object> iData = new HashMap<String, Object>(2);
		iData.put("foo", 1.234f);
		iData.put("bar", 123.4f);
		Map<String, Object> aData = new HashMap<String, Object>(2);
		aData.put("bim", 123L);
		aData.put("bam", 456L);
		Map<String, Object> jMeta = new HashMap<String, Object>();
		Map<String, Object> iMeta = new HashMap<String, Object>();
		iMeta.put("count", 30);
		iMeta.put("min", 0.12f);
		iMeta.put("max", 2.3f);
		jMeta.put("i", iMeta);

		// put in >1 day of data, just to verify we only pull back 1 day
		for ( int i = 0; i < 35; i += 1 ) {
			DateTime ts = start.plusDays(i);
			iData.put("foo", 1.234f + i); // varies
			iData.put("bar", 123.4f); // static
			insertAggDatumDailyRow(ts.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, iData, aData, jMeta);
			if ( ts.getMonthOfYear() == start.getMonthOfYear() ) {
				end = ts;
			}
		}

		log.debug("Date range: {} - {}", start, end);

		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT ts_start,node_id,source_id,jdata::text,jmeta::text FROM solaragg.calc_agg_datum_agg(?,ARRAY[?],?,?, 'd')",
				TEST_NODE_ID, TEST_SOURCE_ID, new Timestamp(start.getMillis()),
				new Timestamp(start.plusMonths(1).getMillis()));
		assertThat("Result rows", rows, hasSize(1));
		Map<String, Object> row = rows.get(0);
		Map<String, Object> expectedData = JsonUtils.getStringMap(
				"{\"a\": {\"bam\": 14136, \"bim\": 3813}, \"i\": {\"bar\": 123.4, \"foo\": 16.234, \"foo_min\": 1.234, \"foo_max\": 31.234}}");
		assertThat("jdata", JsonUtils.getStringMap((String) row.get("jdata")), equalTo(expectedData));
		Map<String, Object> expectedMeta = JsonUtils.getStringMap(
				"{\"i\": {\"bar\": {\"count\": 31}, \"foo\": {\"max\": 31.234, \"min\": 1.234, \"count\": 31}}}");
		assertThat("jmeta", JsonUtils.getStringMap((String) row.get("jmeta")), equalTo(expectedMeta));
	}

	@Test
	public void calcReadingAggDatumWithOneHourData() {
		DateTime start = new DateTime(2018, 7, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatum> data = createSampleData(59, start);
		for ( GeneralNodeDatum d : data ) {
			dao.store(d);
		}

		GeneralNodeDatumReadingAggregate r = calculateDatumDiffOver(TEST_NODE_ID, TEST_SOURCE_ID, start,
				start.plusHours(1));

		Integer whStart = data.get(0).getSamples().getAccumulatingSampleInteger("wattHours");
		assertThat("agg reading start sample data", r.getAs(), hasEntry("wattHours", whStart));

		Integer whFinal = data.get(data.size() - 1).getSamples()
				.getAccumulatingSampleInteger("wattHours");
		assertThat("agg reading final sample data", r.getAf(), hasEntry("wattHours", whFinal));

		Integer whDiff = whFinal - whStart;
		assertThat("agg reading diff sample data", r.getA(), hasEntry("wattHours", whDiff));
	}

}
