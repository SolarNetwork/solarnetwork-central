/* ==================================================================
 * EventAdminBiz.java - Jun 15, 2011 6:33:46 PM
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

package net.solarnetwork.central.dras.biz;

import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.support.MembershipCommand;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Event administrator API.
 * 
 * @author matt
 * @version $Revision$
 */
@PreAuthorize("hasRole('ROLE_OPERATOR')")
public interface EventAdminBiz {

	/**
	 * Create or update a new Event.
	 * 
	 * @param template the program template
	 * @return the persisted Event instance
	 */
	Event storeEvent(Event template);

	/**
	 * Manage the participants of a program.
	 * 
	 * @param eventId the event ID to assign members to
	 * @param participants the participant membership
	 * @param participantGroups the participant group membership
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<Event,Member> assignMembers(Long eventId,
			MembershipCommand participants,
			MembershipCommand participantGroups);
}
