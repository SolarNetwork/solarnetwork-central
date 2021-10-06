/* ==================================================================
 * UserMetadata.java - 14/11/2016 11:09:42 AM
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

package net.solarnetwork.central.user.domain;

import java.time.Instant;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * API for user metadata.
 * 
 * @author matt
 * @version 2.0
 */
public interface UserMetadata {

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

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
