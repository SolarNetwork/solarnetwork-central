/* ==================================================================
 * DelegatingProgramBiz.java - Jun 23, 2011 8:06:05 PM
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
import java.util.Set;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.biz.ProgramBiz;
import net.solarnetwork.central.dras.dao.ProgramFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Program;

/**
 * Delegating {@link ProgramBiz}, to support AOP with OSGi services.
 * 
 * @author matt
 * @version $Revision$
 */
public class DelegatingProgramBiz implements ProgramBiz {

	private ProgramBiz delegate;
	
	/**
	 * Constructor.
	 * 
	 * @param delegate the delegate
	 */
	public DelegatingProgramBiz(ProgramBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public List<Match> findPrograms(ObjectCriteria<ProgramFilter> criteria,
			List<SortDescriptor> sortDescriptors) {
		return delegate.findPrograms(criteria, sortDescriptors);
	}

	@Override
	public Program getProgram(Long programId) {
		return delegate.getProgram(programId);
	}

	@Override
	public Set<Constraint> getProgramConstraints(Long programId) {
		return delegate.getProgramConstraints(programId);
	}
	
}
