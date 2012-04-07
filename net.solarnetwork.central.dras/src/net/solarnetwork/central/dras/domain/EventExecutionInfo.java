/* ==================================================================
 * EventExecutionInfo.java - Jun 27, 2011 5:13:05 PM
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
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.dras.biz.EventExecutor;

/**
 * Event execution information.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventExecutionInfo extends BaseEntity 
implements EventExecutor.EventExecutionRequest, Serializable {

	private static final long serialVersionUID = 1899483462178992721L;

	private Event event;
	private String executionKey;
	private DateTime executionDate;
	private List<EventOperationModeTarget> modes = new ArrayList<EventOperationModeTarget>();
	private List<EventExecutionTargets> targets = new ArrayList<EventExecutionTargets>();
	private List<Member> participants = new ArrayList<Member>();
	
	@Override
	public Event getEvent() {
		return event;
	}
	
	@Override
	public List<EventOperationModeTarget> getModes() {
		return modes;
	}

	@Override
	public List<EventExecutionTargets> getTargets() {
		return targets;
	}

	@Override
	public List<Member> getParticipants() {
		return participants;
	}

	public String getExecutionKey() {
		return executionKey;
	}
	public void setExecutionKey(String executionKey) {
		this.executionKey = executionKey;
	}
	public DateTime getExecutionDate() {
		return executionDate;
	}
	public void setExecutionDate(DateTime executionDate) {
		this.executionDate = executionDate;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public void setModes(List<EventOperationModeTarget> modes) {
		this.modes = modes;
	}
	public void setTargets(List<EventExecutionTargets> targets) {
		this.targets = targets;
	}
	public void setParticipants(List<Member> participants) {
		this.participants = participants;
	}
	
}
