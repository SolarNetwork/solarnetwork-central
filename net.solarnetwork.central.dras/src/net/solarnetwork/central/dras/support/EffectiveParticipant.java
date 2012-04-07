/* ==================================================================
 * EffectiveParticipant.java - Jun 14, 2011 3:52:13 PM
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

import net.solarnetwork.central.dras.domain.Effective;
import net.solarnetwork.central.dras.domain.EffectiveObject;
import net.solarnetwork.central.dras.domain.Participant;

/**
 * EffectiveObject for a Participant.
 * 
 * @author matt
 * @version $Revision$
 */
public final class EffectiveParticipant extends EffectiveObject<Participant> {

	private static final long serialVersionUID = 5928820364297702366L;

	/**
	 * Default constructor.
	 */
	public EffectiveParticipant() {
		super(new Effective(), new Participant());
	}
	
	@Override
	public Participant getObject() {
		return super.getObject();
	}

	@Override
	public void setObject(Participant object) {
		super.setObject(object);
	}
	
}
