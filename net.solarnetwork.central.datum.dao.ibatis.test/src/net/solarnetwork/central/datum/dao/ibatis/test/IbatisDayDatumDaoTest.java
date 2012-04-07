/* ==================================================================
 * IbatisDayDatumDaoTest.java - Sep 12, 2011 1:12:14 PM
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

package net.solarnetwork.central.datum.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.solarnetwork.central.datum.dao.ibatis.IbatisDayDatumDao;
import net.solarnetwork.central.datum.domain.DayDatum;

/**
 * Unit test for the {@link IbatisDayDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisDayDatumDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private IbatisDayDatumDao dao;
	
	private DayDatum lastDatum;
	
	@Before
	public void setUp() throws Exception {
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

}
