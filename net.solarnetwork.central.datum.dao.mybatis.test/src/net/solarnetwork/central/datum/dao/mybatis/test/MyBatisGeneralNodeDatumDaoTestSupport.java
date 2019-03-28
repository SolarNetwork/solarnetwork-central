/* ==================================================================
 * MyBatisGeneralNodeDatumDaoTestSupport.java - 8/02/2019 9:58:23 am
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Support for {@link MyBatisGeneralNodeDatumDao} tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class MyBatisGeneralNodeDatumDaoTestSupport extends AbstractMyBatisDaoTestSupport {

	protected static final String TEST_SOURCE_ID = "test.source";
	protected static final String TEST_2ND_SOURCE = "2nd source";
	protected static final Long TEST_2ND_NODE = -200L;

	protected MyBatisGeneralNodeDatumDao dao;

	protected GeneralNodeDatum lastDatum;

	@Autowired
	protected PlatformTransactionManager txManager;

	@Before
	public void setup() {
		dao = new MyBatisGeneralNodeDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		dao.getLoadingSupport().setDataSource(jdbcTemplate.getDataSource());
		dao.getLoadingSupport().setTransactionManager(txManager);
	}

	protected GeneralNodeDatum getTestInstance() {
		return getTestInstance(new DateTime(), TEST_NODE_ID, TEST_SOURCE_ID);
	}

	protected GeneralNodeDatum getTestInstance(DateTime created, Long nodeId, String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(created);
		datum.setNodeId(nodeId);
		datum.setPosted(created);
		datum.setSourceId(sourceId);

		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("watt_hours", 4123);
		samples.setAccumulating(accum);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setStatus(msgs);

		return datum;
	}

	protected int datumRowCount() {
		return jdbcTemplate.queryForObject("select count(*) from solardatum.da_datum", Integer.class);
	}

	protected Date[] sqlDates(String... str) {
		return Arrays.stream(str)
				.map(s -> new Date(ISODateTimeFormat.localDateParser().parseLocalDate(s)
						.toDateTimeAtStartOfDay().getMillis()))
				.collect(Collectors.toList()).toArray(new Date[str.length]);
	}

	protected List<Date> sqlDatesFromLocalDates(List<Map<String, Object>> rows) {
		//List<Map<String, Object>> dayData = getDatumAggregateDaily();
		return rows.stream().map(d -> (Date) d.get("local_date")).collect(toList());
	}

	protected List<Map<String, Object>> getDatum() {
		return jdbcTemplate
				.queryForList("select * from solardatum.da_datum order by node_id,ts,source_id");
	}

	protected List<Map<String, Object>> getDatumAggregateHourly() {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_hourly order by node_id,ts_start,source_id");
	}

	protected List<GeneralNodeDatumReadingAggregate> getDatumReadingAggregteHourly() {
		List<Map<String, Object>> rows = jdbcTemplate
				.queryForList("SELECT jsonb_strip_nulls(jsonb_build_object("
						+ "'date', extract('epoch' from ts_start)::int8 * 1000::int8, "
						+ "'nodeId', node_id, 'sourceId', source_id, "
						+ "'as', jdata_as, 'af', jdata_af, 'a', jdata_ad))::text AS jdata "
						+ "FROM solaragg.agg_datum_hourly ORDER BY ts_start, node_id, source_id");

		return rows.stream().map(d -> JsonUtils.getObjectFromJSON((String) d.get("jdata"),
				GeneralNodeDatumReadingAggregate.class)).collect(toList());
	}

	protected List<GeneralNodeDatumReadingAggregate> getDatumReadingAggregteDaily() {
		List<Map<String, Object>> rows = jdbcTemplate
				.queryForList("SELECT jsonb_strip_nulls(jsonb_build_object("
						+ "'date', extract('epoch' from ts_start)::int8 * 1000::int8, "
						+ "'nodeId', node_id, 'sourceId', source_id, "
						+ "'as', jdata_as, 'af', jdata_af, 'a', jdata_ad))::text AS jdata "
						+ "FROM solaragg.agg_datum_daily ORDER BY ts_start, node_id, source_id");

		return rows.stream().map(d -> JsonUtils.getObjectFromJSON((String) d.get("jdata"),
				GeneralNodeDatumReadingAggregate.class)).collect(toList());
	}

	protected List<GeneralNodeDatumReadingAggregate> getDatumReadingAggregteMonthly() {
		List<Map<String, Object>> rows = jdbcTemplate
				.queryForList("SELECT jsonb_strip_nulls(jsonb_build_object("
						+ "'date', extract('epoch' from ts_start)::int8 * 1000::int8, "
						+ "'nodeId', node_id, 'sourceId', source_id, "
						+ "'as', jdata_as, 'af', jdata_af, 'a', jdata_ad))::text AS jdata "
						+ "FROM solaragg.agg_datum_monthly ORDER BY ts_start, node_id, source_id");

		return rows.stream().map(d -> JsonUtils.getObjectFromJSON((String) d.get("jdata"),
				GeneralNodeDatumReadingAggregate.class)).collect(toList());
	}

	protected List<Map<String, Object>> getDatumAggregateDaily() {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_daily order by node_id,ts_start,source_id");
	}

	protected List<Map<String, Object>> getDatumAggregateDaily(Long nodeId) {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_daily where node_id = ? order by ts_start,source_id",
				nodeId);
	}

	protected List<Map<String, Object>> getDatumAggregateMonthly() {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_monthly order by node_id,ts_start,source_id");
	}

	protected List<Map<String, Object>> getDatumRanges() {
		return jdbcTemplate
				.queryForList("select * from solardatum.da_datum_range order by node_id,source_id");
	}

	protected void insertResetDatumAuxiliaryRecord(DateTime date, Long nodeId, String sourceId,
			Map<String, Number> finalSamples, Map<String, Number> startSamples) {
		jdbcTemplate.update(
				"INSERT INTO solardatum.da_datum_aux(ts, node_id, source_id, atype, updated, jdata_af, jdata_as) "
						+ "VALUES (?, ?, ?, 'Reset'::solardatum.da_datum_aux_type, CURRENT_TIMESTAMP, ?::jsonb, ?::jsonb)",
				new Timestamp(date.getMillis()), nodeId, sourceId,
				JsonUtils.getJSONString(finalSamples, null),
				JsonUtils.getJSONString(startSamples, null));
	}

	protected List<GeneralNodeDatum> createSampleData(int count, DateTime start) {
		return createSampleData(count, start, TEST_NODE_ID, TEST_SOURCE_ID);
	}

	protected List<GeneralNodeDatum> createSampleData(int count, DateTime start, Long nodeId,
			String sourceId) {
		List<GeneralNodeDatum> data = new ArrayList<>(4);
		long wh = (long) (Math.random() * 1000000000.0);
		for ( int i = 0; i < count; i++ ) {
			GeneralNodeDatum d = new GeneralNodeDatum();
			d.setNodeId(nodeId);
			d.setCreated(start.plusMinutes(i));
			d.setSourceId(sourceId);

			GeneralNodeDatumSamples s = new GeneralNodeDatumSamples();
			int watts = (int) (Math.random() * 50000);
			s.putInstantaneousSampleValue("watts", watts);
			wh += (long) (watts / 60.0);
			s.putAccumulatingSampleValue("wattHours", wh);
			d.setSamples(s);
			data.add(d);
		}
		return data;
	}

	protected GeneralNodeDatumReadingAggregate calculateDatumDiffOver(Long nodeId, String sourceId,
			DateTime start, DateTime end) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT jsonb_strip_nulls(jsonb_build_object("
						+ "'as', jdata->'as', 'af', jdata->'af', 'a', jdata->'a'"
						+ "))::text AS jdata FROM solardatum.calculate_datum_diff_over(?, ?, ?, ?)",
				nodeId, sourceId, new Timestamp(start.getMillis()), new Timestamp(end.getMillis()));

		assertThat(rows, hasSize(1));
		Map<String, Object> d = rows.get(0);
		return JsonUtils.getObjectFromJSON((String) d.get("jdata"),
				GeneralNodeDatumReadingAggregate.class);
	}

	protected void insertAggDatumHourlyRow(long ts, Long nodeId, String sourceId, Map<String, ?> iData,
			Map<String, ?> aData, Map<String, ?> jMeta, Map<String, ?> asData, Map<String, ?> afData,
			Map<String, ?> adData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_hourly (ts_start,local_date,node_id,source_id,jdata_i,jdata_a,jmeta,jdata_as,jdata_af,jdata_ad)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null), JsonUtils.getJSONString(asData, null),
				JsonUtils.getJSONString(afData, null), JsonUtils.getJSONString(adData, null));
	}

	protected void insertAggDatumDailyRow(long ts, Long nodeId, String sourceId, Map<String, ?> iData,
			Map<String, ?> aData, Map<String, ?> jMeta, Map<String, ?> asData, Map<String, ?> afData,
			Map<String, ?> adData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_daily (ts_start,local_date,node_id,source_id,jdata_i,jdata_a,jmeta,jdata_as,jdata_af,jdata_ad)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null), JsonUtils.getJSONString(asData, null),
				JsonUtils.getJSONString(afData, null), JsonUtils.getJSONString(adData, null));
	}

	protected void insertAggDatumMonthlyRow(long ts, Long nodeId, String sourceId, Map<String, ?> iData,
			Map<String, ?> aData, Map<String, ?> jMeta, Map<String, ?> asData, Map<String, ?> afData,
			Map<String, ?> adData) {
		jdbcTemplate.update(
				"INSERT INTO solaragg.agg_datum_monthly (ts_start,local_date,node_id,source_id,jdata_i,jdata_a,jmeta,jdata_as,jdata_af,jdata_ad)"
						+ " VALUES (?,?,?,?,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb,?::jsonb)",
				new Timestamp(ts), new Timestamp(ts), nodeId, sourceId,
				JsonUtils.getJSONString(iData, null), JsonUtils.getJSONString(aData, null),
				JsonUtils.getJSONString(jMeta, null), JsonUtils.getJSONString(asData, null),
				JsonUtils.getJSONString(afData, null), JsonUtils.getJSONString(adData, null));
	}
}
