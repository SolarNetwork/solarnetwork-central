/* ==================================================================
 * DatumFilter.java - Dec 2, 2013 5:08:29 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain;

import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;

/**
 * Filter for Datum entities.
 * 
 * @author matt
 * @version 1.2
 */
public interface DatumFilter extends Filter, DateRangeFilter {

	/**
	 * Get a type of datum.
	 * 
	 * @return the datum type
	 */
	String getType();

}
