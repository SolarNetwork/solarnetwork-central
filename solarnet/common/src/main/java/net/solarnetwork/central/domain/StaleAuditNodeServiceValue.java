/* ==================================================================
 * StaleAuditNodeServiceValue.java - 22/01/2023 2:46:46 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

import java.time.Instant;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * API for a "stale" audit node service value record that represents a period of
 * time for a specific audit level that needs to be computed.
 * 
 * @author matt
 * @version 1.0
 */
public interface StaleAuditNodeServiceValue extends Identity<AggregateDatumId> {

	/**
	 * Get the node ID this audit value is part of.
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getObjectId()}.
	 * </p>
	 * 
	 * @return the node ID
	 */
	default Long getNodeId() {
		AggregateDatumId id = getId();
		return id != null ? id.getObjectId() : null;
	}

	/**
	 * Get the service name this audit value is a part of.
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getSourceId()}.
	 * </p>
	 * 
	 * @return the service
	 */
	default String getService() {
		AggregateDatumId id = getId();
		return id != null ? id.getSourceId() : null;
	}

	/**
	 * Get the associated timestamp of this audit value.
	 * 
	 * <p>
	 * This value represents the point in time the count associated with this
	 * service were observed, collected, inferred, predicted, etc.
	 * </p>
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getTimestamp()}.
	 * </p>
	 * 
	 * @return the timestamp for this datum
	 */
	default Instant getTimestamp() {
		AggregateDatumId id = getId();
		return id != null ? id.getTimestamp() : null;
	}

	/**
	 * Get time period associated with this audit value.
	 * 
	 * <p>
	 * This is a shortcut for {@code getId().getAggregation()}.
	 * </p>
	 * 
	 * @return the aggregation
	 */
	default Aggregation getAggregation() {
		AggregateDatumId id = getId();
		return id != null ? id.getAggregation() : null;
	}

}
