/* ==================================================================
 * StaleFluxDatum.java - 9/11/2020 7:23:52 pm
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

import java.util.UUID;
import net.solarnetwork.central.domain.Aggregation;

/**
 * API for a "stale" SolarFlux datum record that represents a stream at a
 * specific aggregation level that needs to be published to SolarFlux.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface StaleFluxDatum {

	/**
	 * Get the unique ID of the stream that is stale.
	 * 
	 * @return the stream ID
	 */
	UUID getStreamId();

	/**
	 * Get the type of aggregation that is stale.
	 * 
	 * @return the kind
	 */
	Aggregation getKind();

}
