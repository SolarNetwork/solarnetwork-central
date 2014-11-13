/* ==================================================================
 * MyBatisDayDatumDaoTests.java - Nov 13, 2014 8:30:41 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import java.util.Iterator;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisDayDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.DayDatumMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisDayDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDayDatumDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisDayDatumDao dao;

	private DayDatum lastDatum;

	@Before
	public void setUp() {
		dao = new MyBatisDayDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastDatum = null;
	}

	@Test
	public void storeNew() {
		DayDatum datum = new DayDatum();
		datum.setCreated(new DateTime());
		datum.setDay(new LocalDate());
		datum.setLocationId(TEST_WEATHER_LOC_ID);
		datum.setSunrise(new LocalTime());
		datum.setSunset(new LocalTime());
		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(DayDatum src, DayDatum entity) {
		assertNotNull("DayDatum should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getDay(), entity.getDay());
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getSunrise(), entity.getSunrise());
		assertEquals(src.getSunset(), entity.getSunset());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		DayDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void getByDate() {
		storeNew();
		DayDatum datum = dao.getDatumForDate(TEST_NODE_ID, lastDatum.getDay());
		validate(lastDatum, datum);
	}

	@Test
	public void findByPostalCode() {
		storeNew();
		SolarLocation locFilter = new SolarLocation();
		locFilter.setPostalCode(TEST_LOC_POSTAL_CODE);
		DatumFilterCommand filter = new DatumFilterCommand(locFilter);

		FilterResults<DayDatumMatch> results = dao.findFiltered(filter, filter.getSortDescriptors(),
				null, null);

		assertNotNull(results);
		assertEquals(Integer.valueOf(1), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		Iterator<DayDatumMatch> itr = results.getResults().iterator();

		DayDatumMatch match = itr.next();
		assertEquals(lastDatum.getId(), match.getId());
	}

}
