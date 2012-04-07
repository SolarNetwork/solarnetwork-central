/* ==================================================================
 * ProgramParticipants.java - Apr 30, 2011 10:18:16 AM
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
import java.util.Set;

import org.joda.time.DateTime;

import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.domain.NodeIdentity;

/**
 * A set of participants within a Program.
 * 
 * @author matt
 * @version $Revision$
 */
public class ProgramParticipants extends BaseEntity implements Cloneable, Serializable {
	
	private static final long serialVersionUID = 1581964704301749726L;

	private Long programId;
	private Set<NodeIdentity> participants;
	
	/**
	 * Default constructor.
	 */
	public ProgramParticipants() {
		super();
	}
	
	/**
	 * Construct with values.
	 * 
	 * @param programId the program ID
	 * @param participants the participants
	 */
	public ProgramParticipants(Long programId, Set<NodeIdentity> participants) {
		super();
		setProgramId(programId);
		setParticipants(participants);
		setCreated(new DateTime());
	}
	
	/**
	 * @return the programId
	 */
	public Long getProgramId() {
		return programId;
	}

	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(Long programId) {
		this.programId = programId;
	}

	/**
	 * @return the participants
	 */
	public Set<NodeIdentity> getParticipants() {
		return participants;
	}
	
	/**
	 * @param participants the participants to set
	 */
	public void setParticipants(Set<NodeIdentity> participants) {
		this.participants = participants;
	}

}
