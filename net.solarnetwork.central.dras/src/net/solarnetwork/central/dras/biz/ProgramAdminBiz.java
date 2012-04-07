/* ==================================================================
 * ProgramAdminBiz.java - Jun 11, 2011 7:08:50 PM
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

import java.util.List;

import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.support.MembershipCommand;

/**
 * Program administration API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ProgramAdminBiz {

	/**
	 * Create or update a new Program.
	 * 
	 * @param template the program template
	 * @return the persisted Program instance
	 */
	Program storeProgram(Program template);

	/**
	 * Manage the participants of a program.
	 * 
	 * @param membership the membership
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<Program, Member> assignParticipantMembers(
			MembershipCommand membership);
	
	/**
	 * Store constraints for a specific program.
	 * 
	 * <p>These constraints serve as defaults for all participants in the 
	 * given program.</p>
	 * 
	 * @param programId the program ID to save the constraints with
	 * @param constraints the list of constraints
	 * @return the EffectiveCollection
	 */
	EffectiveCollection<Program, Constraint> storeProgramConstraints(Long programId, 
			List<Constraint> constraints);
	
}
