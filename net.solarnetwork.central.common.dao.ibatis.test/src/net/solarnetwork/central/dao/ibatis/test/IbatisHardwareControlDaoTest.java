/* ==================================================================
 * IbatisHardwareDaoTest.java - Sep 29, 2011 12:57:02 PM
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

package net.solarnetwork.central.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dao.HardwareControlDao;
import net.solarnetwork.central.dao.HardwareDao;
import net.solarnetwork.central.dao.ibatis.IbatisHardwareDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Hardware;
import net.solarnetwork.central.domain.HardwareControl;
import net.solarnetwork.central.support.SimpleHardwareFilter;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisHardwareDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisHardwareControlDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private HardwareControlDao hardwareControlDao;
	@Autowired private HardwareDao hardwareDao;
	
	private Hardware hardware = null;
	private HardwareControl hardwareControl = null;

	@Before
	public void setUp() throws Exception {
		setupTestHardware();
		this.hardware = hardwareDao.get(TEST_HARDWARE_ID);
		assertNotNull(this.hardware);
	}

	@Test
	public void storeNew() {
		HardwareControl hwc = new HardwareControl();
		hwc.setCreated(new DateTime());
		hwc.setHardware(hardware);
		hwc.setName("Test Control");
		hwc.setUnit("W");
		Long id = hardwareControlDao.store(hwc);
		assertNotNull(id);
		hwc.setId(id);
		hardwareControl = hwc;
	}

	private void validate(HardwareControl src, HardwareControl entity) {
		assertNotNull("Hardware should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getHardware(), entity.getHardware());
		assertEquals(src.getName(), entity.getName());
		assertEquals(src.getUnit(), entity.getUnit());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	HardwareControl hwc = hardwareControlDao.get(hardwareControl.getId());
    	validate(hardwareControl, hwc);
	}
    
	@Test
	public void update() {
		storeNew();
		HardwareControl hwc = hardwareControlDao.get(hardwareControl.getId());
		hwc.setUnit("Z");
		Long newId = hardwareControlDao.store(hwc);
		assertEquals(hwc.getId(), newId);
		HardwareControl hwc2 = hardwareControlDao.get(hardwareControl.getId());
		validate(hwc, hwc2);
	}
	
	@Test
	public void findByName() {
		storeNew();
		SimpleHardwareFilter filter = new SimpleHardwareFilter();
		filter.setName(TEST_HARDWARE_MANUFACTURER);
		FilterResults<EntityMatch> matches = hardwareControlDao.findFiltered(
				filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Integer.valueOf(1), matches.getReturnedResultCount());
		EntityMatch match = matches.getResults().iterator().next();
		assertNotNull(match);
		assertEquals(hardwareControl.getId(), match.getId());
	}

}
