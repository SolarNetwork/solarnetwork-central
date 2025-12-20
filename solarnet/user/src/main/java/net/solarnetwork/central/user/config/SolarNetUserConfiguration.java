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
 * @version 1.3
 */
public interface SolarNetUserConfiguration {

	/** A qualifier for user secrets support. */
	String USER_SECRETS = "user-secrets";

	/** The qualifier for the user key pair related services. */
	String USER_KEYPAIR = "user-keypair";

	/** The qualifier for the user secret related services. */
	String USER_SECRET = "user-secret";

	/** The qualifier for the token related services. */
	String USER_AUTH_TOKEN = "user-auth-token";

	/**
	 * A qualifier for user instructions support.
	 * 
	 * @since 1.3
	 */
	String USER_INSTRUCTIONS = "user-instr";

	/**
	 * A qualifier for user instructions expression support.
	 * 
	 * @since 1.3
	 */
	String USER_INSTRUCTIONS_EXPRESSIONS = "user-instr-expr";

	/**
	 * A qualifier for user instructions HTTP.
	 *
	 * @since 1.3
	 */
	String USER_INSTRUCTIONS_HTTP = "user-instr-http";

	/**
	 * A qualifier for user instructions locks.
	 *
	 * @since 1.3
	 */
	String USER_INSTRUCTIONS_LOCKS = "user-instr-locks";

	/**
	 * A qualifier for user metadata.
	 * 
	 * @since 1.3
	 */
	String USER_METADATA = "user-metadata";

	/**
	 * A qualifier for user metadata path.
	 * 
	 * @since 1.3
	 */
	String USER_METADATA_PATH = "user-metadata-path";

	/**
	 * The audit service name for content processed (bytes).
	 *
	 * @since 1.3
	 */
	String CONTENT_PROCESSED_AUDIT_SERVICE = "ccio";

}
