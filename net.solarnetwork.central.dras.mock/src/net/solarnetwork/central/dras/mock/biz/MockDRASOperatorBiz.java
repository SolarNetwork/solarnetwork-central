/* ==================================================================
 * MockDRASOperatorBiz.java - May 9, 2011 7:01:51 PM
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

package net.solarnetwork.central.dras.mock.biz;

import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.biz.DRASOperatorBiz;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.EventRule;
import net.solarnetwork.central.dras.domain.EventRule.RuleKind;
import net.solarnetwork.central.dras.domain.EventRule.ScheduleKind;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.domain.ProgramParticipants;

import org.joda.time.Duration;
import org.joda.time.ReadableDateTime;

/**
 * Mock implementation of {@link DRASOperatorBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class MockDRASOperatorBiz implements DRASOperatorBiz {

	private MockDRASObserverBiz mockObserverBiz;
	
	/*@Override
	public NodeGroupInformation createGroup(Location location) {
		Long id = mockObserverBiz.counter.decrementAndGet();
		SolarNodeGroup group = new SolarNodeGroup(id,
				mockObserverBiz.uniLocation.getId(), 
				"Mock Group " +id.toString());
		mockObserverBiz.addGroup(group);
		return new SimpleNodeGroupInformation(group.getName(), 
				new SolarNodeGroupCapability(), mockObserverBiz.uniLocation);
	}*/

	@Override
	public Program createProgram(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ProgramParticipants assignProgramParticipants(Program program,
			Set<Identity<Long>> participants) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Event createEvent(Program program, String name,
			ReadableDateTime eventDate, ReadableDateTime endDate) {
		Event e = new Event(mockObserverBiz.counter.decrementAndGet(),
				program.getId(), 
				(name != null ? name : String.format("Mock Event %d", (mockObserverBiz.getEvents(program, null, null).size()+1))),
				eventDate.toDateTime(),
				endDate.toDateTime()
				);
		
		EventParticipants ep = new EventParticipants(mockObserverBiz.counter.decrementAndGet(), 
				e.getId(), null, null);
		
		EventRule rule = new EventRule(mockObserverBiz.counter.decrementAndGet(), 
				RuleKind.LOAD_AMOUNT, ScheduleKind.DYNAMIC);
		mockObserverBiz.addEventRule(rule);

		EventTargets et = new EventTargets(mockObserverBiz.counter.decrementAndGet(), rule.getId(), 
				new TreeSet<EventTarget>(Arrays.asList(new EventTarget(Duration.ZERO, 1000D))));
		mockObserverBiz.addEvent(e, ep, et);
		return e;
	}

	@Override
	public EventParticipants assignEventParticipants(Event event, Set<Identity<Long>> participants) {
		// maintain any existing groups and reset participants
		EventParticipants curr = mockObserverBiz.getCurrentEventParticipants(event);
		EventParticipants ep = new EventParticipants(mockObserverBiz.counter.decrementAndGet(), 
				event.getId(), participants, 
				(curr == null ? null : curr.getGroups()));
		mockObserverBiz.setEventParticipants(event, ep);
		return ep;
	}

	@Override
	public EventParticipants assignEventParticipantGroups(Event event, Set<Identity<Long>> groups) {
		// maintain any existing participants and reset groups
		EventParticipants curr = mockObserverBiz.getCurrentEventParticipants(event);
		EventParticipants ep = new EventParticipants(mockObserverBiz.counter.decrementAndGet(), 
				event.getId(),
				(curr == null ? null : curr.getParticipants()),
				groups);
		mockObserverBiz.setEventParticipants(event, ep);
		return ep;
	}

	@Override
	public EventTargets assignEventTargets(Event event,
			SortedSet<EventTarget> targets) {
		EventTargets et = new EventTargets(mockObserverBiz.counter.decrementAndGet(), event.getId(), targets);
		mockObserverBiz.setEventTargets(event, et);
		return et;
	}

	/**
	 * @return the mockObserverBiz
	 */
	public MockDRASObserverBiz getMockObserverBiz() {
		return mockObserverBiz;
	}

	/**
	 * @param mockObserverBiz the mockObserverBiz to set
	 */
	public void setMockObserverBiz(MockDRASObserverBiz mockObserverBiz) {
		this.mockObserverBiz = mockObserverBiz;
	}

}
