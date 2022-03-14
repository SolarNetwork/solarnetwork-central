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
 */

package net.solarnetwork.central.domain;

/**
 * A filtered query results object.
 * 
 * <p>
 * This object extends {@link Iterable} but also exposes a JavaBean getter
 * property {@link #getResults()} to easy the marshaling of the results into
 * other forms.
 * </p>
 * 
 * @author matt
 * @version 1.1
 */
public interface FilterResults<T> extends Iterable<T> {

	/**
	 * Get the actual results.
	 * 
	 * <p>
	 * These are the same results returned by {@link Iterable#iterator()}.
	 * </p>
	 * 
	 * @return the results, never <em>null</em>
	 */
	Iterable<T> getResults();

	/**
	 * If available, a total number of results.
	 * 
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
