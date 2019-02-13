/* ==================================================================
 * MyBatisGeneralNodeDatumDaoFindReadingsDifferenceAggTests.java - 13/02/2019 12:24:44 pm
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;

/**
 * Test cases for querying {@code Difference} style reading aggregates.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisGeneralNodeDatumDaoFindReadingsDifferenceAggTests
		extends MyBatisGeneralNodeDatumDaoTestSupport {

	@Test
	public void findHourly() {
		// given
		final DateTime start = new DateTime(2019, 2, 1, 0, 0, 0, DateTimeZone.forID(TEST_TZ));
		List<GeneralNodeDatumReadingAggregate> readings = new ArrayList<>(10);
		for ( int i = 0; i < 3; i++ ) {
			Map<String, Number> inst = Collections.singletonMap("bim", i);
			Map<String, Number> as = Collections.singletonMap("foo", i);
			Map<String, Number> af = Collections.singletonMap("foo", i + 1);
			Map<String, Number> a = Collections.singletonMap("foo", 1);
			DateTime date = start.plusHours(i);
			readings.add(
					new GeneralNodeDatumReadingAggregate(date, TEST_NODE_ID, TEST_SOURCE_ID, as, af, a));
			insertAggDatumHourlyRow(date.getMillis(), TEST_NODE_ID, TEST_SOURCE_ID, inst, null, null, as,
					af, a);

		}

		// when
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);
		criteria.setSourceId(TEST_SOURCE_ID);
		criteria.setStartDate(start);
		criteria.setEndDate(start.plusHours(3));
		criteria.setAggregate(Aggregation.Hour);

		FilterResults<ReportingGeneralNodeDatumMatch> results = dao.findAggregationFilteredReadings(
				criteria, DatumReadingType.Difference, null, null, null, null);

		// then
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		Map<String, ?> data = results.getResults().iterator().next().getSampleData();
		assertNotNull("Aggregate sample data", data);
		assertNotNull("Aggregate Wh", data.get("watt_hours"));
		assertEquals("Aggregate Wh", Integer.valueOf(10), data.get("watt_hours"));
	}

}
