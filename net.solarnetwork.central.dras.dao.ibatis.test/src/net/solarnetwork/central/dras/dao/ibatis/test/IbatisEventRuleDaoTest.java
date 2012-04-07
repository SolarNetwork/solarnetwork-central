/* ==================================================================
 * IbatisEventRuleDaoTest.java - Jun 6, 2011 5:11:28 PM
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

import java.util.LinkedHashSet;
import java.util.Set;

import net.solarnetwork.central.dras.dao.EventRuleDao;
import net.solarnetwork.central.dras.domain.EventRule;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for {@link EventRuleDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisEventRuleDaoTest extends AbstractIbatisDaoTestSupport {

	/** EventRuleDao to test. */
	@Autowired
	protected EventRuleDao eventRuleDao;
	
	private Long lastEventRuleId;
	
	@Before
	public void setup() {
		lastEventRuleId = null;
	}
	
	/**
	 * Test get EventRule.
	 */
	@Test
	public void getEventRuleById() {
		setupTestEventRule();
		EventRule eventRule = eventRuleDao.get(TEST_EVENT_RULE_ID);
		assertNotNull(eventRule);
		assertNotNull(eventRule.getId());
		assertEquals(TEST_EVENT_RULE_ID, eventRule.getId());
		assertEquals(TEST_EVENT_RULE_NAME, eventRule.getName());
		assertEquals(Double.valueOf(3), eventRule.getMin());
		assertEquals(Double.valueOf(4), eventRule.getMax());
	}
	
	/**
	 * Test get EventRule that doesn't exist.
	 */
	@Test
	public void getNonExistingEventRuleById() {
		EventRule eventRule = eventRuleDao.get(-99999L);
		assertNull(eventRule);
	}
	
	private void validateEventRule(EventRule eventRule, EventRule entity) {
		assertNotNull("EventRule should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(eventRule.getCreator(), entity.getCreator());
		assertEquals(eventRule.getEnumeration(), entity.getEnumeration());
		assertEquals(eventRule.getId(), entity.getId());
		assertEquals(eventRule.getKind(), entity.getKind());
		assertEquals(eventRule.getMax(), entity.getMax());
		assertEquals(eventRule.getMin(), entity.getMin());
		assertEquals(eventRule.getName(), entity.getName());
		assertEquals(eventRule.getSchedule(), entity.getSchedule());
		assertEquals(eventRule.getScheduleKind(), entity.getScheduleKind());
	}
	
	/**
	 * Test store new EventRule.
	 */
	@Test
	public void insertEventRule() {
		EventRule eventRule = new EventRule();
		eventRule.setName(TEST_EVENT_RULE_NAME);
		eventRule.setCreator(TEST_USER_ID);
		eventRule.setKind(EventRule.RuleKind.GRID_RELIABILITY);
		eventRule.setMax(2.0);
		eventRule.setMin(1.0);
		eventRule.setName(TEST_EVENT_RULE_NAME);
		eventRule.setScheduleKind(EventRule.ScheduleKind.DYNAMIC);
		
		Set<Double> enumeration = new LinkedHashSet<Double>(3);
		enumeration.add(1.0);
		enumeration.add(2.0);
		enumeration.add(3.0);
		eventRule.setEnumeration(enumeration);
		
		Set<Duration> schedule = new LinkedHashSet<Duration>(3);
		schedule.add(new Duration(1000));
		schedule.add(new Duration(2000));
		schedule.add(new Duration(3000));
		eventRule.setSchedule(schedule);

		logger.debug("Inserting new EventRule: " +eventRule);
		
		Long id = eventRuleDao.store(eventRule);
		assertNotNull(id);
		
		EventRule entity = eventRuleDao.get(id);
		validateEventRule(eventRule, entity);
		
		lastEventRuleId = id;
	}

	/**
	 * Test store updated User.
	 */
	@Test
	public void updateEventRule() {
		insertEventRule();
		
		EventRule eventRule = eventRuleDao.get(lastEventRuleId);
		assertEquals(TEST_EVENT_RULE_NAME, eventRule.getName());
		eventRule.setName("foo.update");
		
		Long id = eventRuleDao.store(eventRule);
		assertEquals(lastEventRuleId, id);
		
		EventRule entity = eventRuleDao.get(id);
		validateEventRule(eventRule, entity);
	}

	
}
