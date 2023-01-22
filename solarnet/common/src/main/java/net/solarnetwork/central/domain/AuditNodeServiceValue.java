/* ==================================================================
 * AuditNodeServiceValue.java - 22/01/2023 11:55:13 am
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
import net.solarnetwork.domain.datum.DatumId;

/**
 * API for a node service audit record count.
 * 
 * <p>
 * The audit service name is saved as the {@code sourceId} of the
 * {@link DatumId} identify.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface AuditNodeServiceValue extends Identity<DatumId> {

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
		DatumId id = getId();
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
		DatumId id = getId();
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
		DatumId id = getId();
		return id != null ? id.getTimestamp() : null;
	}

	/**
	 * Get time period associated with this audit value.
	 * 
	 * @return the aggregation
	 */
	Aggregation getAggregation();

	/**
	 * Get the count value properties.
	 * 
	 * @return the count
	 */
	long getCount();

}
