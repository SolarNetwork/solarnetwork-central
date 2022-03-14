/* ==================================================================
 * NodeMetadata.java - 13/11/2016 12:05:32 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for node metadata.
 * 
 * @author matt
 * @version 2.0
 */
public interface NodeMetadata {

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	Long getNodeId();

	/**
	 * Get the creation date.
	 * 
	 * @return the creation date
	 */
	Instant getCreated();

	/**
	 * Get the updated date.
	 * 
	 * @return the updated date
	 */
	Instant getUpdated();

	/**
	 * Get the metadata.
	 * 
	 * @return the metadata
	 */
	GeneralDatumMetadata getMetadata();

}
