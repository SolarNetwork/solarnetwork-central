/* ==================================================================
 * IbatisEventTargetsDaoTest.java - Jun 6, 2011 8:24:56 PM
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

import java.util.SortedSet;
import java.util.TreeSet;

import net.solarnetwork.central.dras.dao.EventTargetsDao;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link EventTargetsDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventTargetsDaoTest extends AbstractIbatisDaoTestSupport {

	/** EventTargetsDao to test. */
	@Autowired
	protected EventTargetsDao eventTargetsDao;
	
	private Long lastEventTargetsId;
	
	@Before
	public void setup() {
		lastEventTargetsId = null;
	}
	
	/**
	 * Test get EventTargets.
	 */
	@Test
	public void getEventTargetsById() {
		setupTestEventRule();
		setupTestEventTargets(TEST_EVENT_RULE_ID);
		EventTargets eventTargets = eventTargetsDao.get(TEST_EVENT_TARGETS_ID);
		assertNotNull(eventTargets);
		assertNotNull(eventTargets.getId());
		assertEquals(TEST_EVENT_TARGETS_ID, eventTargets.getId());
		assertEquals(TEST_EVENT_RULE_ID, eventTargets.getEventRuleId());
		assertEquals(new Duration(1000*60*60), eventTargets.getOverallDuration());
	}
	
	/**
	 * Test get EventTargets that doesn't exist.
	 */
	@Test
	public void getNonExistingEventTargetsById() {
		EventTargets eventTargets = eventTargetsDao.get(-99999L);
		assertNull(eventTargets);
	}
	
	private void validateEventTargets(EventTargets eventTargets, EventTargets entity) {
		assertNotNull("EventTargets should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(eventTargets.getEventRuleId(), entity.getEventRuleId());
		assertEquals(eventTargets.getId(), entity.getId());
		assertEquals(eventTargets.getOverallDuration(), entity.getOverallDuration());
		assertEquals(eventTargets.getTargets(), entity.getTargets());
	}
	
	/**
	 * Test store new EventTargets.
	 */
	@Test
	public void insertEventTargets() {
		setupTestEventRule();
		EventTargets eventTargets = new EventTargets();
		eventTargets.setEventRuleId(TEST_EVENT_RULE_ID);
		eventTargets.setOverallDuration(new Duration(1000*60*60));
		
		SortedSet<EventTarget> targets = new TreeSet<EventTarget>();
		targets.add(new EventTarget(new Duration(1000*60*15), 1.0));
		targets.add(new EventTarget(new Duration(1000*60*30), 2.0));
		targets.add(new EventTarget(new Duration(1000*60*45), 3.0));
		eventTargets.setTargets(targets);

		logger.debug("Inserting new EventTargets: " +eventTargets);
		
		Long id = eventTargetsDao.store(eventTargets);
		assertNotNull(id);
		
		EventTargets entity = eventTargetsDao.get(id);
		validateEventTargets(eventTargets, entity);
		
		lastEventTargetsId = id;
	}

	/**
	 * Test store updated User.
	 */
	@Test
	public void updateEventTargets() {
		insertEventTargets();
		
		EventTargets eventTargets = eventTargetsDao.get(lastEventTargetsId);
		eventTargets.setOverallDuration(new Duration(1000*60*50));
		
		Long id = eventTargetsDao.store(eventTargets);
		assertEquals(lastEventTargetsId, id);
		
		EventTargets entity = eventTargetsDao.get(id);
		validateEventTargets(eventTargets, entity);
	}


}
