/* ==================================================================
 * CommonDbUtils.java - 6/10/2021 3:46:15 PM
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

package net.solarnetwork.central.common.dao.jdbc;

import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;

/**
 * Common SolarNetwork DB utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonDbUtils {

	/**
	 * Insert a security token.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param tokenId
	 *        the token ID
	 * @param tokenSecret
	 *        the token secret
	 * @param userId
	 *        the owner user ID
	 * @param status
	 *        the status
	 * @param type
	 *        the type
	 * @param policy
	 *        the policy
	 */
	public static void insertSecurityToken(JdbcOperations jdbcTemplate, String tokenId,
			String tokenSecret, Long userId, SecurityTokenStatus status, SecurityTokenType type,
			String policy) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
						+ " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::jsonb)",
				tokenId, tokenSecret, userId, status.name(), type.name(), policy);
	}

}
