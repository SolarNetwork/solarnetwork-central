/* ==================================================================
 * MyBatisSolarLocationDaoTests.java - Nov 10, 2014 9:05:24 AM
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

package net.solarnetwork.central.common.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisSolarLocationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisSolarLocationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisSolarLocationDao solarLocationDao;

	private SolarLocation location = null;

	@Before
	public void setUp() throws Exception {
		solarLocationDao = new MyBatisSolarLocationDao();
		solarLocationDao.setSqlSessionFactory(getSqlSessionFactory());
	}

	@Test
	public void storeNew() {
		SolarLocation loc = new SolarLocation();
		loc.setCountry("NZ");
		loc.setCreated(new DateTime());
		loc.setLatitude(new BigDecimal("1.2"));
		loc.setLocality("locality");
		loc.setLongitude(new BigDecimal("1.1"));
		loc.setElevation(new BigDecimal("1053"));
		loc.setName("test location");
		loc.setPostalCode("6011");
		loc.setRegion("region");
		loc.setStateOrProvince("state");
		loc.setStreet("street");
		loc.setTimeZoneId("UTC");
		Long id = solarLocationDao.store(loc);
		assertNotNull(id);
		loc.setId(id);
		location = loc;
	}

	private void validatePublic(SolarLocation src, SolarLocation entity) {
		assertNotNull("SolarLocation should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getCountry(), entity.getCountry());
		assertEquals(src.getLocality(), entity.getLocality());
		assertEquals(src.getPostalCode(), entity.getPostalCode());
		assertEquals(src.getRegion(), entity.getRegion());
		assertEquals(src.getStateOrProvince(), entity.getStateOrProvince());
		assertEquals(src.getTimeZoneId(), entity.getTimeZoneId());
	}

	private void validate(SolarLocation src, SolarLocation entity) {
		validatePublic(src, entity);
		if ( src.getLatitude() != null ) {
			assertNotNull(entity.getLatitude());
			assertEquals(0, src.getLatitude().compareTo(entity.getLatitude()));
		}
		if ( src.getLongitude() != null ) {
			assertNotNull(entity.getLongitude());
			assertEquals(0, src.getLongitude().compareTo(entity.getLongitude()));
		}
		if ( src.getElevation() != null ) {
			assertNotNull(entity.getElevation());
			assertEquals(0, src.getElevation().compareTo(entity.getElevation()));
		}
		assertEquals(src.getStreet(), entity.getStreet());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		SolarLocation loc = solarLocationDao.get(location.getId());
		validate(location, loc);
	}

	@Test
	public void update() {
		storeNew();
		SolarLocation loc = solarLocationDao.get(location.getId());
		loc.setName("new name");
		Long newId = solarLocationDao.store(loc);
		assertEquals(loc.getId(), newId);
		SolarLocation loc2 = solarLocationDao.get(location.getId());
		validate(loc, loc2);
	}

	@Test
	public void findByTimeZoneNoResults() {
		storeNew();
		// should not find this location, because properties other than country and time zone are set
		SolarLocation loc = solarLocationDao.getSolarLocationForTimeZone(location.getCountry(),
				location.getTimeZoneId());
		assertNull(loc);
	}

	@Test
	public void findByTimeZone() {
		SolarLocation loc = new SolarLocation();
		loc.setCreated(new DateTime());
		loc.setName("NZ - Pacific/Auckland");
		loc.setCountry("NZ");
		loc.setTimeZoneId("Pacific/Auckland");
		Long id = solarLocationDao.store(loc);
		assertNotNull(id);
		loc.setId(id);
		// should not find this location, because properties other than country and time zone are set
		SolarLocation found = solarLocationDao.getSolarLocationForTimeZone(loc.getCountry(),
				loc.getTimeZoneId());
		assertNotNull(loc);
		validate(loc, found);
	}

	@Test
	public void findFilteredNoMatch() {
		SolarLocation filter = new SolarLocation();
		filter.setName("does-not-exist");
		FilterResults<LocationMatch> results = solarLocationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(0), results.getReturnedResultCount());
		assertEquals(Long.valueOf(0L), results.getTotalResults());
	}

	@Test
	public void findFilteredMatch() {
		setupTestLocation();
		SolarLocation filter = new SolarLocation();
		filter.setRegion(TEST_LOC_REGION);
		FilterResults<LocationMatch> results = solarLocationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals(Long.valueOf(1L), results.getTotalResults());
		assertNotNull(results.getResults());
		LocationMatch match = results.getResults().iterator().next();
		assertNotNull(match);
		assertEquals(TEST_LOC_ID, match.getId());
	}

	@Test
	public void findFilteredMultipleOrder() {
		setupTestLocation();
		SolarLocation loc2 = new SolarLocation();
		loc2.setCreated(new DateTime());
		loc2.setRegion(TEST_LOC_REGION);
		loc2.setCountry(TEST_LOC_COUNTRY);
		loc2.setPostalCode(TEST_LOC_POSTAL_CODE);
		loc2.setTimeZoneId(TEST_TZ);
		Long id2 = solarLocationDao.store(loc2);

		SolarLocation loc3 = new SolarLocation();
		loc3.setCreated(new DateTime());
		loc3.setRegion(TEST_LOC_REGION);
		loc3.setCountry(TEST_LOC_COUNTRY);
		loc3.setPostalCode(TEST_LOC_POSTAL_CODE);
		loc3.setTimeZoneId(TEST_TZ);
		Long id3 = solarLocationDao.store(loc3);

		SolarLocation filter = new SolarLocation();
		filter.setRegion(TEST_LOC_REGION);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);
		filter.setTimeZoneId(TEST_TZ);
		FilterResults<LocationMatch> results = solarLocationDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(3), results.getReturnedResultCount());
		assertEquals(Long.valueOf(3L), results.getTotalResults());
		assertNotNull(results.getResults());

		List<Long> expectedIds = Arrays.asList(TEST_LOC_ID, id2, id3);
		int idx = 0;
		for ( LocationMatch match : results.getResults() ) {
			assertEquals("Results should be ordered by name", expectedIds.get(idx), match.getId());
			idx++;
		}
	}

	@Test
	public void findExactLocationNoMatch() {
		setupTestLocation();
		SolarLocation criteria = new SolarLocation();
		criteria.setPostalCode(TEST_LOC_POSTAL_CODE);
		SolarLocation match = solarLocationDao.getSolarLocationForLocation(criteria);
		assertNull(match);
	}

	@Test
	public void findExactLocationMatch() {
		setupTestLocation();
		SolarLocation criteria = solarLocationDao.get(TEST_LOC_ID);
		SolarLocation match = solarLocationDao.getSolarLocationForLocation(criteria);
		assertNotNull(match);
		assertEquals(criteria, match);
	}

}
