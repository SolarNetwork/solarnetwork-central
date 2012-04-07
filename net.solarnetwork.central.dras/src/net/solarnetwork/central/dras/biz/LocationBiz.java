/* ==================================================================
 * LocationBiz.java - Jun 11, 2011 10:16:41 AM
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

import net.solarnetwork.central.dao.ObjectCriteria;
import net.solarnetwork.central.dao.SortDescriptor;
import net.solarnetwork.central.dras.dao.LocationFilter;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.central.dras.domain.Match;

/**
 * Location observer API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface LocationBiz {

	/**
	 * Get a single Location by its ID.
	 * 
	 * @param locationId the ID of the location to get
	 * @return the Location, or <em>null</em> if not found
	 */
	Location getLocation(Long locationId);
	
	/**
	 * Find a set of Location objects, optionally filtered by a search criteria and optionally
	 * sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Location} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of locations, or an empty set if none found
	 */
	List<Match> findLocations(ObjectCriteria<LocationFilter> criteria, 
			List<SortDescriptor> sortDescriptors);

}
