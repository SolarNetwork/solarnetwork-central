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
 */

package net.solarnetwork.central.in.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.easymock.EasyMock;
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
 * @version 1.2
 */
@ContextConfiguration
public class DaoDataCollectorBizTest extends AbstractCentralTransactionalTest {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID_2 = "test.source.2";

	@Autowired
	private DaoDataCollectorBiz biz;

	private DatumMetadataBiz datumMetadataBiz;

	private Datum lastDatum;

	@Before
	public void setup() {
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		setupTestNode();
		setupTestPriceLocation();
		setAuthenticatedNode(TEST_NODE_ID);
	}

	private DayDatum newDayDatumInstance() {
		DayDatum d = new DayDatum();
		d.setSkyConditions("Sunny");
		d.setDay(new LocalDate(2011, 10, 21));
		d.setNodeId(TEST_NODE_ID);
		d.setSunrise(new LocalTime(6, 40));
		d.setSunset(new LocalTime(18, 56));
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
		SourceLocationFilter filter = new SourceLocationFilter(TEST_PRICE_SOURCE_NAME, TEST_LOC_NAME);
		List<SourceLocationMatch> results = biz.findPriceLocations(filter);
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
		SourceLocationFilter filter = new SourceLocationFilter(TEST_WEATHER_SOURCE_NAME, TEST_LOC_NAME);
		List<SourceLocationMatch> results = biz.findWeatherLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		SourceLocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_WEATHER_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_ID, loc.getLocationId());
		assertEquals(TEST_LOC_NAME, loc.getLocationName());
		assertEquals(TEST_WEATHER_SOURCE_NAME, loc.getSourceName());
	}

	@Test
	public void findLocation() {
		SolarLocation filter = new SolarLocation();
		filter.setCountry(TEST_LOC_COUNTRY);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);
		List<LocationMatch> results = biz.findLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		LocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_COUNTRY, loc.getCountry());
		assertEquals(TEST_LOC_POSTAL_CODE, loc.getPostalCode());
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		datumMetadataBiz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.replay(datumMetadataBiz);

		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.verify(datumMetadataBiz);
	}

}
