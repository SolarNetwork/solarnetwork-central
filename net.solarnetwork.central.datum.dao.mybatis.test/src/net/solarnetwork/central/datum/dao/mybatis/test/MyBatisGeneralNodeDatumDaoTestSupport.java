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
import java.sql.Date;
import java.sql.Timestamp;
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

	protected List<Map<String, Object>> getDatumAggregateDaily() {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_daily order by node_id,ts_start,source_id");
	}

	protected List<Map<String, Object>> getDatumAggregateMonthly() {
		return jdbcTemplate.queryForList(
				"select * from solaragg.agg_datum_monthly order by node_id,ts_start,source_id");
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

}