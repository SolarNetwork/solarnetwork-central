/* ==================================================================
 * FilteredResultsProcessor.java - 1/05/2022 2:14:43 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Map;

/**
 * API for a service that can directly handle filtered results, in a streaming
 * fashion.
 * 
 * @param <R>
 *        the result item type
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public interface FilteredResultsProcessor<R> extends Closeable, Flushable {

	/**
	 * Called at the start of the export process, to initialize any necessary
	 * resources or write any header information.
	 * 
	 * @param totalResultCount
	 *        the total number of results that match the given query, or
	 *        {@literal null} if the count is not known
	 * @param startingOffset
	 *        a starting offset within the total result count, or
	 *        {@literal null} if not known
	 * @param expectedResultCount
	 *        the expected number of results to be returned to
	 *        {@link #handleResultItem(Object)}
	 * @param attributes
	 *        optional implementation-specific attributes to pass to the
	 *        processor
	 * @throws IOException
	 *         if an IO error occurs
	 */
	void start(Long totalResultCount, Integer startingOffset, Integer expectedResultCount,
			Map<String, ?> attributes) throws IOException;

	/**
	 * Process a result item.
	 * 
	 * <p>
	 * This method should only be called after
	 * {@link #start(Long, Integer, Integer)} has been called.
	 * </p>
	 * 
	 * @param resultItem
	 *        the result item to process
	 * @throws IOException
	 *         if an IO error occurs
	 */
	void handleResultItem(R resultItem) throws IOException;

}
