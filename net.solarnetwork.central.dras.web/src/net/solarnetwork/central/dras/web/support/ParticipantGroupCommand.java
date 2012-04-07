/* ==================================================================
 * ParticipantCommand.java - Jun 12, 2011 3:41:41 PM
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

import net.solarnetwork.central.dras.domain.ParticipantGroup;

/**
 * Command object for ParticipantGroup updates.
 * 
 * @author matt
 * @version $Revision$
 */
public class ParticipantGroupCommand {

	@NotNull
	private ParticipantGroup participantGroup;

	public ParticipantGroup getParticipantGroup() {
		return participantGroup;
	}
	public void setParticipantGroup(ParticipantGroup participantGroup) {
		this.participantGroup = participantGroup;
	}
	
}
