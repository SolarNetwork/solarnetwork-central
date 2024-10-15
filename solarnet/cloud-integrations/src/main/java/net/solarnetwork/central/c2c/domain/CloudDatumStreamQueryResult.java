/* ==================================================================
 * CloudDatumStreamQueryResult.java - 15/10/2024 1:50:57 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.domain;

import java.util.SequencedCollection;
import net.solarnetwork.domain.datum.Datum;

/**
 * Cloud datum stream query results API.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamQueryResult extends Iterable<Datum> {

	/**
	 * Test if there are no results available.
	 *
	 * @return {@literal true} if there are no results
	 */
	default boolean isEmpty() {
		return (getReturnedResultCount() == 0);
	}

	/**
	 * Get the returned result count.
	 *
	 * <p>
	 * This is an alias of {@link #getReturnedResultCount()}.
	 * </p>
	 *
	 * @return the number of returned results
	 */
	default int size() {
		return getReturnedResultCount();
	}

	/**
	 * Get the number of results included in {@link #getResults()}.
	 *
	 * @return the number of returned results
	 */
	default int getReturnedResultCount() {
		final var results = getResults();
		return (results != null ? results.size() : 0);
	}

	/**
	 * Get a query filter configured to return the next set of results, if any.
	 *
	 * @return a query filter, or {@literal null}
	 */
	default CloudDatumStreamQueryFilter getNextQueryFilter() {
		return null;
	}

	/**
	 * Get the results.
	 *
	 * <p>
	 * These are the same results returned by {@link Iterable#iterator()}.
	 * </p>
	 *
	 * @return the results, never {@code node}
	 */
	SequencedCollection<Datum> getResults();

}
