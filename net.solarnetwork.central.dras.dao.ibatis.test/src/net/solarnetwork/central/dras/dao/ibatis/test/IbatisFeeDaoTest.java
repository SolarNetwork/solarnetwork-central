/* ==================================================================
 * IbatisFeeDaoTest.java - Jun 23, 2011 2:18:25 PM
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
import net.solarnetwork.central.dras.dao.FeeDao;
import net.solarnetwork.central.dras.domain.Fee;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link FeeDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisFeeDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private FeeDao feeDao;
	
	private Fee lastFee;
	
	@Before
	public void setup() {
		lastFee = null;
	}
	
	private void validateFee(Fee expected, Fee found) {
		assertNotNull("Fee should exist", found);
		assertEquals(expected.getId(), found.getId());
		
		assertEquals(expected.getAvailableFee(), found.getAvailableFee());
		assertEquals(expected.getAvailablePeriod(), found.getAvailablePeriod());
		assertEquals(expected.getCancelFee(), found.getCancelFee());
		assertEquals(expected.getCurrency(), found.getCurrency());
		assertEquals(expected.getDeliveryFee(), found.getDeliveryFee());
		assertEquals(expected.getEstablishFee(), found.getEstablishFee());
		assertEquals(expected.getEventFee(), found.getEventFee());
	}
	
	@Test
	public void insertFee() {
		Fee f = createFee();
		
		Long id = feeDao.store(f);
		assertNotNull(id);
		
		f.setId(id);
		lastFee = f;
	}

	@Test
	public void getFeeById() {
		insertFee();
		Fee entity = feeDao.get(lastFee.getId());
		validateFee(lastFee, entity);
	}
	
	@Test
	public void getNonExistingFeeById() {
		Fee fee = feeDao.get(-99999L);
		assertNull(fee);
	}
	
	@Test
	public void updateFee() {
		insertFee();
		
		assertEquals(Long.valueOf(1L), lastFee.getAvailableFee());
		lastFee.setAvailableFee(11L);
		
		feeDao.store(lastFee);
		
		Fee entity = feeDao.get(lastFee.getId());
		validateFee(lastFee, entity);
	}

}
