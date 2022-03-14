/* ==================================================================
 * GeneralLocationDatumDao.java - Oct 17, 2014 2:20:48 PM
 *
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.dao;

import java.time.Instant;
import java.util.Set;
import net.solarnetwork.central.dao.AggregationFilterableDao;
import net.solarnetwork.central.dao.FilterableDao;
import net.solarnetwork.central.dao.GenericDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatumMatch;
import net.solarnetwork.central.datum.v2.domain.DateInterval;

/**
 * DAO API for {@link GeneralLocationDatum}.
 *
 * @author matt
 * @version 2.0
 */
public interface GeneralLocationDatumDao
		extends GenericDao<GeneralLocationDatum, GeneralLocationDatumPK>,
		FilterableDao<GeneralLocationDatumFilterMatch, GeneralLocationDatumPK, GeneralLocationDatumFilter>,
		AggregationFilterableDao<ReportingGeneralLocationDatumMatch, AggregateGeneralLocationDatumFilter> {

	/**
	 * Get the interval of available data in the system. Note the returned
	 * interval will be configured with the location's local time zone, if
	 * available.
	 *
	 * @param locationId
	 *        the location ID to search for
	 * @param sourceId
	 *        an optional source ID to limit the results to, or <em>null</em>
	 *        for all sources
	 * @return interval, or <em>null</em> if no data available
	 */
	DateInterval getReportableInterval(Long locationId, String sourceId);

	/**
	 * Get the available sources for a given location, optionally limited to a
	 * date range.
	 *
	 * @param locationId
	 *        the location ID to search for
	 * @param start
	 *        an optional start date (inclusive) to filter on
	 * @param end
	 *        an optional end date (inclusive) to filter on
	 * @return the distinct source IDs available (never <em>null</em>)
	 */
	Set<String> getAvailableSources(Long locationId, Instant start, Instant end);

}
