/* ==================================================================
 * UserSecret.java - 22/03/2025 8:35:06 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.domain.UserIdRelated;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.domain.Identity;

/**
 * API for a user secret.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserSecret extends Identity<UserStringStringCompositePK>, UserIdRelated {

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	@Override
	Long getUserId();

	/**
	 * Get the topic ID.
	 * 
	 * @return the topic ID
	 */
	String getTopicId();

	/**
	 * Get the key.
	 * 
	 * @return the key
	 */
	String getKey();

	/**
	 * Get the creation date.
	 * 
	 * @return the creation date
	 */
	Instant getCreated();

	/**
	 * Get the modification date.
	 * 
	 * @return the modification date
	 */
	Instant getModified();

	/**
	 * Get the secret value.
	 * 
	 * @return the secret value
	 */
	byte[] secret();

}
