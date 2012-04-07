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
import net.solarnetwork.central.dao.HardwareDao;
import net.solarnetwork.central.dao.ibatis.IbatisHardwareDao;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.Hardware;
import net.solarnetwork.central.support.SimpleHardwareFilter;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisHardwareDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisHardwareDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private HardwareDao hardwareDao;
	
	private Hardware hardware = null;

	@Test
	public void storeNew() {
		Hardware hw = new Hardware();
		hw.setCreated(new DateTime());
		hw.setManufacturer("test manufacturer");
		hw.setModel("test model");
		hw.setRevision(0);
		Long id = hardwareDao.store(hw);
		assertNotNull(id);
		hw.setId(id);
		hardware = hw;
	}

	private void validate(Hardware src, Hardware entity) {
		assertNotNull("Hardware should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getManufacturer(), entity.getManufacturer());
		assertEquals(src.getModel(), entity.getModel());
		assertEquals(src.getRevision(), entity.getRevision());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	Hardware hw = hardwareDao.get(hardware.getId());
    	validate(hardware, hw);
	}
    
	@Test
	public void update() {
		storeNew();
		Hardware hw = hardwareDao.get(hardware.getId());
		hw.setModel("new model");
		Long newId = hardwareDao.store(hw);
		assertEquals(hw.getId(), newId);
		Hardware hw2 = hardwareDao.get(hardware.getId());
		validate(hw, hw2);
	}
	
	@Test
	public void findByName() {
		storeNew();
		SimpleHardwareFilter filter = new SimpleHardwareFilter();
		filter.setName("test manufacturer");
		FilterResults<EntityMatch> matches = hardwareDao.findFiltered(filter, null, null, null);
		assertNotNull(matches);
		assertEquals(Integer.valueOf(1), matches.getReturnedResultCount());
		assertNotNull(matches.getResults());
		EntityMatch m = matches.getResults().iterator().next();
		assertEquals(hardware.getId(), m.getId());
	}

}
