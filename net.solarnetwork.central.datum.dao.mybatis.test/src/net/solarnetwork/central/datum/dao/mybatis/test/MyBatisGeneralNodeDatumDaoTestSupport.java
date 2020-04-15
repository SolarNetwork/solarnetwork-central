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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
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

	protected static final String TEST_2ND_SOURCE = "2nd source";
	protected static final Long TEST_2ND_NODE = -200L;

	protected MyBatisGeneralNodeDatumDao dao;

	protected GeneralNodeDatum lastDatum;

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

	protected List<Map<String, Object>> datumRowsMonthly() {
		// @formatter:off
		return jdbcTemplate.queryForList("SELECT ts_start, node_id, source_id"
				+ " , solarcommon.jdata_from_components(jdata_i, jdata_a, jdata_s, jdata_t)::text AS jdata"
				+ ", jdata_as::text AS jdata_as" 
				+ ", jdata_af::text AS jdata_af"
				+ ", jdata_ad::text AS jdata_ad"
				+ " FROM solaragg.agg_datum_monthly ORDER BY ts_start, node_id, source_id");
		// @formatter:on
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

	protected List<Map<String, Object>> findDatumForTimeSpan(Long nodeId, String sourceId, DateTime date,
			String interval) {
		return jdbcTemplate.queryForList(
				"SELECT * FROM solaragg.find_datum_for_time_span(?,?::text[],?,?::interval)", nodeId,
				"{" + sourceId + "}", new Timestamp(date.getMillis()), interval);
	}

}
