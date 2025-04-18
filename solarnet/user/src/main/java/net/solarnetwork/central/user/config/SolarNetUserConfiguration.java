/* ==================================================================
 * UserConfiguration.java - 7/10/2021 10:52:06 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.config;

/**
 * Marker interface for the Instructor configuration package.
 * 
 * @author matt
 * @version 1.2
 */
public interface SolarNetUserConfiguration {

	/** A qualifier for user secrets support. */
	public static final String USER_SECRETS = "user-secrets";

	/** The qualifier for the user key pair related services. */
	public static final String USER_KEYPAIR = "user-keypair";

	/** The qualifier for the user secret related services. */
	public static final String USER_SECRET = "user-secret";

	/** The qualifier for the token related services. */
	public static final String USER_AUTH_TOKEN = "user-auth-token";

}
