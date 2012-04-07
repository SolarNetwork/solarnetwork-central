/* ==================================================================
 * BasicFilterResults.java - Feb 13, 2012 3:42:02 PM
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

package net.solarnetwork.central.support;

import net.solarnetwork.central.domain.FilterResults;

/**
 * Basic implementation of {@link FilterResults}.
 * 
 * @param T the result type
 * @author matt
 * @version $Revision$
 */
public class BasicFilterResults<T> implements FilterResults<T> {

	private Iterable<T> results;
	private Long totalResults;
	private Integer startingOffset;
	private Integer returnedResultCount;
	
	public BasicFilterResults(Iterable<T> results, Long totalResults, 
			Integer startingOffset, Integer returnedResultCount) {
		super();
		this.results = results;
		this.totalResults = totalResults;
		this.startingOffset = startingOffset;
		this.returnedResultCount = returnedResultCount;
	}
	
	public BasicFilterResults(Iterable<T> results) {
		this(results, null, null, null);
	}
	
	@Override
	public Iterable<T> getResults() {
		return results;
	}

	@Override
	public Long getTotalResults() {
		return totalResults;
	}

	@Override
	public Integer getStartingOffset() {
		return startingOffset;
	}

	@Override
	public Integer getReturnedResultCount() {
		return returnedResultCount;
	}

}
