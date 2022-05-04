/* ==================================================================
 * ReadingDatumDao.java - 17/11/2020 7:41:09 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import java.io.IOException;
import java.util.List;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.support.StreamDatumFilteredResultsProcessor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.StreamDatum;

/**
 * API for datum reading information.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public interface ReadingDatumDao {

	/**
	 * Find datum reading for a given search criteria.
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the matching records
	 */
	ObjectDatumStreamFilterResults<ReadingDatum, DatumPK> findDatumReadingFiltered(DatumCriteria filter);

	/**
	 * API for querying for a stream of reading {@link StreamDatum}.
	 * 
	 * <p>
	 * The type of datum instances passed to the given {@code processor} will
	 * depend on the {@link DatumCriteria#getReadingType()} passed in the
	 * {@code filter}. For
	 * {@code net.solarnetwork.central.datum.domain.DatumReadingType#CalculatedAt}
	 * plain {@link net.solarnetwork.central.datum.v2.domain.Datum} objects will
	 * be used. For all others {@link import ReadingDatum} objects will be used.
	 * </p>
	 * 
	 * @param filter
	 *        the filter
	 * @param processor
	 *        the stream processor
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        the optional starting offset
	 * @param max
	 *        the optional maximum result count
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.1
	 */
	void findFilteredStream(DatumCriteria filter, StreamDatumFilteredResultsProcessor processor,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) throws IOException;

}
