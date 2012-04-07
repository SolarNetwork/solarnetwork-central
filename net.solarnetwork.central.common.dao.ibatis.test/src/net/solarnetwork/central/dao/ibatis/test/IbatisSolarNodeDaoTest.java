/* ==================================================================
 * IbatisSolarNodeDaoTest.java - Feb 2, 2010 2:20:41 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.dao.ibatis.IbatisSolarNodeDao;

/**
 * Test case for the {@link IbatisSolarNodeDao} class.
 * 
 * @author matt
 * @version $Id$
 */
public class IbatisSolarNodeDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private SolarNodeDao solarNodeDao;
	
	@Before
	public void setupInTransaction() {		
		setupTestLocation();
	}
	
	@Test
	public void getSolarNodeById() throws Exception {
		simpleJdbcTemplate.update(
				"insert into solarnet.sn_node (node_id, loc_id) values (?,?)", 
				TEST_NODE_ID, TEST_LOC_ID);

		SolarNode node = solarNodeDao.get(TEST_NODE_ID);
		assertNotNull(node);
		assertNotNull(node.getId());
		assertEquals(TEST_NODE_ID, node.getId());
		assertEquals(TEST_LOC_ID, node.getLocationId());
		assertNotNull(node.getTimeZone());
		assertEquals(TEST_TZ, node.getTimeZone().getID());
	}
	
	@Test
	public void getNonExistingSolarNodeById() throws Exception {
		SolarNode node = solarNodeDao.get(-99L);
		assertNull(node);
	}
	
	@Test
	public void insertSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);
		
		Long id = solarNodeDao.store(node);
		assertNotNull(id);
	}
	
	@Test
	public void updateSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);
		
		Long id = solarNodeDao.store(node);
		assertNotNull(id);
		node = solarNodeDao.get(id);
		assertEquals(id, node.getId());
		node.setName("myname");
		Long id2 = solarNodeDao.store(node);
		assertEquals(id, id2);
		node = solarNodeDao.get(id);
		assertEquals("myname", node.getName());
	}

}
