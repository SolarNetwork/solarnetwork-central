/* ==================================================================
 * CapableParticipantGroup.java - Jun 14, 2011 2:35:07 PM
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

package net.solarnetwork.central.dras.support;

import net.solarnetwork.central.dras.domain.CapableObject;
import net.solarnetwork.central.dras.domain.ParticipantGroup;

/**
 * CapableObject implementation for ParticipantGroup.
 * 
 * @author matt
 * @version $Revision$
 */
public class CapableParticipantGroup extends CapableObject<ParticipantGroup> {

	private static final long serialVersionUID = 3062085485443135615L;

	/**
	 * Default constructor.
	 */
	public CapableParticipantGroup() {
		setObject(new ParticipantGroup());
	}

	public ParticipantGroup getParticipantGroup() {
		return super.getObject();
	}
	public void setParticipantGroup(ParticipantGroup object) {
		super.setObject(object);
	}
	
}
