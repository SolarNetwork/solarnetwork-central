/* ==================================================================
 * SecurityUser.java - Nov 22, 2012 8:57:58 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security;

/**
 * API for user details.
 * 
 * @author matt
 * @version 1.0
 */
public interface SecurityUser {

	/**
	 * Get a friendly display name.
	 * 
	 * @return display name
	 */
	String getDisplayName();

	/**
	 * Get the email used to authenticate the user with.
	 * 
	 * @return email
	 */
	String getEmail();

	/**
	 * Get a unique user ID.
	 * 
	 * @return the user ID
	 */
	Long getUserId();

}
