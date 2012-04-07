/* ==================================================================
 * DaoParticipantBizTest.java - Jun 14, 2011 4:48:56 PM
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

package net.solarnetwork.central.dras.biz.dao.test.ibatis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dras.biz.dao.DaoParticipantBiz;
import net.solarnetwork.central.dras.domain.Capability;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test case for {@link DaoParticipantBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class DaoParticipantBizTest extends AbstractTestSupport {

	@Autowired private DaoParticipantBiz participantBiz;

	@Before
	public void setupSecurity() {
		SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "unittest"));
	}
	
	@Test
	public void storeParticipantCapability() {
		setupTestLocation();
		setupTestParticipant();
		
		Capability input = new Capability();
		input.setShedCapacityWatts(1000L);
		input.setShedCapacityWattHours(2000L);
		
		Capability result = participantBiz.storeParticipantCapability(
				TEST_PARTICIPANT_ID, input);
		assertNotNull(result);
		assertNotNull(result.getId());
		
		// try to store same details again, should not make new Capability
		Capability result2 = participantBiz.storeParticipantCapability(
				TEST_PARTICIPANT_ID, input);
		assertNotNull(result);
		assertEquals(result, result2);
	}
}
