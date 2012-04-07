/* ==================================================================
 * EventExecutionTargets.java - Jun 27, 2011 8:03:19 PM
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

import net.solarnetwork.central.domain.BaseIdentity;

/**
 * Event execution EventTargets information.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventExecutionTargets extends BaseIdentity implements Serializable, Member {

	private static final long serialVersionUID = -155626546190139992L;

	private EventRule.RuleKind eventRuleKind;
	private String eventRuleName;
	private SortedSet<EventTarget> targets;
	
	public String getEventRuleName() {
		return eventRuleName;
	}
	public void setEventRuleName(String eventRuleName) {
		this.eventRuleName = eventRuleName;
	}
	public SortedSet<EventTarget> getTargets() {
		return targets;
	}
	public void setTargets(SortedSet<EventTarget> targets) {
		this.targets = targets;
	}
	public EventRule.RuleKind getEventRuleKind() {
		return eventRuleKind;
	}
	public void setEventRuleKind(EventRule.RuleKind eventRuleKind) {
		this.eventRuleKind = eventRuleKind;
	}

}
