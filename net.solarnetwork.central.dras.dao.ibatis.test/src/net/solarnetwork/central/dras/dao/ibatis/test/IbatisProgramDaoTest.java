/* ==================================================================
 * IbatisProgramDaoTest.java - Jun 5, 2011 7:31:14 PM
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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.ConstraintDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EventRule;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.domain.User;
import net.solarnetwork.central.dras.support.SimpleProgramFilter;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link net.solarnetwork.central.dras.dao.ibatis.IbatisProgramDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisProgramDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private ConstraintDao constraintDao;
	@Autowired private ProgramDao programDao;
	
	private Long lastProgramId;
	
	@Before
	public void setup() {
		lastProgramId = null;
	}
	
	@Test
	public void getProgramById() {
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		Program program = programDao.get(TEST_PROGRAM_ID);
		assertNotNull(program);
		assertNotNull(program.getId());
		assertEquals(TEST_PROGRAM_ID, program.getId());
		assertEquals(TEST_PROGRAM_NAME, program.getName());
		assertEquals(Boolean.TRUE, program.getEnabled());
	}
	
	@Test
	public void getNonExistingProgramById() {
		Program program = programDao.get(-99999L);
		assertNull(program);
	}
	
	private void validateProgram(Program program, Program entity) {
		assertNotNull("Program should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(program.getCreator(), entity.getCreator());
		assertEquals(program.getEnabled(), entity.getEnabled());
		assertEquals(program.getId(), entity.getId());
		assertEquals(program.getName(), entity.getName());
		assertEquals(program.getPriority(), entity.getPriority());
	}
	
	@Test
	public void insertProgram() {
		Program program = new Program();
		program.setCreator(TEST_USER_ID);
		program.setName(TEST_PROGRAM_NAME);
		program.setEnabled(Boolean.TRUE);
		program.setPriority(0);
		
		logger.debug("Inserting new Program: " +program);
		
		Long id = programDao.store(program);
		assertNotNull(id);
		
		Program entity = programDao.get(id);
		validateProgram(program, entity);
		
		lastProgramId = id;
	}

	@Test
	public void updateProgram() {
		insertProgram();
		
		Program program = programDao.get(lastProgramId);
		assertEquals(TEST_PROGRAM_NAME, program.getName());
		program.setName("foo.update");
		
		Long id = programDao.store(program);
		assertEquals(lastProgramId, id);
		
		Program entity = programDao.get(id);
		validateProgram(program, entity);
	}

	@Test
	public void emptyProgramUserMemberSet() {
		insertProgram();
		Set<Member> members = programDao.getUserMembers(lastProgramId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getUserMember() {
		insertProgram();
		simpleJdbcTemplate.update(
				"insert into solardras.program_user (pro_id,usr_id,eff_id) values (?,?,?)", 
				lastProgramId, TEST_USER_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new User(TEST_USER_ID));

		Set<Member> found = programDao.getUserMembers(lastProgramId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignUserMember() {
		insertProgram();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_USER_ID);
		programDao.assignUserMembers(lastProgramId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new User(TEST_USER_ID));
		
		Set<Member> found = programDao.getUserMembers(lastProgramId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void emptyProgramParticipantMemberSet() {
		insertProgram();
		Set<Member> members = programDao.getParticipantMembers(lastProgramId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getParticipantMember() {
		insertProgram();
		setupTestLocation();
		setupTestParticipant();
		simpleJdbcTemplate.update(
				"insert into solardras.program_participant (pro_id,par_id,eff_id) values (?,?,?)", 
				lastProgramId, TEST_PARTICIPANT_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new Participant(TEST_PARTICIPANT_ID));

		Set<Member> found = programDao.getParticipantMembers(lastProgramId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignParticipantMember() {
		insertProgram();
		setupTestLocation();
		setupTestParticipant();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_ID);
		programDao.assignParticipantMembers(lastProgramId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new Participant(TEST_PARTICIPANT_ID));
		
		Set<Member> found = programDao.getParticipantMembers(lastProgramId, new DateTime());
		validateMembers(members, found);
	}
	
	@Test
	public void emptyProgramEventRuleSet() {
		insertProgram();
		Set<EventRule> members = programDao.getProgramEventRules(lastProgramId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getEventRule() {
		insertProgram();
		setupTestEventRule();
		simpleJdbcTemplate.update(
				"insert into solardras.program_event_rule (pro_id,evr_id,eff_id) values (?,?,?)", 
				lastProgramId, TEST_EVENT_RULE_ID, TEST_EFFECTIVE_ID);

		Set<EventRule> expected = new HashSet<EventRule>(1);
		expected.add(new EventRule(TEST_EVENT_RULE_ID));

		Set<EventRule> found = programDao.getProgramEventRules(lastProgramId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignEventRule() {
		insertProgram();
		setupTestEventRule();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_EVENT_RULE_ID);
		programDao.assignProgramEventRules(lastProgramId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<EventRule> members = new HashSet<EventRule>(1);
		members.add(new EventRule(TEST_EVENT_RULE_ID));
		
		Set<EventRule> found = programDao.getProgramEventRules(lastProgramId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void getEventRules() {
		insertProgram();
		
		Set<Double> enums = new HashSet<Double>(3);
		enums.add(1.0);
		enums.add(2.0);
		enums.add(3.0);
		
		Set<Duration> sched = new HashSet<Duration>(2);
		sched.add(new Duration(1000));
		sched.add(new Duration(1000*60));
		
		Set<EventRule> expected = new HashSet<EventRule>(1);
		for ( long id = -8889; id <= -8887; id++ ) {
			setupTestEventRule(id, "Test Rule " +id, enums, sched);
			expected.add(new EventRule(id));
			simpleJdbcTemplate.update(
					"insert into solardras.program_event_rule (pro_id,evr_id,eff_id) values (?,?,?)", 
					lastProgramId, id, TEST_EFFECTIVE_ID);
		}

		Set<EventRule> found = programDao.getProgramEventRules(lastProgramId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignEventRules() {
		insertProgram();
		setupTestEventRule(-8889L, "Test Rule 1", null, null);
		setupTestEventRule(-8888L, "Test Rule 2", null, null);
		setupTestEventRule(-8887L, "Test Rule 3", null, null);
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(-8889L);
		memberIds.add(-8888L);
		memberIds.add(-8887L);
		programDao.assignProgramEventRules(lastProgramId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<EventRule> members = new HashSet<EventRule>(memberIds.size());
		for ( Long memberId : memberIds ) {
			members.add(new EventRule(memberId));
		}
		
		Set<EventRule> found = programDao.getProgramEventRules(lastProgramId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void findFilteredName() {
		insertProgram();
		
		SimpleProgramFilter filter = new SimpleProgramFilter();
		filter.setName("test");
		FilterResults<Match> results = programDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastProgramId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredUser() {
		assignUserMember();
		
		SimpleProgramFilter filter = new SimpleProgramFilter();
		filter.setUserId(TEST_USER_ID);
		FilterResults<Match> results = programDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastProgramId, results.getResults().iterator().next().getId());
		
		// now verify don't find
		filter.setUserId(-88889L);
		results = programDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void assignConstraints() {
		Constraint c = createConstraint();
		c.setId(constraintDao.store(c));
		Constraint c2 = createConstraint();
		c2.setId(constraintDao.store(c2));
		insertProgram();
		
		Set<Constraint> members = new LinkedHashSet<Constraint>(2);
		members.add(c);
		members.add(c2);
		
		Set<Long> memberIds = new LinkedHashSet<Long>(2);
		memberIds.add(c.getId());
		memberIds.add(c2.getId());
		
		programDao.assignConstraints(lastProgramId, memberIds, TEST_EFFECTIVE_ID);

		Set<Constraint> found = programDao.getConstraints(lastProgramId, new DateTime());
		validateMembers(members, found);
	}
}
