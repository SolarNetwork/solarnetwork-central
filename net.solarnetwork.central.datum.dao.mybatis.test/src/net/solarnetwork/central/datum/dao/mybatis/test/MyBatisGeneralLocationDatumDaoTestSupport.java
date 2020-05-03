/* ==================================================================
 * MyBatisGeneralLocationDatumDaoTestSupport.java - 4/05/2020 10:39:22 am
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralLocationDatumDao;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.domain.GeneralLocationDatumSamples;

/**
 * Support for location datum DAO tests.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class MyBatisGeneralLocationDatumDaoTestSupport extends AbstractMyBatisDaoTestSupport {

	protected static final String TEST_SOURCE_ID = "test.source";
	protected static final String TEST_2ND_SOURCE = "2nd source";
	protected static final Long TEST_2ND_NODE = -200L;
	protected static final Long TEST_2ND_LOC = -222L;

	protected MyBatisGeneralLocationDatumDao dao;

	protected GeneralLocationDatum lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisGeneralLocationDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	protected GeneralLocationDatum getTestInstance() {
		return getTestInstance(new DateTime(), TEST_LOC_ID, TEST_SOURCE_ID);
	}

	protected GeneralLocationDatum getTestInstance(DateTime created, Long locationId, String sourceId) {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(locationId);
		datum.setPosted(new DateTime());
		datum.setSourceId(sourceId);

		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
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

	protected List<Map<String, Object>> datumRowsHourly() {
		// @formatter:off
		return jdbcTemplate.queryForList("SELECT d.ts_start, d.loc_id, d.source_id"
				+ " , solaragg.jdata_from_datum(d)::text AS jdata"
				+ " FROM solaragg.agg_loc_datum_hourly d ORDER BY ts_start, loc_id, source_id");
		// @formatter:on
	}

	protected List<Map<String, Object>> datumRowsDaily() {
		// @formatter:off
		return jdbcTemplate.queryForList("SELECT d.ts_start, d.loc_id, d.source_id"
				+ " , solaragg.jdata_from_datum(d)::text AS jdata"
				+ " FROM solaragg.agg_loc_datum_daily d ORDER BY ts_start, loc_id, source_id");
		// @formatter:on
	}

	protected List<Map<String, Object>> datumRowsMonthly() {
		// @formatter:off
		return jdbcTemplate.queryForList("SELECT d.ts_start, d.loc_id, d.source_id"
				+ " , solaragg.jdata_from_datum(d)::text AS jdata"
				+ " FROM solaragg.agg_loc_datum_monthly d ORDER BY ts_start, loc_id, source_id");
		// @formatter:on
	}

}
