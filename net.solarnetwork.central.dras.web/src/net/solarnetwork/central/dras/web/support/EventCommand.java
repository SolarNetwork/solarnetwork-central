/* ==================================================================
 * EventCommand.java - Jun 15, 2011 9:17:36 PM
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

package net.solarnetwork.central.dras.web.support;

import javax.validation.constraints.NotNull;

import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.support.MembershipCommand;

import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Command object for Event updates.
 * 
 * @author matt
 * @version $Revision$
 */
public class EventCommand {

	@NotNull
	private Event event = new Event();
	
	private MembershipCommand p;
	private MembershipCommand g;

	public void setNotificationDate(@DateTimeFormat(iso=ISO.DATE_TIME) DateTime date) {
		event.setNotificationDate(date);
	}
	public void setEventDate(@DateTimeFormat(iso=ISO.DATE_TIME) DateTime date) {
		event.setEventDate(date);
	}
	public void setEndDate(@DateTimeFormat(iso=ISO.DATE_TIME) DateTime date) {
		event.setEndDate(date);
	}
	
	public Event getEvent() {
		return event;
	}
	public void setEvent(Event event) {
		this.event = event;
	}
	public MembershipCommand getP() {
		return p;
	}
	public void setP(MembershipCommand p) {
		this.p = p;
	}
	public MembershipCommand getG() {
		return g;
	}
	public void setG(MembershipCommand g) {
		this.g = g;
	}
	
}
