/* ==================================================================
 * StaleAggregateDatum.java - 7/11/2020 7:25:36 am
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

package net.solarnetwork.central.datum.v2.domain;

import java.time.Instant;
import java.util.UUID;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * API for a "stale" audit datum record that represents a period of time for a
 * specific audit level that needs to be computed.
 * 
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public interface StaleAuditDatum extends Identity<StreamKindPK> {

	/**
	 * Get the unique ID of the stream that is stale..
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getStreamId()}.
	 * </p>
	 * 
	 * @return the stream ID
	 */
	UUID getStreamId();

	/**
	 * Get the associated timestamp of this datum.
	 * 
	 * <p>
	 * This value represents the starting point in time stream is stale.
	 * </p>
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 * 
	 * @return the timestamp for this datum
	 */
	Instant getTimestamp();

	/**
	 * Get the type of aggregation that is stale.
	 * 
	 * <p>
	 * This value represents the time period in the stream that is stale. Thus
	 * the overall stale period is from {@code timestamp} to
	 * {@code timestamp + kin}.
	 * </p>
	 * 
	 * <p>
	 * This is a shortcut for {@code Aggregation.forKey(getId().getKind())}.
	 * </p>
	 * 
	 * @return the kind
	 */
	Aggregation getKind();

}
