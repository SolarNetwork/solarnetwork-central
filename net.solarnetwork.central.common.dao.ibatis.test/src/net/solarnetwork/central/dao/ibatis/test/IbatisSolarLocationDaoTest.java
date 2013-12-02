/* ==================================================================
 * IbatisSolarLocationDaoTest.java - Sep 9, 2011 1:56:12 PM
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

package net.solarnetwork.central.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.ibatis.IbatisSolarLocationDao;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisSolarLocationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisSolarLocationDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired
	private SolarLocationDao solarLocationDao;
	@Autowired
	private SolarNodeDao solarNodeDao;

	private SolarNode node = null;
	private SolarLocation location = null;

	@Before
	public void setUp() throws Exception {
		setupTestNode();
		this.node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(this.node);
	}

	@Test
	public void storeNew() {
		SolarLocation loc = new SolarLocation();
		loc.setCountry("NZ");
		loc.setCreated(new DateTime());
		loc.setLatitude(1.2);
		loc.setLocality("locality");
		loc.setLongitude(1.1);
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
		assertEquals(src.getName(), entity.getName());
		assertEquals(src.getPostalCode(), entity.getPostalCode());
		assertEquals(src.getRegion(), entity.getRegion());
		assertEquals(src.getStateOrProvince(), entity.getStateOrProvince());
		assertEquals(src.getTimeZoneId(), entity.getTimeZoneId());
	}

	private void validate(SolarLocation src, SolarLocation entity) {
		validatePublic(src, entity);
		assertEquals(src.getLatitude(), entity.getLatitude());
		assertEquals(src.getLongitude(), entity.getLongitude());
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
	public void findByName() {
		storeNew();
		SolarLocation loc = solarLocationDao.getSolarLocationForName(location.getName());
		assertNotNull(loc);
		validatePublic(location, loc);
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

}
