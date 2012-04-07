/* ==================================================================
 * EventBiz.java - Jun 14, 2011 6:19:19 PM
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
import net.solarnetwork.central.dras.dao.EventFilter;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Match;

/**
 * Event observer API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface EventBiz {

	/**
	 * Get a single Event by its ID.
	 * 
	 * @param eventId the ID of the event to get
	 * @return the Event
	 */
	Event getEvent(Long eventId);

	/**
	 * Find events, optionally sorted in some way.
	 * 
	 * <p>If specific ordering is required, the {@code sortDescriptors} parameter
	 * can be used to sort the results, where sort keys are {@link Event} property names.
	 * If no {@code sortDescriptors} are provided, the results will be sorted by
	 * {@code id}, in an ascending manner.</p>
	 * 
	 * @param criteria an optional search criteria
	 * @param sortDescriptors an optional list of sort descriptors to order the results by
	 * @return set of events, or an empty set if none found
	 */
	List<Match> findEvents(ObjectCriteria<EventFilter> criteria, 
			List<SortDescriptor> sortDescriptors);
	
}
