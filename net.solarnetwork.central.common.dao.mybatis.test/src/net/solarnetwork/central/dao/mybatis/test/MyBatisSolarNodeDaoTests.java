/* ==================================================================
 * MyBatisSolarNodeDao.java - Nov 10, 2014 1:57:58 PM
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

package net.solarnetwork.central.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisSolarNodeDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisSolarNodeDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisSolarNodeDao dao;

	@Before
	public void setup() {
		setupTestLocation();
		dao = new MyBatisSolarNodeDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	@Test
	public void getSolarNodeById() throws Exception {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", TEST_NODE_ID,
				TEST_LOC_ID);

		SolarNode node = dao.get(TEST_NODE_ID);
		assertNotNull(node);
		assertNotNull(node.getId());
		assertEquals(TEST_NODE_ID, node.getId());
		assertEquals(TEST_LOC_ID, node.getLocationId());
		assertNotNull(node.getTimeZone());
		assertEquals(TEST_TZ, node.getTimeZone().getID());
	}

	@Test
	public void getNonExistingSolarNodeById() throws Exception {
		SolarNode node = dao.get(-99L);
		assertNull(node);
	}

	@Test
	public void insertSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);

		Long id = dao.store(node);
		assertNotNull(id);
	}

	@Test
	public void updateSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);

		Long id = dao.store(node);
		assertNotNull(id);
		node = dao.get(id);
		assertEquals(id, node.getId());
		node.setName("myname");
		Long id2 = dao.store(node);
		assertEquals(id, id2);
		node = dao.get(id);
		assertEquals("myname", node.getName());
	}

}
