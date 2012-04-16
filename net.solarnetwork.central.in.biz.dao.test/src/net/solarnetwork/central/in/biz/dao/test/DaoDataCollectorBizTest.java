/* ==================================================================
 * DaoDataCollectorBizTest.java - Oct 23, 2011 2:49:59 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.in.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test case for the {@link DaoDataCollectorBiz} class.
 * 
 * @author matt
 * @version $Revision$
 */
@ContextConfiguration
public class DaoDataCollectorBizTest extends AbstractCentralTransactionalTest {

	@Autowired DaoDataCollectorBiz biz;
	
	private Datum lastDatum;
	
	@Before
	public void setup() {
		setupTestNode();
		setupTestPriceLocation();
	}
	
	private DayDatum newDayDatumInstance() {
		DayDatum d = new DayDatum();
		d.setSkyConditions("Sunny");
		d.setDay(new LocalDate(2011, 10, 21));
		d.setNodeId(TEST_NODE_ID);
		d.setSunrise(new LocalTime(6, 40));
		d.setSunset(new LocalTime(18,56));
		return d;
	}
	
	@Test
	public void collectDay() {
		DayDatum d = newDayDatumInstance();
		DayDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getDay(), result.getDay());
		assertNotNull(d.getLocationId());
		lastDatum = d;
	}
	
	@Test
	public void collectSameDay() {
		collectDay();
		DayDatum d = newDayDatumInstance();
		DayDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertEquals(lastDatum.getId(), result.getId());
	}
	
	@Test
	public void findPriceLocation() {
		SourceLocationFilter filter = new SourceLocationFilter(
				TEST_PRICE_SOURCE_NAME, TEST_LOC_NAME);
		List<SourceLocationMatch> results = biz.findPriceLocation(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		SourceLocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_PRICE_SOURCE_ID, loc.getId());
		assertEquals(TEST_LOC_ID, loc.getLocationId());
		assertEquals(TEST_LOC_NAME, loc.getLocationName());
		assertEquals(TEST_PRICE_SOURCE_NAME, loc.getSourceName());
}
	
	@Test
	public void findWeatherLocation() {
		SourceLocationFilter filter = new SourceLocationFilter(
				TEST_WEATHER_SOURCE_NAME, TEST_LOC_NAME);
		List<SourceLocationMatch> results = biz.findWeatherLocation(filter);
		assertNotNull(results);
		assertEquals(1, results.size());
		
		SourceLocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_WEATHER_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_ID, loc.getLocationId());
		assertEquals(TEST_LOC_NAME, loc.getLocationName());
		assertEquals(TEST_WEATHER_SOURCE_NAME, loc.getSourceName());
	}
	
}
