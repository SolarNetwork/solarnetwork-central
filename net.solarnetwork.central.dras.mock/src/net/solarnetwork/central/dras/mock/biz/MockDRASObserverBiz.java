/* ==================================================================
 * MockDRASObserverBiz.java - Apr 30, 2011 1:04:31 PM
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeGroup;
import net.solarnetwork.central.dras.biz.DRASObserverBiz;
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.dao.ParticipantFilter;
import net.solarnetwork.central.dras.domain.CapabilityInformation;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.EventRule;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.support.SimpleCapabilityInformation;
import net.solarnetwork.central.security.SecurityException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.MutableDateTime;
import org.joda.time.Period;

/**
 * Mock implementation of {@link DRASObserverBiz}.
 * 
 * @author matt
 * @version $Revision$
 */
public class MockDRASObserverBiz implements DRASObserverBiz {
	
	private List<SolarNodeGroup> uniGroups;
	private List<CapabilityInformation> participantGroups;
	private Map<Long, Set<CapabilityInformation>> participantGroupMemebers;
	private Set<Identity<Long>> uniProgramParticipants;
	private Program uniProgram;
	private List<Event> uniEvents;
	private Map<Long, EventParticipants> uniEventParticipants;
	private Map<Long, EventTargets> uniEventTargets;
	private Map<Long, SolarLocation> uniLocations;
	private Map<Long, EventRule> uniEventRules;

	private final double minLatitude = -37.5655F;
	private final double maxLatitude = -36.6670F;
	private final double minLongitude = 174.258957F;
	private final double maxLongitude = 175.663147F;
	
	private final double groupLatitude = -36.900764F;
	private final double groupLongitude = 174.772224F;

	private final Long participantGenerationCapacity = 10000L;
	private final Long participantStorageCapacity = 5000L;
	final int numGroups = 2;	
	final int numNodes = 5;
	final int numEvents = 15;
	
	// package visibility to share with operator
	SolarLocation uniLocation;
	AtomicLong counter = new AtomicLong(0);
	
	void addEventRule(EventRule rule) {
		if ( uniEventRules == null ) {
			uniEventRules = new LinkedHashMap<Long, EventRule>();
		}
		uniEventRules.put(rule.getId(), rule);
	}

	void addGroup(SolarNodeGroup group) {
		uniGroups.add(group);
	}
	
	void addEvent(Event event, EventParticipants participants, EventTargets targets) {
		uniEvents.add(event);
		uniEventParticipants.put(event.getId(), participants);
		uniEventTargets.put(event.getId(), targets);
	}
	
	void setEventTargets(Event event, EventTargets targets ) {
		uniEventTargets.put(event.getId(), targets);
	}
	
	void setEventParticipants(Event event, EventParticipants participants) {
		uniEventParticipants.put(event.getId(), participants);
	}
	
	SolarLocation createRandomLocation() {
		SolarLocation newLocation = (SolarLocation)uniLocation.clone();
		newLocation.setId(counter.decrementAndGet());
		newLocation.setCreated(new DateTime());
		double latOffset = Math.random() * Math.abs(maxLatitude - minLatitude);
		newLocation.setLatitude(minLatitude + latOffset);
		double longOffset = Math.random() * Math.abs(maxLongitude - minLongitude);
		newLocation.setLongitude(minLongitude + longOffset);
		
		uniLocations.put(newLocation.getId(), newLocation);
		return newLocation;
	}
	
	public MockDRASObserverBiz() {
		TimeZone tz = TimeZone.getTimeZone("Pacific/Auckland");
		
		uniLocation = new SolarLocation();
		uniLocation.setId(counter.decrementAndGet());
		uniLocation.setCountry("NZ");
		uniLocation.setCreated(new DateTime());
		uniLocation.setName("Mock Location");
		uniLocation.setRegion("UNI");
		uniLocation.setTimeZoneId(tz.getID());
		uniLocation.setLatitude(groupLatitude);
		uniLocation.setLongitude(groupLongitude);
		
		uniLocations = new LinkedHashMap<Long, SolarLocation>();
		uniLocations.put(uniLocation.getId(), uniLocation);
		
		uniGroups = new ArrayList<SolarNodeGroup>(numGroups);
		addGroup(new SolarNodeGroup(counter.decrementAndGet(), uniLocation.getId(), "Mock Group A"));
		addGroup(new SolarNodeGroup(counter.decrementAndGet(), uniLocation.getId(), "Mock Group B"));
		participantGroups = new ArrayList<CapabilityInformation>(numGroups);
		for ( SolarNodeGroup group : uniGroups ) {
			SimpleCapabilityInformation groupInfo = new SimpleCapabilityInformation();
			groupInfo.setId(group.getId());
			groupInfo.setLocation(uniLocation);
			groupInfo.setGenerationCapacityWatts(participantGenerationCapacity);
			groupInfo.setStorageCapacityWattHours(participantStorageCapacity);
			participantGroups.add(groupInfo);
		}
		
		uniProgramParticipants = new LinkedHashSet<Identity<Long>>(numNodes);
		participantGroupMemebers = new LinkedHashMap<Long, Set<CapabilityInformation>>(numGroups);
		for ( int i = 0; i < numNodes; i++ ) {
			SolarNode participant = new SolarNode(counter.decrementAndGet(), createRandomLocation().getId());
			uniProgramParticipants.add(participant);
			int groupIndex = (i % numGroups);
			SimpleCapabilityInformation group = (SimpleCapabilityInformation)participantGroups.get(groupIndex);
			Set<CapabilityInformation> groupMembers = participantGroupMemebers.get(group.getId());
			if ( groupMembers == null ) {
				groupMembers = new LinkedHashSet<CapabilityInformation>(numNodes);
				participantGroupMemebers.put(group.getId(), groupMembers);
			}
			SimpleCapabilityInformation info = new SimpleCapabilityInformation();
			info.setId(participant.getId());
			info.setLocation(uniLocations.get(participant.getLocationId()));
			info.setGenerationCapacityWatts(participantGenerationCapacity);
			info.setStorageCapacityWattHours(participantStorageCapacity);
			groupMembers.add(info);
			group.addGenerationCapacityWatts(participantGenerationCapacity);
			group.addStorageCapacityWattHours(participantStorageCapacity);
		}
		uniProgram = new Program(counter.decrementAndGet(), "UNI Program", 1);
		
		MutableDateTime mdt = new MutableDateTime(
				2011, 1, 1, 8, 0, 0, 0,
				DateTimeZone.forTimeZone(tz));
		uniEvents = new ArrayList<Event>(numEvents);
		uniEventParticipants = new LinkedHashMap<Long, EventParticipants>(numEvents);
		uniEventTargets = new LinkedHashMap<Long, EventTargets>(numEvents);
		
		EventRule eventRule = new EventRule(counter.decrementAndGet(), 
				EventRule.RuleKind.LOAD_AMOUNT,
				EventRule.ScheduleKind.DYNAMIC);
		addEventRule(eventRule);
		
		for ( int i = 0; i < numEvents; i++ ) {
			Event event = new Event(counter.decrementAndGet(), uniProgram.getId(), 
					String.format("Mock Event %d", (i+1)), 
					mdt.toDateTime(),
					mdt.toDateTime().plus(Period.hours(2)));
			
			Set<Identity<Long>> groupSet = new LinkedHashSet<Identity<Long>>(2);
			switch ( i % 3 ) {
			case 0:
				groupSet.add(participantGroups.get(0));
				break;
				
			case 1:
				groupSet.add(participantGroups.get(1));
				break;
				
			case 2:
				groupSet.add(participantGroups.get(0));
				groupSet.add(participantGroups.get(1));
				break;
			}
			EventParticipants ep = new EventParticipants(counter.decrementAndGet(), event.getId(), null, groupSet);
			uniEventParticipants.put(event.getId(), ep);
			
			
			
			// give each event a load shed target of 1kW
			EventTargets et = new EventTargets(counter.decrementAndGet(), eventRule.getId(), 
					new TreeSet<EventTarget>(Arrays.asList(new EventTarget(Duration.ZERO, 1000D))));
			
			addEvent(event, ep, et);
			mdt.addWeeks(1);
		}
	}

	@Override
	public List<CapabilityInformation> getAllParticipantGroups(
			List<SortDescriptor> sortDescriptor) {
		return Collections.unmodifiableList(participantGroups);
	}

	@Override
	public Set<CapabilityInformation> getParticipantGroupMembers(Long groupId,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		Set<CapabilityInformation> members = participantGroupMemebers.get(groupId);
		if ( members == null ) {
			throw new SecurityException("Access denied for Group " +groupId);
		}
		return members;
	}

	@Override
	public List<Program> getAllPrograms(List<SortDescriptor> sortDescriptors) {
		List<Program> programs = new ArrayList<Program>(1);
		programs.add(uniProgram);
		return Collections.unmodifiableList(programs);
	}

	@Override
	public Program getProgram(Long programId) {
		if ( !uniProgram.getId().equals(programId) ) {
			throw new SecurityException("Access denied for Program " +programId);
		}
		return uniProgram;
	}

	@Override
	public Set<CapabilityInformation> getProgramParticipants(Program program,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		Set<CapabilityInformation> results = new LinkedHashSet<CapabilityInformation>(
				uniProgramParticipants.size());
		for ( Identity<Long> participant : uniProgramParticipants ) {
			SolarNode node = (SolarNode)participant;
			SimpleCapabilityInformation info = new SimpleCapabilityInformation();
			info.setId(participant.getId());
			info.setLocation(uniLocations.get(node.getLocationId()));
			info.setGenerationCapacityWatts(participantGenerationCapacity);
			info.setStorageCapacityWattHours(participantStorageCapacity);
			results.add(info);
		}
		return Collections.unmodifiableSet(results);
	}

	@Override
	public List<Event> getEvents(Program program,
			ObjectCriteria<EventFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return Collections.unmodifiableList(uniEvents);
	}

	@Override
	public Event getEvent(Long eventId) {
		for ( Event event : uniEvents ) {
			if ( event.getId().equals(eventId) ) {
				return event;
			}
		}
		throw new SecurityException("Access denied for Event " +eventId);
	}

	@Override
	public EventParticipants getCurrentEventParticipants(Event event) {
		return uniEventParticipants.get(event.getId());
	}

	@Override
	public Set<CapabilityInformation> getEventParticipants(Event event,
			ObjectCriteria<ParticipantFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		EventParticipants ep = getCurrentEventParticipants(event);	
		Set<CapabilityInformation> results = new LinkedHashSet<CapabilityInformation>(numNodes);
		if ( ep.getParticipants() != null ) {
			for ( final Identity<Long> participant : ep.getParticipants() ) {
				SolarNode node = (SolarNode)participant;
				SimpleCapabilityInformation info = new SimpleCapabilityInformation();
				info.setId(participant.getId());
				info.setLocation(uniLocations.get(node.getLocationId()));
				info.setGenerationCapacityWatts(participantGenerationCapacity);
				info.setStorageCapacityWattHours(participantStorageCapacity);
				results.add(info);
			}
		}
		if ( ep.getGroups() != null ) {
			for ( final Identity<Long> group : ep.getGroups() ) {
				Set<CapabilityInformation> members = getParticipantGroupMembers(group.getId(), null, null);
				results.addAll(members);
			}
		}
		return Collections.unmodifiableSet(results);
	}

	@Override
	public EventTargets getEventTargets(Event event) {
		return uniEventTargets.get(event.getId());
	}

}
