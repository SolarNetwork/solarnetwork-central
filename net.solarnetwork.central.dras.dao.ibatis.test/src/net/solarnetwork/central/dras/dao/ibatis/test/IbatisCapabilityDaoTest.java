/* ==================================================================
 * IbatisCapabilityDaoTest.java - Jun 6, 2011 1:51:37 PM
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

package net.solarnetwork.central.dras.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.dras.dao.CapabilityDao;
import net.solarnetwork.central.dras.domain.Capability;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link CapabilityDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisCapabilityDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private CapabilityDao capabilityDao;
	
	private Long lastCapabilityId;
	
	@Before
	public void setup() {
		lastCapabilityId = null;
		setupTestLocation();
	}
	
	@Test
	public void getCapabilityById() {
		setupTestCapability();
		Capability capability = capabilityDao.get(TEST_CAPABILITY_ID);
		assertNotNull(capability);
		assertNotNull(capability.getId());
		assertEquals(TEST_CAPABILITY_ID, capability.getId());
		assertEquals("Test", capability.getDemandResponseKind());
		assertEquals(Long.valueOf(11L), capability.getShedCapacityWatts());
		assertEquals(Long.valueOf(12L), capability.getShedCapacityWattHours());
		assertEquals(Long.valueOf(13L), capability.getVarCapacityVoltAmps());
	}
	
	@Test
	public void getNonExistingCapabilityById() {
		Capability capability = capabilityDao.get(-99999L);
		assertNull(capability);
	}
	
	private void validateCapability(Capability capability, Capability entity) {
		assertNotNull("Capability should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(capability.getDemandResponseKind(), entity.getDemandResponseKind());
		assertEquals(capability.getId(), entity.getId());
		assertEquals(capability.getShedCapacityWattHours(), entity.getShedCapacityWattHours());
		assertEquals(capability.getShedCapacityWatts(), entity.getShedCapacityWatts());
		assertEquals(capability.getVarCapacityVoltAmps(), entity.getVarCapacityVoltAmps());
	}
	
	@Test
	public void insertCapability() {
		Capability capability = new Capability();
		capability.setDemandResponseKind("Test");
		capability.setShedCapacityWattHours(1L);
		capability.setShedCapacityWatts(2L);
		capability.setVarCapacityVoltAmps(3L);
		
		logger.debug("Inserting new Capability: " +capability);
		
		Long id = capabilityDao.store(capability);
		assertNotNull(id);
		
		Capability entity = capabilityDao.get(id);
		validateCapability(capability, entity);
		
		lastCapabilityId = id;
	}

	@Test
	public void updateCapability() {
		insertCapability();
		
		Capability capability = capabilityDao.get(lastCapabilityId);
		assertEquals("Test", capability.getDemandResponseKind());
		capability.setDemandResponseKind("Foo");
		
		Long id = capabilityDao.store(capability);
		assertEquals(lastCapabilityId, id);
		
		Capability entity = capabilityDao.get(id);
		validateCapability(capability, entity);
	}

}
