/* ==================================================================
 * DaoLocationBizTest.java - Jun 12, 2011 8:57:22 PM
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

package net.solarnetwork.central.dras.biz.dao.test.ibatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import net.solarnetwork.central.dras.biz.LocationBiz;
import net.solarnetwork.central.dras.biz.dao.DaoLocationBiz;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.LocationCriteria;
import net.solarnetwork.central.dras.support.SimpleLocationFilter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link LocationBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class DaoLocationBizTest extends AbstractTestSupport {

	@Autowired private LocationDao locationDao;
	@Autowired private DaoLocationBiz locationBiz;
	
	@Test
	public void getNoLocation() {
		Location loc = locationBiz.getLocation(-8888L);
		assertNull(loc);
	}
	
	@Test
	public void getLocation() {
		setupTestLocation();
		Location loc = locationBiz.getLocation(TEST_LOCATION_ID);
		assertNotNull(loc);
		assertEquals(TEST_LOCATION_ID, loc.getId());
	}
	
	@Test
	public void findLocationBox() {
		setupTestLocation();
		
		LocationCriteria criteria = new LocationCriteria();
		SimpleLocationFilter filter = (SimpleLocationFilter)criteria.getSimpleFilter();
		filter.setLatitude(0.0);
		filter.setLongitude(0.0);
		filter.setBoxLatitude(2.0);
		filter.setBoxLongitude(2.0);
		
		List<Match> matches = locationBiz.findLocations(criteria, null);
		assertNotNull(matches);
		assertEquals(0, matches.size());

		// setup location with lat/long
		Location loc = locationDao.get(TEST_LOCATION_ID);
		loc.setLatitude(1.0);
		loc.setLongitude(1.0);
		locationDao.store(loc);
		
		matches = locationBiz.findLocations(criteria, null);
		assertNotNull(matches);
		assertEquals(1, matches.size());
		assertEquals(TEST_LOCATION_ID, matches.get(0).getId());
	}
	
	@Test
	public void createLocation() {
		Location loc = new Location();
		loc.setCountry("NZ");
		loc.setGxp("foo.gxp");
		loc.setIcp("foo.icp");
		Location entity = locationBiz.storeLocation(loc);
		assertNotNull(entity.getCreated());
		assertNotNull(entity.getId());
		assertEquals(loc.getCountry(), entity.getCountry());
		assertEquals(loc.getGxp(), entity.getGxp());
		assertEquals(loc.getIcp(), entity.getIcp());
	}
}
