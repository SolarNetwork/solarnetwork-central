/* ==================================================================
 * EventExecutionInfoDaoTest.java - Jun 30, 2011 2:05:38 PM
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

import java.util.ArrayList;

import net.solarnetwork.central.dras.dao.EventDao;
import net.solarnetwork.central.dras.dao.EventExecutionInfoDao;
import net.solarnetwork.central.dras.domain.EventExecutionInfo;
import net.solarnetwork.central.dras.domain.Member;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link EventExecutionInfoDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventExecutionInfoDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired private EventExecutionInfoDao eventExecutionInfoDao;
	@Autowired private EventDao eventDao;
	
	private EventExecutionInfo lastEventExecutionInfo;
	
	@Before
	public void setup() {
		lastEventExecutionInfo = null;
		setupTestProgram(TEST_PROGRAM_ID, "Test Program");
		setupTestEvent(TEST_EVENT_ID, TEST_PROGRAM_ID);
	}

	private EventExecutionInfo createEventExecutionInfo() {
		
		EventExecutionInfo info = new EventExecutionInfo();
		info.setEvent(eventDao.get(TEST_EVENT_ID));
		
		info.setParticipants(new ArrayList<Member>(
				eventDao.resolveUserMembers(TEST_EVENT_ID, new DateTime())));
		
		return info;
	}
	

	private void validateEventExecutionInfo(EventExecutionInfo expected, EventExecutionInfo found) {
		assertNotNull("EventExecutionInfo should exist", found);
		assertEquals(expected.getId(), found.getId());
		
		assertEquals(expected.getEvent(), found.getEvent());
		assertEquals(expected.getExecutionDate(), found.getExecutionDate());
		assertEquals(expected.getExecutionKey(), found.getExecutionKey());
		assertEquals(expected.getModes(), found.getModes());
		assertEquals(expected.getParticipants(), found.getParticipants());
		assertEquals(expected.getTargets(), found.getTargets());
	}
	
	@Test
	public void insertEventExecutionInfo() {
		EventExecutionInfo c = createEventExecutionInfo();
		
		Long id = eventExecutionInfoDao.store(c);
		assertNotNull(id);
		
		c.setId(id);
		lastEventExecutionInfo = c;
	}

	@Test
	public void getEventExecutionInfoById() {
		insertEventExecutionInfo();
		EventExecutionInfo entity = eventExecutionInfoDao.get(lastEventExecutionInfo.getId());
		validateEventExecutionInfo(lastEventExecutionInfo, entity);
	}
	
	@Test
	public void getNonExistingEventExecutionInfoById() {
		EventExecutionInfo eventExecutionInfo = eventExecutionInfoDao.get(-99999L);
		assertNull(eventExecutionInfo);
	}
	
	@Test
	public void updateEventExecutionInfo() {
		insertEventExecutionInfo();
		
		assertNull(lastEventExecutionInfo.getExecutionKey());
		lastEventExecutionInfo.setExecutionKey("foo");
		
		eventExecutionInfoDao.store(lastEventExecutionInfo);
		
		EventExecutionInfo entity = eventExecutionInfoDao.get(lastEventExecutionInfo.getId());
		validateEventExecutionInfo(lastEventExecutionInfo, entity);
	}

}
