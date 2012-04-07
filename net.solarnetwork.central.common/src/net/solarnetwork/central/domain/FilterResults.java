/* ==================================================================
 * FilterResults.java - Feb 13, 2012 3:34:57 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

/**
 * A filtered query results object.
 * 
 * @author matt
 * @version $Revision$
 */
public interface FilterResults<T> {

	/**
	 * Get the actual results.
	 * @return the results, never <em>null</em>
	 */
	Iterable<T> getResults();
	
	/**
	 * If available, a total number of results.
	 * @return total results
	 */
	Long getTotalResults();
	
	/**
	 * Get the starting offset of the returned results.
	 * 
	 * @return the starting offset, never <em>null</em>
	 */
	Integer getStartingOffset();
	
	/**
	 * Get the number of results that matched the query.
	 * 
	 * @return the number of returned results, never <em>null</em>
	 */
	Integer getReturnedResultCount();
	
}
