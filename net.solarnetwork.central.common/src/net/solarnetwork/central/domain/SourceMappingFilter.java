/* ==================================================================
 * SourceMappingFilter.java - 25/05/2018 11:42:10 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.central.domain;

import java.util.Map;
import java.util.Set;

/**
 * Extension of {@link Filter} for mapping source IDs into virtual IDs.
 * 
 * @author matt
 * @version 1.0
 * @sicne 1.39
 */
public interface SourceMappingFilter extends Filter {

	/**
	 * Get a map whose keys represent virtual source ID values for the
	 * associated value's set of real source IDs.
	 * 
	 * <p>
	 * This mapping provides a way to request a set of source IDs be treated as
	 * a single logical virtual source ID. For example a set of source IDs for
	 * data collected from PV inverters could be treated as a single virtual
	 * source ID.
	 * <p>
	 * 
	 * @return the mapping of virtual source IDs to the set of real source IDs
	 *         that should be mapped to them
	 */
	Map<String, Set<String>> getSourceIdMappings();

}
