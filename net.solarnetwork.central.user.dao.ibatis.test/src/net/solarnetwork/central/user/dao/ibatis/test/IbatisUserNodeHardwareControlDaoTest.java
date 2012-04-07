/* ==================================================================
 * IbatisUserNodeHardwareControlDaoTest.java - Oct 2, 2011 8:13:17 PM
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

package net.solarnetwork.central.user.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dao.HardwareControlDao;
import net.solarnetwork.central.domain.HardwareControl;
import net.solarnetwork.central.user.dao.ibatis.IbatisUserNodeHardwareControlDao;
import net.solarnetwork.central.user.domain.UserNodeHardwareControl;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisUserNodeHardwareControlDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisUserNodeHardwareControlDaoTest
extends AbstractIbatisUserDaoTestSupport {

	/** The tables to delete from at the start of the tests (within a transaction). */
	private static final String[] DELETE_TABLES = new String[] {
		"solaruser.user_node_hardware_control", 
		"solarnet.sn_hardware_control", 
		"solarnet.sn_hardware",
		};

	@Autowired private IbatisUserNodeHardwareControlDao dao;
	
	@Autowired private HardwareControlDao hardwareControlDao;
	
	private HardwareControl control;
	private UserNodeHardwareControl lastObject;
	
	@Before
	public void setUp() throws Exception {
		deleteFromTables(DELETE_TABLES);
		setupTestNode();
		setupTestHardware();
		setupTestHardwareControl();
		control = hardwareControlDao.get(TEST_HARDWARE_CONTROL_ID);
	}
	
	@Test
	public void storeNew() {
		UserNodeHardwareControl obj = new UserNodeHardwareControl();
		obj.setNodeId(TEST_NODE_ID);
		obj.setCreated(new DateTime());
		obj.setName(TEST_NAME);
		obj.setSourceId("/test/source");
		obj.setControl(this.control);
		Long id = dao.store(obj);
		assertNotNull(id);
		this.lastObject = dao.get(id);
	}

	private void validate(UserNodeHardwareControl src, UserNodeHardwareControl entity) {
		assertNotNull("UserNodeHardwareControl should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getControl(), entity.getControl());
		assertEquals(src.getId(), entity.getId());
		assertEquals(src.getName(), entity.getName());
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getSourceId(), entity.getSourceId());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	UserNodeHardwareControl obj = dao.get(lastObject.getId());
		validate(lastObject, obj);
	}
    
	@Test
	public void update() {
		storeNew();
    	UserNodeHardwareControl obj = dao.get(lastObject.getId());
    	obj.setName("Updated name");
		Long id = dao.store(obj);
		assertNotNull(id);
		assertEquals(lastObject.getId(), id);
		UserNodeHardwareControl updated = dao.get(id);
		validate(obj, updated);
	}
}
