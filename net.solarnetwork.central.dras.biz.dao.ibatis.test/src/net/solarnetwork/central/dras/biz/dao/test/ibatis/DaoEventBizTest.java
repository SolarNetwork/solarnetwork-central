/* ==================================================================
 * DaoEventBizTest.java - Jun 16, 2011 8:56:00 AM
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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.solarnetwork.central.dras.biz.dao.DaoEventBiz;
import net.solarnetwork.central.dras.dao.ParticipantGroupDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Test case for {@link DaoEventBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class DaoEventBizTest extends AbstractTestSupport {

	@Autowired private ParticipantGroupDao participantGroupDao;
	@Autowired private ProgramDao programDao;
	@Autowired private DaoEventBiz eventBiz;

	@Before
	public void setupSecurity() {
		SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USERNAME, "unittest"));
	}
	
	@Test
	public void assignMembers() {
		setupTestLocation();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		setupTestParticipant();
		setupTestParticipantGroup();
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);
		
		// add participant to program
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_ID);
		programDao.assignParticipantMembers(TEST_PROGRAM_ID, memberIds, TEST_EFFECTIVE_ID);

		MembershipCommand participants = new MembershipCommand();
		participants.getGroup().addAll(memberIds);
		
		// add participant to participant group
		participantGroupDao.assignParticipantMembers(
				TEST_PARTICIPANT_GROUP_ID, memberIds, TEST_EFFECTIVE_ID);
		
		memberIds.clear();
		memberIds.add(TEST_PARTICIPANT_GROUP_ID);
		MembershipCommand groups = new MembershipCommand();
		groups.getGroup().addAll(memberIds);
		
		// assign participant and group
		EffectiveCollection<Event, Member> result = eventBiz.assignMembers(TEST_EVENT_ID,
				participants, groups);
		assertNotNull(result);
		assertNotNull(result.getEffective());
		assertNotNull(result.getObject());
		assertEquals(TEST_EVENT_ID, result.getObject().getId());
		assertNotNull(result.getCollection());
		assertEquals(2, result.getCollection().size());
		
		// we have Participant and ParticipantGroup objects
		Map<String, Collection<Long>> mapping = result.getMemberMap();
		assertEquals(2, mapping.size());
		Collection<Long> foundParticipants = mapping.get(Participant.class.getSimpleName());
		assertNotNull(foundParticipants);
		assertEquals(1, foundParticipants.size());
		Collection<Long> foundParticipantGroups = mapping.get(ParticipantGroup.class.getSimpleName());
		assertNotNull(foundParticipantGroups);
		assertEquals(1, foundParticipantGroups.size());
		assertTrue(foundParticipants.contains(TEST_PARTICIPANT_ID));
		assertTrue(foundParticipantGroups.contains(TEST_PARTICIPANT_GROUP_ID));
	}

}
