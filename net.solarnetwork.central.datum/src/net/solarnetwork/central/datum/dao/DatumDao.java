/* ===================================================================
 * DatumDao.java
 * 
 * Created Jul 29, 2009 11:06:58 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.datum.dao;

import java.util.List;
import java.util.Set;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import org.joda.time.LocalDate;
import org.joda.time.ReadableDateTime;
import org.joda.time.ReadableInterval;

/**
 * Generic DAO API providing standardized data access methods.
 *
 * @author matt
 * @version $Revision$ $Date$
 * @param <T> the domain object type
 */
public interface DatumDao<T extends Datum> extends GenericDao<T, Long>{
	
	/**
	 * Get the class supported by this Dao.
	 * 
	 * @return class
	 */
	Class<? extends T> getDatumType();

	/**
	 * Get a datum by its primary key.
	 * 
	 * @param id the primary key
	 * @return the datum, or <em>null</em> if not found
	 */
	T getDatum(Long id);
	
	/**
	 * Store (create or update) a datum and return it's primary key.
	 * 
	 * @param datum the datum to persist
	 * @return the generated primary key
	 */
	Long storeDatum(T datum);
	
	/**
	 * Get a datum by an ID and specific date.
	 * 
	 * <p>The meaning of ID can vary between datum types. For example
	 * in some cases it might be a node ID, in others a location ID.</p>
	 * 
	 * @param id an ID
	 * @param date the date
	 * @return the Datum, or <em>null</em> if not found
	 */
	T getDatumForDate(Long id, ReadableDateTime date);

	/**
	 * Query for a list of aggregated datum objects.
	 * 
	 * <p>The returned domain objects are not generally persisted objects,
	 * they represent aggregated results, most likely aggregated over time.</p>
	 * 
	 * @param criteria the query criteria
	 * @return the query results
	 */
	List<T> getAggregatedDatum(DatumQueryCommand criteria);
	
	/**
	 * Get the most-recently created Datum up to (and including) 
	 * a specific {@link DatumQueryCommand#getEndDate()} value.
	 * 
	 * <p>The node ID, location ID, and source ID properties of
	 * {@link DatumQueryCommand} can be used to filter the results. 
	 * The {@code endDate} property must be provided as the date to
	 * find the most recent data for.</p>
	 * 
	 * @param criteria the query criteria
	 * @return the Datum, or <em>null</em> if not found
	 */
	List<T> getMostRecentDatum(DatumQueryCommand criteria);
	
	/**
	 * Get the interval of available data in the system.
	 * 
	 * @param nodeId the node ID to search for
	 * @return interval, or <em>null</em> if no data available
	 */
	ReadableInterval getReportableInterval(Long nodeId);
	
	/**
	 * Get the interval of available data in the system, across all nodes.
	 * 
	 * @return interval, or <em>null</em> if no data available
	 */
	ReadableInterval getReportableInterval();
	
	/**
	 * Get the available sources for a given node, optionally limited to a date range.
	 * 
	 * @param nodeId the node ID to search for
	 * @param start an optional start date (inclusive) to filter on
	 * @param end an optional end date (inclusive) to filter on
	 * @return the distinct source IDs available (never <em>null</em>)
	 */
	Set<String> getAvailableSources(Long nodeId, LocalDate start, LocalDate end);
	
}
