/* ==================================================================
 * EventTargets.java - May 9, 2011 3:17:24 PM
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

package net.solarnetwork.central.dras.domain;

import java.io.Serializable;
import java.util.SortedSet;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import net.solarnetwork.central.domain.BaseEntity;

/**
 * Set of event targets for a given event.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventTargets extends BaseEntity implements Cloneable, Serializable, Member {

	private static final long serialVersionUID = 5535356841579121139L;

	private Long eventRuleId;
	private Duration overallDuration;
	private SortedSet<EventTarget> targets;
	
	/**
	 * Default constructor.
	 */
	public EventTargets() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * @param the ID
	 */
	public EventTargets(Long id) {
		super();
		setId(id);
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param id the ID
	 * @param eventId the event ID
	 * @param eventTargets the event targets
	 */
	public EventTargets(Long id, Long eventRuleId, SortedSet<EventTarget> eventTargets) {
		this(id);
		setCreated(new DateTime());
		setEventRuleId(eventRuleId);
		setTargets(eventTargets);
	}

	public SortedSet<EventTarget> getTargets() {
		return targets;
	}
	public void setTargets(SortedSet<EventTarget> targets) {
		this.targets = targets;
	}
	public Long getEventRuleId() {
		return eventRuleId;
	}
	public void setEventRuleId(Long eventRuleId) {
		this.eventRuleId = eventRuleId;
	}
	public Duration getOverallDuration() {
		return overallDuration;
	}
	public void setOverallDuration(Duration overallDuration) {
		this.overallDuration = overallDuration;
	}

}
