/* ==================================================================
 * DelegatingProgramAdminBiz.java - Jun 23, 2011 5:23:25 PM
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

package net.solarnetwork.central.dras.biz.support;

import java.util.List;

import net.solarnetwork.central.dras.biz.ProgramAdminBiz;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.support.MembershipCommand;

/**
 * Delegating {@link ProgramAdminBiz}, to support AOP with OSGi services.
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingProgramAdminBiz implements ProgramAdminBiz {

	private ProgramAdminBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate the delgate
	 */
	public DelegatingProgramAdminBiz(ProgramAdminBiz delegate) {
		super();
		this.delegate = delegate;
	}
	
	@Override
	public Program storeProgram(Program template) {
		return delegate.storeProgram(template);
	}

	@Override
	public EffectiveCollection<Program, Member> assignParticipantMembers(
			MembershipCommand membership) {
		return delegate.assignParticipantMembers(membership);
	}

	@Override
	public EffectiveCollection<Program, Constraint> storeProgramConstraints(
			Long programId, List<Constraint> constraints) {
		return delegate.storeProgramConstraints(programId, constraints);
	}
	
}
