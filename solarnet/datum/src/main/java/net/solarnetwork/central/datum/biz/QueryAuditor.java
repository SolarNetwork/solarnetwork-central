/* ==================================================================
 * QueryAuditor.java - 14/02/2018 7:34:37 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.biz;

import java.time.Clock;
import java.util.Map;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.domain.FilterMatch;
import net.solarnetwork.central.domain.FilterResults;

/**
 * API for auditing query events in SolarNetwork.
 * 
 * @author matt
 * @version 1.2
 */
public interface QueryAuditor {

	/**
	 * Get the clock used for auditing.
	 * 
	 * <p>
	 * This clock may bucket time into discreet intervals.
	 * </p>
	 * 
	 * @return the clock never {@literal null}
	 * @since 1.2
	 */
	Clock getAuditClock();

	/**
	 * Audit the results of a general node datum query.
	 * 
	 * @param <T>
	 *        the match type
	 * @param filter
	 *        the criteria used for the query
	 * @param results
	 *        the query results
	 */
	<T extends FilterMatch<GeneralNodeDatumPK>> void auditNodeDatumFilterResults(
			GeneralNodeDatumFilter filter, FilterResults<T> results);

	/**
	 * Get the current audit results map.
	 * 
	 * <p>
	 * The "current" results is defined as a thread-local copy of the audit
	 * results processed via previous calls to
	 * {@link #auditNodeDatumFilterResults(GeneralNodeDatumFilter, FilterResults)}.
	 * The counts accumulate until {@link #resetCurrentAuditResults()} is
	 * called. Because a variety of services might be interested in these audit
	 * results, they are provided this way as they would be otherwise difficult
	 * to pass back to those interested services.
	 * </p>
	 * 
	 * <p>
	 * It is expected that during a single user-initiated transaction, such as a
	 * web request, the {@link #resetCurrentAuditResults()} method is called at
	 * the start and then this method is used before passing the results back to
	 * the user to find out the audit results.
	 * </p>
	 * 
	 * 
	 * @return the map, never {@literal null}
	 * @since 1.1
	 */
	Map<GeneralNodeDatumPK, Integer> currentAuditResults();

	/**
	 * Reset the current audit results map.
	 * 
	 * @since 1.1
	 * @see #currentAuditResults()
	 */
	void resetCurrentAuditResults();

	/**
	 * Add audit results.
	 * 
	 * <p>
	 * These results presumably came from a previous call to
	 * {@link #currentAuditResults()} and are being added again because of some
	 * reprocessing occurred that resulted in the same results. This might
	 * happen, for example, if query results are cached, and the cached results
	 * are returned.
	 * </p>
	 * 
	 * @param results
	 *        the results to add
	 * @since 1.1
	 */
	void addNodeDatumAuditResults(Map<GeneralNodeDatumPK, Integer> results);

}
