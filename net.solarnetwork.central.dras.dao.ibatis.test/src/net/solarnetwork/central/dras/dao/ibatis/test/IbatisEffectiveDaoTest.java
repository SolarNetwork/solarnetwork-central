/* ==================================================================
 * IbatisEffectiveDaoTest.java - Jun 6, 2011 11:48:16 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This effective is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This effective is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this effective; if not, write to the Free Software 
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
import static org.junit.Assert.fail;
import net.solarnetwork.central.dras.dao.EffectiveDao;
import net.solarnetwork.central.dras.domain.Effective;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link EffectiveDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEffectiveDaoTest extends AbstractIbatisDaoTestSupport {
	
	/** EffectiveDao to test. */
	@Autowired
	protected EffectiveDao effectiveDao;
	
	private Long lastEffectiveId;
	
	@Before
	public void setup() {
		lastEffectiveId = null;
	}
	
	@Test
	public void getEffectiveById() {
		Effective effective = effectiveDao.get(TEST_EFFECTIVE_ID);
		assertNotNull(effective);
		assertNotNull(effective.getId());
		assertEquals(TEST_EFFECTIVE_ID, effective.getId());
	}
	
	@Test
	public void getNonExistingEffectiveById() {
		Effective effective = effectiveDao.get(-99999L);
		assertNull(effective);
	}
	
	private void validateEffective(Effective effective, Effective entity) {
		assertNotNull("Effective should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(effective.getCreator(), entity.getCreator());
		assertEquals(effective.getEffectiveDate(), entity.getEffectiveDate());
	}
	
	@Test
	public void insertEffective() {
		Effective effective = new Effective();
		effective.setCreator(TEST_USER_ID);
		effective.setEffectiveDate(new DateTime());
		
		logger.debug("Inserting new Effective: " +effective);
		
		Long id = effectiveDao.store(effective);
		assertNotNull(id);
		
		Effective entity = effectiveDao.get(id);
		validateEffective(effective, entity);
		
		lastEffectiveId = id;
	}

	@Test
	public void updateEffectiveNotAllowed() {
		insertEffective();
		
		Effective effective = effectiveDao.get(lastEffectiveId);
		effective.setCreator(23L);
		
		try {
			effectiveDao.store(effective);
			fail("Should have thrown exception if updating Effective");
		} catch ( UnsupportedOperationException e ) {
			// this is expected
		}
	}


}
