/* ==================================================================
 * AuditDatumBiz.java - 12/07/2018 4:15:17 PM
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

import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * API for accessing audit datum data.
 * 
 * @author matt
 * @version 2.1
 * @since 1.27
 */
public interface AuditDatumBiz {

	/**
	 * Find audit record counts for a given search criteria.
	 * 
	 * <p>
	 * These audit records represent counts of events that happened within the
	 * time period associated with each record. You can request hourly, daily,
	 * and monthly level records by specifying the associated
	 * {@link Aggregation} on {@code filter}. Thus each returned record
	 * represents one hour, day, or month worth of events.
	 * </p>
	 * 
	 * <p>
	 * A single {@code userId} value <b>must</b> be provided on {@code filter}.
	 * The {@code nodeIds} and {@code sourceIds} properties can be used to
	 * restrict the results further. The {@code startDate} and {@code endDate}
	 * properties can be used to restrict the range of time of the results.
	 * </p>
	 * 
	 * <p>
	 * You can request a roll-up of the results by specifying one or more
	 * {@link DatumRollupType} on {@code filter}. For example to roll-up to
	 * nodes by combining sources, provide the {@link DatumRollupType#Time} and
	 * {@link DatumRollupType#Node} types. The order of these values is
	 * significant, and generally follows the semantics of a SQL
	 * {@literal GROUP BY} clause.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the matching records
	 * @since 1.1
	 */
	FilterResults<AuditDatumRollup, DatumPK> findAuditDatumFiltered(AuditDatumCriteria filter);

	/**
	 * Find accumulative audit record counts for a given search criteria.
	 * 
	 * <p>
	 * These audit records represent the total accumulation of events that have
	 * occurred <i>up to and including</i> the time period associated with each
	 * record. Only daily level records are available. Thus each returned record
	 * represents all events that have transpired through the end of the day in
	 * the record.
	 * </p>
	 * 
	 * <p>
	 * A single {@code userId} value <b>must</b> be provided on {@code filter}.
	 * The {@code nodeIds} and {@code sourceIds} properties can be used to
	 * restrict the results further. The {@code startDate} and {@code endDate}
	 * properties can be used to restrict the range of time of the results.
	 * </p>
	 * 
	 * <p>
	 * You can request a roll-up of the results by specifying one or more
	 * {@link DatumRollupType} on {@code filter}. For example to roll-up to
	 * nodes by combining sources, provide the {@link DatumRollupType#Time} and
	 * {@link DatumRollupType#Node} types. The order of these values is
	 * significant, and generally follows the semantics of a SQL
	 * {@literal GROUP BY} clause.
	 * </p>
	 * 
	 * <p>
	 * The {@code mostRecent} flag on {@code filter} can be used to find the
	 * most recent record for each unique node and source combination.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @return the matching records
	 * @since 1.1
	 */
	FilterResults<AuditDatumRollup, DatumPK> findAccumulativeAuditDatumFiltered(
			AuditDatumCriteria filter);

}
