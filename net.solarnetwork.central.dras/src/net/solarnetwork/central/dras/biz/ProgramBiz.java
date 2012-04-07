/* ==================================================================
 * ProgramBiz.java - Jun 8, 2011 4:08:53 PM
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
import java.util.Set;

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.dao.ProgramFilter;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.domain.Program;

/**
 * Program observer API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface ProgramBiz {

	/**
	 * Find programs, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Program} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code name}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of programs, or an empty set if none found
	 */
	List<Match> findPrograms(ObjectCriteria<ProgramFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
	/**
	 * Get a single Program by its ID.
	 * 
	 * @param programId the ID of the program to get
	 * @return the Program
	 */
	Program getProgram(Long programId);
	
	/**
	 * Get the complete set of program constraints.
	 * 
	 * @param programId the program ID to get the constraints for
	 * @return the constraints, never <em>null</em>
	 */
	Set<Constraint> getProgramConstraints(Long programId);
	
}
