/* ==================================================================
 * IbatisLocationDaoTest.java - Jun 6, 2011 11:48:16 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This location is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This location is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this location; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.SimpleLocationFilter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link LocationDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisLocationDaoTest extends AbstractIbatisDaoTestSupport {
	
	/** LocationDao to test. */
	@Autowired
	protected LocationDao locationDao;
	
	private Long lastLocationId;
	
	@Before
	public void setup() {
		lastLocationId = null;
	}
	
	@Test
	public void getLocationById() {
		setupTestLocation();
		Location location = locationDao.get(TEST_LOCATION_ID);
		assertNotNull(location);
		assertNotNull(location.getId());
		assertEquals(TEST_LOCATION_ID, location.getId());
		assertEquals(TEST_LOCATION_NAME, location.getName());
		assertEquals("Wellington", location.getLocality());
	}
	
	@Test
	public void getNonExistingLocationById() {
		Location location = locationDao.get(-99999L);
		assertNull(location);
	}
	
	private void validateLocation(Location location, Location entity) {
		assertNotNull("Location should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(location.getCountry(), entity.getCountry());
		assertEquals(location.getGxp(), entity.getGxp());
		assertEquals(location.getIcp(), entity.getIcp());
		assertEquals(location.getId(), entity.getId());
		assertEquals(location.getLatitude(), entity.getLatitude());
		assertEquals(location.getLongitude(), entity.getLongitude());
		assertEquals(location.getName(), entity.getName());
		assertEquals(location.getPostalCode(), entity.getPostalCode());
		assertEquals(location.getRegion(), entity.getRegion());
		assertEquals(location.getStateOrProvince(), entity.getStateOrProvince());
		assertEquals(location.getStreet(), entity.getStreet());
		assertEquals(location.getTimeZoneId(), entity.getTimeZoneId());
	}
	
	@Test
	public void insertLocation() {
		Location location = new Location();
		location.setName("My Test Location");
		location.setCountry("NZ");
		location.setTimeZoneId("Pacific/Auckland");
		location.setRegion("Lower North Island");
		location.setLocality("Wellington");
		location.setPostalCode("6000");
		location.setGxp("GXP");
		location.setIcp("ICP");
		location.setLatitude(1.234);
		location.setLongitude(2.345);
		
		logger.debug("Inserting new Location: " +location);
		
		Long id = locationDao.store(location);
		assertNotNull(id);
		
		Location entity = locationDao.get(id);
		validateLocation(location, entity);
		
		lastLocationId = id;
	}

	@Test
	public void updateLocation() {
		insertLocation();
		
		Location location = locationDao.get(lastLocationId);
		assertEquals("My Test Location", location.getName());
		location.setName("foo.update");
		
		Long id = locationDao.store(location);
		assertEquals(lastLocationId, id);
		
		Location entity = locationDao.get(id);
		validateLocation(location, entity);
	}

	@Test
	public void findFilteredBoxEmptySet() {
		insertLocation();
		
		// now search for box coordinates
		SimpleLocationFilter filter = new SimpleLocationFilter();
		filter.setLatitude(6.3);
		filter.setLongitude(6.5);
		filter.setBoxLatitude(8.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = locationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredBox() {
		insertLocation();

		// create a second location with lat/long
		setupTestLocation();
		Location l = locationDao.get(TEST_LOCATION_ID);
		l.setLatitude(10.1234);
		l.setLongitude(20.3456);
		locationDao.store(l);
		
		// now search for box coordinates
		SimpleLocationFilter filter = new SimpleLocationFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = locationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastLocationId, results.getResults().iterator().next().getId());
	}

	@Test
	public void findFilteredIds() {
		insertLocation();

		// create a second location
		setupTestLocation();
		
		// now search for ID set
		SimpleLocationFilter filter = new SimpleLocationFilter();
		List<Long> ids = new ArrayList<Long>(2);
		ids.add(lastLocationId);
		ids.add(TEST_LOCATION_ID);
		filter.setIds(ids);

		FilterResults<Match> results = locationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(2, results.getReturnedResultCount().intValue());
		Iterator<Match> iterator = results.getResults().iterator();
		assertEquals(TEST_LOCATION_ID, iterator.next().getId());
		assertEquals(lastLocationId, iterator.next().getId());
		
		// search again, for just one
		ids.remove(0);
		results = locationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(TEST_LOCATION_ID, results.getResults().iterator().next().getId());
	}

}
