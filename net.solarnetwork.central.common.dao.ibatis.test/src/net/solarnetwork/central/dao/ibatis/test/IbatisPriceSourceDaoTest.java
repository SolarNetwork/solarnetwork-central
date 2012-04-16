/* ==================================================================
 * IbatisPriceSourceDaoTest.java - Oct 19, 2011 9:19:30 PM
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
import net.solarnetwork.central.dao.PriceSourceDao;
import net.solarnetwork.central.dao.ibatis.IbatisPriceSourceDao;
import net.solarnetwork.central.domain.PriceSource;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisPriceSourceDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisPriceSourceDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private PriceSourceDao dao;
	
	private PriceSource priceSource = null;

	@Test
	public void storeNew() {
		PriceSource d = new PriceSource();
		d.setCreated(new DateTime());
		d.setName("Test name");
		Long id = dao.store(d);
		assertNotNull(id);
		d.setId(id);
		priceSource = d;
	}

	private void validate(PriceSource src, PriceSource entity) {
		assertNotNull("PriceSource should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getId(), entity.getId());
		assertEquals(src.getName(), entity.getName());
	}

    @Test
	public void getByPrimaryKey() {
    	storeNew();
    	PriceSource d = dao.get(priceSource.getId());
    	validate(priceSource, d);
	}
    
	@Test
	public void update() {
		storeNew();
		PriceSource d = dao.get(priceSource.getId());
		d.setName("new name");
		Long newId = dao.store(d);
		assertEquals(d.getId(), newId);
		PriceSource d2 = dao.get(priceSource.getId());
		validate(d, d2);
	}
	
	@Test
	public void findByName() {
		storeNew();
		PriceSource s = dao.getPriceSourceForName("Test name");
		assertNotNull(s);
		validate(priceSource, s);
	}
	
}
