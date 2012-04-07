/* ==================================================================
 * ParticipantGroup.java - Jun 2, 2011 8:56:54 PM
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

import net.solarnetwork.central.domain.SolarNodeGroup;

/**
 * Extension of {link SolarNodeGroup} with DRAS support.
 * 
 * @author matt
 * @version $Revision$
 */
public class ParticipantGroup extends SolarNodeGroup 
implements Member, Match, Cloneable, Serializable {

	private static final long serialVersionUID = 2853734825588666018L;

	private Long creator;
	private Boolean confirmed;
	private Long verificationMethodId;
	private Boolean enabled;
	private Capability capability;
	
	/**
	 * Default constructor.
	 */
	public ParticipantGroup() {
		super();
	}
	
	/**
	 * Construct with ID.
	 * @param id the ID
	 */
	public ParticipantGroup(Long id) {
		super();
		setId(id);
	}
	
	public Long getCreator() {
		return creator;
	}
	public void setCreator(Long creator) {
		this.creator = creator;
	}
	public Boolean getConfirmed() {
		return confirmed;
	}
	public void setConfirmed(Boolean confirmed) {
		this.confirmed = confirmed;
	}
	public Long getVerificationMethodId() {
		return verificationMethodId;
	}
	public void setVerificationMethodId(Long verificationMethodId) {
		this.verificationMethodId = verificationMethodId;
	}
	public Boolean getEnabled() {
		return enabled;
	}
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	public Capability getCapability() {
		return capability;
	}
	public void setCapability(Capability capability) {
		this.capability = capability;
	}

}
