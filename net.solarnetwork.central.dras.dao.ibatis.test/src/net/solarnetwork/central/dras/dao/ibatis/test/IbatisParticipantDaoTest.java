/* ==================================================================
 * IbatisParticipantDaoTest.java - Jun 6, 2011 12:59:04 PM
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
import net.solarnetwork.central.dras.dao.ParticipantDao;
import net.solarnetwork.central.dras.dao.ParticipantGroupDao;
import net.solarnetwork.central.dras.dao.ProgramDao;
import net.solarnetwork.central.dras.domain.Fee;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.support.CapableParticipant;
import net.solarnetwork.central.dras.support.SimpleParticipantFilter;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link ParticipantDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisParticipantDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired protected CapabilityDao capabilityDao;
	@Autowired protected EventDao eventDao;
	@Autowired protected ParticipantDao participantDao;
	@Autowired protected ParticipantGroupDao participantGroupDao;
	@Autowired protected LocationDao locationDao;
	@Autowired protected ProgramDao programDao;
	@Autowired private FeeDao feeDao;
	
	private Long lastParticipantId;
	
	@Before
	public void setup() {
		lastParticipantId = null;
		setupTestLocation();
	}
	
	@Test
	public void getParticipantById() {
		setupTestParticipant();
		Participant participant = participantDao.get(TEST_PARTICIPANT_ID);
		assertNotNull(participant);
		assertNotNull(participant.getId());
		assertEquals(TEST_PARTICIPANT_ID, participant.getId());
		assertEquals(TEST_USER_ID, participant.getUserId());
		assertEquals(TEST_LOCATION_ID, participant.getLocationId());
	}
	
	@Test
	public void getNonExistingParticipantById() {
		Participant participant = participantDao.get(-99999L);
		assertNull(participant);
	}
	
	private void validateParticipant(Participant participant, Participant entity) {
		assertNotNull("Participant should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(participant.getConfirmed(), entity.getConfirmed());
		assertEquals(participant.getCreator(), entity.getCreator());
		assertEquals(participant.getEnabled(), entity.getEnabled());
		assertEquals(participant.getId(), entity.getId());
		assertEquals(participant.getKind(), entity.getKind());
		assertEquals(participant.getLocationId(), entity.getLocationId());
		assertEquals(participant.getUserId(), entity.getUserId());
		assertEquals(participant.getVerificationMethodId(), entity.getVerificationMethodId());
		assertEquals(participant.getCapability(), entity.getCapability());
	}
	
	@Test
	public void insertParticipant() {
		Participant participant = new Participant();
		participant.setCreator(TEST_USER_ID);
		participant.setConfirmed(Boolean.FALSE);
		participant.setEnabled(Boolean.TRUE);
		participant.setKind(Participant.ParticipantKind.SIMPLE);
		participant.setLocationId(TEST_LOCATION_ID);
		participant.setUserId(TEST_USER_ID);
		
		logger.debug("Inserting new Participant: " +participant);
		
		Long id = participantDao.store(participant);
		assertNotNull(id);
		
		Participant entity = participantDao.get(id);
		validateParticipant(participant, entity);
		
		lastParticipantId = id;
	}

	@Test
	public void updateParticipant() {
		insertParticipant();
		
		Participant participant = participantDao.get(lastParticipantId);
		assertEquals(Participant.ParticipantKind.SIMPLE, participant.getKind());
		participant.setKind(Participant.ParticipantKind.SMART);
		
		Long id = participantDao.store(participant);
		assertEquals(lastParticipantId, id);
		
		Participant entity = participantDao.get(id);
		validateParticipant(participant, entity);
	}

	@Test
	public void emptyParticipantCapability() {
		insertParticipant();
		Participant p = participantDao.get(lastParticipantId);
		assertNull(p.getCapability());
	}
	
	@Test
	public void getCapability() {
		insertParticipant();
		setupTestCapability();
		simpleJdbcTemplate.update(
				"update solardras.participant SET cap_id = ? WHERE id = ?", 
				TEST_CAPABILITY_ID, lastParticipantId);

		Participant p = participantDao.get(lastParticipantId);
		assertNotNull(p.getCapability());
		assertEquals(TEST_CAPABILITY_ID, p.getCapability().getId());
	}
	
	@Test
	public void setCapability() {
		insertParticipant();
		setupTestCapability();
		Participant p = participantDao.get(lastParticipantId);
		p.setCapability(capabilityDao.get(TEST_CAPABILITY_ID));
		participantDao.store(p);
		
		Participant found = participantDao.get(lastParticipantId);
		validateParticipant(p, found);
	}

	@Test
	public void findFilteredBoxEmptySet() {
		insertParticipant();
		
		// now search for box coordinates
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
	}
	
	@Test
	public void findFilteredBox() {
		insertParticipant();

		// create a location with lat/long
		Location l = locationDao.get(TEST_LOCATION_ID);
		l.setLatitude(1.1234);
		l.setLongitude(2.3456);
		locationDao.store(l);
		
		// now search for box coordinates
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);

		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastParticipantId, results.getResults().iterator().next().getId());
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
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setLatitude(-3.3);
		filter.setLongitude(-3.5);
		filter.setBoxLatitude(3.3);
		filter.setBoxLongitude(3.5);
		filter.setIncludeCapability(Boolean.TRUE);

		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(CapableParticipant.class, m.getClass());
		CapableParticipant info = (CapableParticipant)m;
		assertNotNull(info.getLocationEntity());
		assertEquals(l.getId(), info.getLocationEntity().getId());
	}

	@Test
	public void findFilteredEvent() {
		setCapability();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);

		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setEventId(TEST_EVENT_ID);

		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());

		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		eventDao.assignParticipantMembers(TEST_EVENT_ID, memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(Participant.class, m.getClass());
	}

	@Test
	public void findFilteredEventCapability() {
		setCapability();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);

		// now search for box coordinates
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setEventId(TEST_EVENT_ID);
		filter.setIncludeCapability(Boolean.TRUE);

		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());

		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		eventDao.assignParticipantMembers(TEST_EVENT_ID, memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(CapableParticipant.class, m.getClass());
	}

	@Test
	public void findFilteredProgram() {
		insertParticipant();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setProgramId(TEST_PROGRAM_ID);
		
		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// assign participant to program... we are searching for groups with some participant
		// that is a member of the given program
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		programDao.assignParticipantMembers(TEST_PROGRAM_ID, memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(Participant.class, m.getClass());
	}

	@Test
	public void findFilteredProgramCapability() {
		insertParticipant();
		setupTestProgram(TEST_PROGRAM_ID, TEST_PROGRAM_NAME);
		
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setProgramId(TEST_PROGRAM_ID);
		filter.setIncludeCapability(Boolean.TRUE);
		
		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// assign participant to program... we are searching for groups with some participant
		// that is a member of the given program
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		programDao.assignParticipantMembers(TEST_PROGRAM_ID, memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(CapableParticipant.class, m.getClass());
	}

	@Test
	public void findFilteredParticipantGroup() {
		insertParticipant();
		setupTestParticipantGroup();
		
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setGroupId(TEST_PARTICIPANT_GROUP_ID);
		
		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// assign participant to participant group
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		participantGroupDao.assignParticipantMembers(TEST_PARTICIPANT_GROUP_ID, 
				memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(Participant.class, m.getClass());
	}

	@Test
	public void findFilteredParticipantGroupCapability() {
		insertParticipant();
		setupTestParticipantGroup();
		
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setGroupId(TEST_PARTICIPANT_GROUP_ID);
		filter.setIncludeCapability(Boolean.TRUE);
		
		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());
		
		// assign participant to participant group
		Set<Long> memberIds = new HashSet<Long>(1);
		memberIds.add(lastParticipantId);
		participantGroupDao.assignParticipantMembers(TEST_PARTICIPANT_GROUP_ID, 
				memberIds, TEST_EFFECTIVE_ID);

		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		Match m = results.getResults().iterator().next();
		assertEquals(lastParticipantId, m.getId());
		assertEquals(CapableParticipant.class, m.getClass());
	}

	@Test
	public void setFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		insertParticipant();
		
		participantDao.setFee(lastParticipantId, f.getId(), TEST_EFFECTIVE_ID);
	}
	
	@Test 
	public void getFee() {
		Fee f = createFee();
		f.setId(feeDao.store(f));
		
		
		Fee found = participantDao.getFee(lastParticipantId, null);
		assertNull(found);
		
		insertParticipant();
		participantDao.setFee(lastParticipantId, f.getId(), TEST_EFFECTIVE_ID);
		
		found = participantDao.getFee(lastParticipantId, null);
		assertNotNull(found);
		assertEquals(f.getId(), found.getId());
	}

	@Test
	public void findFilteredUser() {
		findFilteredEvent();
		
		// search for non-existing user, should not find any participants
		SimpleParticipantFilter filter = new SimpleParticipantFilter();
		filter.setUserId(-7777L);
		FilterResults<Match> results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());

		// now assign our test user, we should find
		filter.setUserId(TEST_USER_ID);
		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastParticipantId, results.getResults().iterator().next().getId());
		
		// now also filter by event and non-existing user ID, we should not find anything
		filter.setUserId(-7777L);
		filter.setEventId(TEST_EVENT_ID);
		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(0, results.getReturnedResultCount().intValue());

		// now by event and our user ID, we should find
		filter.setUserId(TEST_USER_ID);
		results = participantDao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(1, results.getReturnedResultCount().intValue());
		assertEquals(lastParticipantId, results.getResults().iterator().next().getId());
	}
}
