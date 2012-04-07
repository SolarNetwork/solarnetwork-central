/* ==================================================================
 * IbatisParticipantGroupDaoTest.java - Jun 6, 2011 4:15:53 PM
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
import java.util.Set;

import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.dras.dao.CapabilityDao;
import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.FeeDao;
import net.solarnetwork.central.dras.dao.LocationDao;
import net.solarnetwork.central.dras.dao.ParticipantGroupDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.CapableParticipantGroup;
import net.solarnetwork.central.dras.support.SimpleParticipantGroupFilter;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link ParticipantGroupDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisParticipantGroupDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired protected CapabilityDao capabilityDao;
	@Autowired protected EventDao eventDao;
	@Autowired protected ParticipantGroupDao participantGroupDao;
	@Autowired protected LocationDao locationDao;
	@Autowired protected ProgramDao programDao;
	@Autowired private FeeDao feeDao;
	
	private Long lastParticipantGroupId;
	
	@Before
	public void setup() {
		lastParticipantGroupId = null;
		setupTestLocation();
	}
	
	/**
	 * Test get ParticipantGroup.
	 */
	@Test
	public void getParticipantGroupById() {
		setupTestParticipantGroup();
		ParticipantGroup group = participantGroupDao.get(TEST_PARTICIPANT_GROUP_ID);
		assertNotNull(group);
		assertNotNull(group.getId());
		assertEquals(TEST_PARTICIPANT_GROUP_ID, group.getId());
		assertEquals(TEST_LOCATION_ID, group.getLocationId());
	}
	
	/**
	 * Test get ParticipantGroup that doesn't exist.
	 */
	@Test
	public void getNonExistingParticipantGroupById() {
		ParticipantGroup group = participantGroupDao.get(-99999L);
		assertNull(group);
	}
	
	private void validateParticipantGroup(ParticipantGroup group, ParticipantGroup entity) {
		assertNotNull("ParticipantGroup should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(group.getConfirmed(), entity.getConfirmed());
		assertEquals(group.getCreator(), entity.getCreator());
		assertEquals(group.getEnabled(), entity.getEnabled());
		assertEquals(group.getId(), entity.getId());
		assertEquals(group.getLocationId(), entity.getLocationId());
		assertEquals(group.getVerificationMethodId(), entity.getVerificationMethodId());
		assertEquals(group.getCapability(), entity.getCapability());
	}
	
	/**
	 * Test store new ParticipantGroup.
	 */
	@Test
	public void insertParticipantGroup() {
		ParticipantGroup group = new ParticipantGroup();
		group.setCreator(TEST_USER_ID);
		group.setConfirmed(Boolean.FALSE);
		group.setEnabled(Boolean.TRUE);
		group.setLocationId(TEST_LOCATION_ID);
		
		logger.debug("Inserting new ParticipantGroup: " +group);
		
		Long id = participantGroupDao.store(group);
		assertNotNull(id);
		
		ParticipantGroup entity = participantGroupDao.get(id);
		validateParticipantGroup(group, entity);
		
		lastParticipantGroupId = id;
	}

	/**
	 * Test store updated User.
	 */
	@Test
	public void updateParticipantGroup() {
		insertParticipantGroup();
		
		ParticipantGroup group = participantGroupDao.get(lastParticipantGroupId);
		group.setEnabled(Boolean.FALSE);
		
		Long id = participantGroupDao.store(group);
		assertEquals(lastParticipantGroupId, id);
		
		ParticipantGroup entity = participantGroupDao.get(id);
		validateParticipantGroup(group, entity);
	}

	/**
	 * Test a group with no capability is OK.
	 */
	@Test
	public void emptyParticipantGroupCapability() {
		insertParticipantGroup();
		ParticipantGroup g = participantGroupDao.get(lastParticipantGroupId);
		assertNull(g.getCapability());
	}
	
	@Test
	public void getCapability() {
		insertParticipantGroup();
		setupTestCapability();
		simpleJdbcTemplate.update(
				"UPDATE solardras.participant_group SET cap_id = ? WHERE id = ?", 
				TEST_CAPABILITY_ID, lastParticipantGroupId);

		ParticipantGroup g = participantGroupDao.get(lastParticipantGroupId);
		Capability capability = g.getCapability();
		assertNotNull(capability);
		assertEquals(TEST_CAPABILITY_ID, capability.getId());
	}
	
	@Test
	public void setCapability() {
		insertParticipantGroup();
		setupTestCapability();
		ParticipantGroup g = participantGroupDao.get(lastParticipantGroupId);
		Capability c = capabilityDao.get(TEST_CAPABILITY_ID);
		c.setContractedCapacityWatts(10000L);
		g.setCapability(c);
		participantGroupDao.store(g);
		
		ParticipantGroup entity = participantGroupDao.get(lastParticipantGroupId);
		assertNotNull(entity);
		assertNotNull(entity.getCapability());
		validateParticipantGroup(g, entity);
	}

	/**
	 * Test a program with no participants is OK.
	 */
	@Test
	public void emptyParticipantGroupParticipantMemberSet() {
		insertParticipantGroup();
		Set<Member> members = participantGroupDao.getParticipantMembers(lastParticipantGroupId, null);
		assertNotNull(members);
		assertEquals(0, members.size());
	}
	
	@Test
	public void getParticipantMember() {
		insertParticipantGroup();
		setupTestParticipant();
		simpleJdbcTemplate.update(
				"insert into solardras.participant_group_member (pgr_id,par_id,eff_id) values (?,?,?)", 
				lastParticipantGroupId, TEST_PARTICIPANT_ID, TEST_EFFECTIVE_ID);

		Set<Member> expected = new HashSet<Member>(1);
		expected.add(new Participant(TEST_PARTICIPANT_ID));

		Set<Member> found = participantGroupDao.getParticipantMembers(lastParticipantGroupId, new DateTime());
		validateMembers(expected, found);
	}
	
	@Test
	public void assignParticipantMember() {
		insertParticipantGroup();
		setupTestParticipant();
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_ID);
		participantGroupDao.assignParticipantMembers(lastParticipantGroupId, memberIds, TEST_EFFECTIVE_ID);
		
		Set<Member> members = new HashSet<Member>(1);
		members.add(new Participant(TEST_PARTICIPANT_ID));
		
		Set<Member> found = participantGroupDao.getParticipantMembers(lastParticipantGroupId, new DateTime());
		validateMembers(members, found);
	}

	@Test
	public void findFilteredName() {
		insertParticipantGroup();
		
		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setName("test");
		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastParticipantGroupId, results.getResults().iterator().next().getId());
	}
	
	@Test
	public void findFilteredBoxEmptySet() {
		insertParticipantGroup();
		
		// now search for box coordinates
		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredBox() {
		assignParticipantMember();

		// create a location with lat/long
		Location l = locationDao.get(TEST_LOCATION_ID);
		l.setLatitude(1.1234);
		l.setLongitude(2.3456);
		locationDao.store(l);
		
		// now search for box coordinates
		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastParticipantGroupId, results.getResults().iterator().next().getId());
	}

	@Test
	public void findFilteredCapability() {
		setCapability();

		// create a location with lat/long
		Location l = locationDao.get(TEST_LOCATION_ID);
		l.setLatitude(1.1234);
		l.setLongitude(2.3456);
		l.setGxp("test.gxp");
		locationDao.store(l);
		
		// now search for box coordinates
		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);
		filter.setIncludeCapability(Boolean.TRUE);

		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantGroupId, m.getId());
		assertEquals(CapableParticipantGroup.class, m.getClass());
		CapableParticipantGroup info = (CapableParticipantGroup)m;
		assertNotNull(info.getLocationEntity());
		assertEquals(l.getId(), info.getLocationEntity().getId());
	}

	@Test
	public void findFilteredEvent() {
		setCapability();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);

		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setEventId(-88889L);
		
		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// now search for event
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantGroupId);
		eventDao.assignParticipantGroupMembers(TEST_EVENT_ID, memberIds, TEST_EFFECTIVE_ID);

		filter.setEventId(TEST_EVENT_ID);

		results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantGroupId, m.getId());
		assertEquals(ParticipantGroup.class, m.getClass());
	}

	@Test
	public void findFilteredEventCapability() {
		setCapability();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);

		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setEventId(-88889L);
		filter.setIncludeCapability(Boolean.TRUE);
		
		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// now search for event
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantGroupId);
		eventDao.assignParticipantGroupMembers(TEST_EVENT_ID, memberIds, TEST_EFFECTIVE_ID);
		filter.setEventId(TEST_EVENT_ID);

		results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantGroupId, m.getId());
		assertEquals(CapableParticipantGroup.class, m.getClass());
	}

	@Test
	public void findFilteredProgram() {
		assignParticipantMember();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		
		SimpleParticipantGroupFilter filter = new SimpleParticipantGroupFilter();
		filter.setProgramId(TEST_PROGRAM_ID);
		
		FilterResults<Match> results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// assign participant to program... we are searching for groups with some participant
		// that is a member of the given program
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(TEST_PARTICIPANT_ID);
		programDao.assignParticipantMembers(TEST_PROGRAM_ID, memberIds, TEST_EFFECTIVE_ID);

		results = participantGroupDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantGroupId, m.getId());
		assertEquals(ParticipantGroup.class, m.getClass());
	}

	@Test
	public void setFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		insertParticipantGroup();
		
		participantGroupDao.setFee(lastParticipantGroupId, f.getId(), TEST_EFFECTIVE_ID);
	}
	
	@Test 
	public void getFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		
		
		Fee found = participantGroupDao.getFee(lastParticipantGroupId, null);
		assertNull(found);
		
		insertParticipantGroup();
		participantGroupDao.setFee(lastParticipantGroupId, f.getId(), TEST_EFFECTIVE_ID);
		
		found = participantGroupDao.getFee(lastParticipantGroupId, null);
		assertNotNull(found);
		assertEquals(f.getId(), found.getId());
	}

}
