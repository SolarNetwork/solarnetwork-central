/* ==================================================================
 * UtilityStoredProcedureTests.java - 24/05/2018 2:44:38 PM
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.test.SystemPropertyMatchTestRule;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Test cases for some utility stored procedures.
 * 
 * <p>
 * Note these tests only run if a {@literal profile} system property is set.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class UtilityStoredProcedureTests extends AbstractMyBatisDaoTestSupport {

	private static final Long TEST_NODE_ID2 = -2L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID2 = "test.source.2";

	private static final int DATA_SET_COUNT = 500;
	private static final DateTime DATE_MIN = new DateTime(2018, 5, 1, 13, 0, DateTimeZone.UTC);
	private static final DateTime DATE_MAX = DATE_MIN.plusMinutes((DATA_SET_COUNT - 1) * 10);

	private MyBatisGeneralNodeDatumDao dao;

	private static final Logger log = LoggerFactory.getLogger(UtilityStoredProcedureTests.class);

	@ClassRule
	public static SystemPropertyMatchTestRule PROFILE_RULE = new SystemPropertyMatchTestRule("profile");

	@Before
	public void setup() {
		dao = new MyBatisGeneralNodeDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralNodeDatumSamples getTestSamplesInstance() {
		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		instants.put("voltage", 123.6f);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("wattHours", 4123L);
		samples.setAccumulating(accum);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setStatus(msgs);

		return samples;
	}

	private GeneralNodeDatum getTestInstance(DateTime now, DateTime created,
			GeneralNodeDatumSamples samples) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(created);
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(now);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setSamples(samples);
		return datum;
	}

	private void insertTestDataSet() {
		// insert data
		final DateTime now = new DateTime();
		final GeneralNodeDatumSamples samples = getTestSamplesInstance();
		for ( int i = 0; i < DATA_SET_COUNT; i++ ) {
			GeneralNodeDatum d = getTestInstance(now, DATE_MIN.plusMinutes(i * 10), samples);
			d.setNodeId(i % 2 == 0 ? TEST_NODE_ID : TEST_NODE_ID2);
			dao.store(d);
			d.setSourceId(TEST_SOURCE_ID2);
			dao.store(d);
		}

		// immediately process reporting data as getting all sources scans daily table
		processAggregateStaleData();
	}

	private void profileQuery(final String description, String sql) {
		final Long[] allNodeIds = new Long[] { TEST_NODE_ID, TEST_NODE_ID2 };
		final long tsStart = DATE_MIN.getMillis();
		final long tsEnd = DATE_MAX.getMillis() + (10 * 60 * 1000L);
		final long tsHalf = tsEnd - tsStart;
		final int iterationCount = 10000;
		jdbcTemplate.execute(new ConnectionCallback<Object>() {

			@Override
			public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
				PreparedStatement ps = conn.prepareStatement(QUERY_JSONB_SUM_WATTS_BY_NODE,
						ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
				long totalTime = 0;

				for ( int i = 0; i < iterationCount; i++ ) {
					if ( (i + 1) % 1000 == 0 ) {
						log.info("Profiling {} {}...", description, i + 1);
					}
					long start = tsStart + (long) (Math.random() * tsHalf);
					long end = start + (long) (Math.random() * tsHalf);
					Array loc = conn.createArrayOf("bigint", allNodeIds);
					ps.setArray(1, loc);
					ps.setTimestamp(2, new Timestamp(start));
					ps.setTimestamp(3, new Timestamp(end));

					final long s = System.currentTimeMillis();
					@SuppressWarnings("unused")
					ResultSet rs = ps.executeQuery();
					final long diff = System.currentTimeMillis() - s;
					totalTime += diff;
				}

				log.info("Average query time {}: {}ms", description,
						totalTime / (double) iterationCount);
				return null;
			}
		});
	}

	// @formatter:off
	private static final String QUERY_JSONB_SUM_WATTS_BY_NODE = 
			"SELECT ts, node_id, solarcommon.jsonb_sum(jdata_i->'watts') "
			+ "FROM solardatum.da_datum "
			+ "WHERE node_id = ANY(?::bigint[]) AND ts >= ? AND ts < ?"
			+ "GROUP BY ts, node_id";
	// @formatter:on

	@Test
	public void profileJsonObjAgg() {
		insertTestDataSet();
		profileQuery("solarcommon.jsonb_sum", QUERY_JSONB_SUM_WATTS_BY_NODE);

	}

	// @formatter:off
	private static final String QUERY_JSON_CAST_SUM_WATTS_BY_NODE = 
			"SELECT ts, node_id, sum((d.jdata_i->>'watts')::double precision) "
			+ "FROM solardatum.da_datum "
			+ "WHERE node_id = ANY(?::bigint[]) AND ts >= ? AND ts < ?"
			+ "GROUP BY ts, node_id";
	// @formatter:on

	@Test
	public void profileJsonCastAgg() {
		insertTestDataSet();
		profileQuery("sum::double precision", QUERY_JSON_CAST_SUM_WATTS_BY_NODE);
	}

	// @formatter:off
	private static final String QUERY_JSONB_OBJ_SUM_BY_NODE = 
			"SELECT ts, node_id, solarcommon.jsonb_sum_object(d.jdata_i) "
			+ "FROM solardatum.da_datum "
			+ "WHERE node_id = ANY(?::bigint[]) AND ts >= ? AND ts < ?"
			+ "GROUP BY ts, node_id";
	// @formatter:on

	@Test
	public void profileJsonbSumObjectAgg() {
		insertTestDataSet();
		profileQuery("solarcommon.jsonb_sum_object", QUERY_JSONB_OBJ_SUM_BY_NODE);
	}

}
