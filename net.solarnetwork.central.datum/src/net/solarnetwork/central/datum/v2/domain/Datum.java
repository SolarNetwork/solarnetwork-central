/* ==================================================================
 * Datum.java - 22/10/2020 1:59:49 pm
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

/**
 * API for an object that exists within a unique stream at a specific point in
 * time and a set of property values.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface Datum extends Identity<DatumPK> {

	/**
	 * Get the unique ID of the stream this datum is a part of.
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
	 * This value represents the point in time the properties associated with
	 * this datum were observed, collected, inferred, predicted, etc.
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
	 * Get the properties associated with this datum.
	 * 
	 * @return the properties
	 */
	DatumProperties getProperties();

}
