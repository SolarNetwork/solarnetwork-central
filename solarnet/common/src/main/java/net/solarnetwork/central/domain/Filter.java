/* ==================================================================
 * Filter.java - Aug 8, 2010 8:21:42 PM
 * 
 * Copyright 2007-2010 SolarNetwork.net Dev Team
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
 * $Revision$
 * ==================================================================
 */

package net.solarnetwork.central.domain;

import java.util.Map;

/**
 * API for a "filter-able" object, that is something that can be searched
 * for by specifying filter keys and associated match values.
 * 
 * @author matt
 * @version $Revision$
 */
public interface Filter {

	/**
	 * Get a mapping of filter keys and associated filter values.
	 * 
	 * @return a filter map
	 */
	Map<String, ?> getFilter();
	
}
