/* ==================================================================
 * IbatisEventDaoTest.java - Jun 7, 2011 6:40:46 PM
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
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventExecutionTargets;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.domain.UserGroup;
import net.solarnetwork.central.dras.support.SimpleEventFilter;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link EventDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventDaoTest extends AbstractIbatisDaoTestSupport {

	/** EventDao to test. */
	@Autowired
	protected EventDao eventDao;
	
	private Long lastEventId;
	
	@Before
	public void setup() {
		lastEventId = null;
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
	}
	
	/**
	 * Test get Event.
	 */
	@Test
	public void getEventById() {
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);
		DateTime eventDate = new DateTime(TEST_PROGRAM_DATE);
		DateTime notifDate = eventDate.minusHours(2);
		DateTime endDate = eventDate.plusHours(1);
		Event event = eventDao.get(TEST_EVENT_ID);
		assertNotNull(event);
		assertEquals(TEST_EVENT_ID, event.getId());
		assertEquals(TEST_PROGRAM_ID, event.getProgramId());
		assertEquals(eventDate, event.getEventDate());
		assertEquals(notifDate, event.getNotificationDate());
		assertEquals(endDate, event.getEndDate());
		assertEquals(Boolean.TRUE, event.getTest());
		assertEquals(Boolean.FALSE, event.getEnabled());
	}
	
	/**
	 * Test get Event that doesn't exist.
	 */
	@Test
	public void getNonExistingEventById() {
		Event event = eventDao.get(-99999L);
		assertNull(event);
	}
	
	private void validateEvent(Event event, Event entity) {
		assertNotNull("Event should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(event.getCreator(), entity.getCreator());
		assertEquals(event.getEnabled(), entity.getEnabled());
		assertEquals(event.getEndDate(), entity.getEndDate());
		assertEquals(event.getEventDate(), entity.getEventDate());
		assertEquals(event.getId(), entity.getId());
		assertEquals(event.getInitiator(), entity.getInitiator());
		assertEquals(event.getName(), entity.getName());
		assertEquals(event.getNotificationDate(), entity.getNotificationDate());
		assertEquals(event.getProgramId(), entity.getProgramId());
		assertEquals(event.getTest(), entity.getTest());
		assertEquals(event.getVersion(), entity.getVersion());
	}
	
	/**
	 * Test store new Event.
	 */
	@Test
	public void insertEvent() {
		DateTime eventDate = new DateTime(TEST_PROGRAM_DATE);

		Event event = new Event();
		event.setCreator(TEST_USER_ID);
		event.setEnabled(Boolean.TRUE);
		event.setEndDate(eventDate.plusHours(1));
		event.setEventDate(eventDate);
		event.setName("Unit test event");
		event.setInitiator("unittest");
		event.setNotificationDate(eventDate.minusDays(2));
		event.setProgramId(TEST_PROGRAM_ID);
		event.setTest(Boolean.TRUE);
		
		logger.debug("Inserting new Event: " +event);
		
		Long id = eventDao.store(event);
		assertNotNull(id);
		
		Event entity = eventDao.get(id);
		validateEvent(event, entity);
		
		lastEventId = id;
	}

	/**
	 * Test store updated User.
	 */
	@Test
	public void updateEvent() {
		insertEvent();
		
		Event event = eventDao.get(lastEventId);
		assertEquals("unittest", event.getInitiator());
		event.setInitiator("foo.update");
		
		Long id = eventDao.store(event);
		assertEquals(lastEventId, id);
		
		Event entity = eventDao.get(id);
		validateEvent(event, entity);
	}

	/**
	 * Test a participant with no eventTargets is OK.
	 */
	@Test
	public void emptyEventTargets() {
		insertEvent();
		Set<EventTargets> members = eventDao.getEventTargets(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getEventTargets() {
		insertEvent();
		setupTestEventRule();
		setupTestEventTargets(TEST_EVENT_RULE_ID);
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_target (evt_id,eta_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_EVENT_TARGETS_ID, TEST_EFFECTIVE_ID);

		Set<EventTargets> members = eventDao.getEventTargets(lastEventId, new DateTime());
		assertNotNull(members);
		assertEquals(1, members.size());
		EventTargets eventTargets = members.iterator().next();
		assertNotNull(eventTargets);
		assertEquals(TEST_EVENT_TARGETS_ID, eventTargets.getId());
		
		assertNotNull(eventTargets.getTargets());
		assertEquals(2, eventTargets.getTargets().size());
		EventTarget[] targets = eventTargets.getTargets().toArray(new EventTarget[2]);
		assertEquals(10000, (int)targets[0].getValue().doubleValue());
		assertEquals(8000, (int)targets[1].getValue().doubleValue());
		
		// validate date before effective date returns null
		members = eventDao.getEventTargets(lastEventId, new DateTime().minusYears(1));
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getEventExecutionTargets() {
		insertEvent();
		setupTestEventRule();
		setupTestEventTargets(TEST_EVENT_RULE_ID);
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_target (evt_id,eta_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_EVENT_TARGETS_ID, TEST_EFFECTIVE_ID);

		Set<EventExecutionTargets> members = eventDao.getEventExecutionTargets(lastEventId, new DateTime());
		assertNotNull(members);
		assertEquals(1, members.size());
		EventExecutionTargets eventTargets = members.iterator().next();

		assertEquals(TEST_EVENT_RULE_NAME, eventTargets.getEventRuleName());

		assertNotNull(eventTargets.getTargets());
		assertEquals(2, eventTargets.getTargets().size());
		EventTarget[] targets = eventTargets.getTargets().toArray(new EventTarget[2]);
		assertEquals(10000, (int)targets[0].getValue().doubleValue());
		assertEquals(8000, (int)targets[1].getValue().doubleValue());
	}
	
	@Test
	public void setEventTargets() {
		insertEvent();
		setupTestEventRule();
		setupTestEventTargets(TEST_EVENT_RULE_ID);
		Set<Long> memberIds = new LinkedHashSet<Long>();
		memberIds.add(TEST_EVENT_TARGETS_ID);
		eventDao.assignEventTargets(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<EventTargets> members = eventDao.getEventTargets(lastEventId, new DateTime());
		assertNotNull(members);
		assertEquals(1, members.size());
		EventTargets eventTargets = members.iterator().next();
		assertNotNull(eventTargets);
		assertEquals(TEST_EVENT_TARGETS_ID, eventTargets.getId());
	}

	/**
	 * Test a event with no members is OK.
	 */
	@Test
	public void emptyEventUserMemberSet() {
		insertEvent();
		Set<Member> members = eventDao.getUserMembers(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getUserMember() {
		insertEvent();
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_user (evt_id,usr_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_USER_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new User(TEST_USER_ID));

		Set<Member> found = eventDao.getUserMembers(lastEventId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignUserMember() {
		insertEvent();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_USER_ID);
		eventDao.assignUserMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new User(TEST_USER_ID));
		
		Set<Member> found = eventDao.getUserMembers(lastEventId, new DateTime());
		validateMembers(members, found);
	}

	/**
	 * Test a event with no member groups is OK.
	 */
	@Test
	public void emptyEventGroupSet() {
		insertEvent();
		Set<Member> members = eventDao.getUserGroupMembers(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getGroupMember() {
		insertEvent();
		setupTestLocation();
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_user_group (evt_id,ugr_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_GROUP_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new UserGroup(TEST_GROUP_ID));

		Set<Member> found = eventDao.getUserGroupMembers(lastEventId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignGroupMember() {
		insertEvent();
		setupTestLocation();
		setupTestUserGroup(TEST_GROUP_ID, TEST_GROUPNAME, TEST_LOCATION_ID);
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_GROUP_ID);
		eventDao.assignUserGroupMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new UserGroup(TEST_GROUP_ID));
		
		Set<Member> found = eventDao.getUserGroupMembers(lastEventId, new DateTime());
		validateMembers(members, found);
	}

	/**
	 * Test a event with no participants is OK.
	 */
	@Test
	public void emptyEventParticipantMemberSet() {
		insertEvent();
		Set<Member> members = eventDao.getParticipantMembers(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getParticipantMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_participant (evt_id,par_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_PARTICIPANT_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new Participant(TEST_PARTICIPANT_ID));

		Set<Member> found = eventDao.getParticipantMembers(lastEventId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignParticipantMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_ID);
		eventDao.assignParticipantMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new Participant(TEST_PARTICIPANT_ID));
		
		Set<Member> found = eventDao.getParticipantMembers(lastEventId, new DateTime());
		validateMembers(members, found);
	}

	/**
	 * Test a event with no participant groups is OK.
	 */
	@Test
	public void emptyEventParticipantGroupMemberSet() {
		insertEvent();
		Set<Member> members = eventDao.getParticipantGroupMembers(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getParticipantGroupMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		setupTestParticipantGroup();
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_participant_group (evt_id,pgr_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_PARTICIPANT_GROUP_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new ParticipantGroup(TEST_PARTICIPANT_GROUP_ID));

		Set<Member> found = eventDao.getParticipantGroupMembers(lastEventId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignParticipantGroupMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		setupTestParticipantGroup();
		assignParticipantToParticipantGroup(TEST_PARTICIPANT_GROUP_ID, TEST_PARTICIPANT_ID, TEST_EFFECTIVE_ID);
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_GROUP_ID);
		eventDao.assignParticipantGroupMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new ParticipantGroup(TEST_PARTICIPANT_GROUP_ID));
		
		Set<Member> found = eventDao.getParticipantGroupMembers(lastEventId, new DateTime());
		validateMembers(members, found);
	}

	/**
	 * Test a event with no locations is OK.
	 */
	@Test
	public void emptyEventLocationMemberSet() {
		insertEvent();
		Set<Member> members = eventDao.getLocationMembers(lastEventId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getLocationMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_loc (evt_id,loc_id,eff_id) values (?,?,?)", 
				lastEventId, TEST_LOCATION_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new Location(TEST_LOCATION_ID));

		Set<Member> found = eventDao.getLocationMembers(lastEventId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignLocationMember() {
		insertEvent();
		setupTestLocation();
		setupTestParticipant();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_LOCATION_ID);
		eventDao.assignLocationMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new Location(TEST_LOCATION_ID));
		
		Set<Member> found = eventDao.getLocationMembers(lastEventId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void findFilteredName() {
		insertEvent();
		
		SimpleEventFilter filter = new SimpleEventFilter();
		filter.setName("dontfindme");
		FilterResults<Match> results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		filter.setName("unit test");
		
		results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastEventId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredProgram() {
		insertEvent();
		
		SimpleEventFilter filter = new SimpleEventFilter();
		filter.setProgramId(-8888L);
		FilterResults<Match> results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		filter.setProgramId(TEST_PROGRAM_ID);
		
		results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastEventId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void resolveUsers() {
		// assign TEST_USER_ID to event indirectly, via participant group
		assignParticipantGroupMember();

		setupTestUser(-8888L, "test.user.2");
		setupTestUser(-8887L, "test.user.3");

		
		// assign another user, via participant
		setupTestLocation(-7777L, "Another Location");
		setupTestParticipant(-8886L, -8888L, -7777L);
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(-8886L);
		eventDao.assignParticipantMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		// assign another user, directly
		memberIds.clear();
		memberIds.add(-8887L);
		eventDao.assignUserMembers(lastEventId, memberIds, TEST_EFFECTIVE_ID);
		
		// now we should get our 3 users back
		Set<Member> results = eventDao.resolveUserMembers(lastEventId, null);
		assertNotNull(results);
		assertEquals(3, results.size());
		assertTrue(results.contains(new User(TEST_USER_ID)));
		assertTrue(results.contains(new User(-8888L)));
		assertTrue(results.contains(new User(-8887L)));
	}
	
	@Test
	public void findFilteredUser() {
		insertEvent();
		
		// search before user has been assigned to program, should NOT find program
		SimpleEventFilter filter = new SimpleEventFilter();
		filter.setUserId(TEST_USER_ID);
		FilterResults<Match> results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// now add user to program, and search again, SHOULD find
		assignUserToProgram(TEST_PROGRAM_ID, TEST_USER_ID, TEST_EFFECTIVE_ID);
		
		results = eventDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastEventId, results.getResults().iterator().next().getId());
	}
}
