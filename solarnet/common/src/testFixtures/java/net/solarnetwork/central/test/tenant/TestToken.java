/* ==================================================================
 * TestToken.java - 22/09/2026 8:39:25 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.test.tenant;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityTokenWithPolicy;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.domain.SecurityPolicy;

/**
 * An active security token owned by a test user.
 *
 * @param tokenId
 *        the token ID
 * @param tokenSecret
 *        the token secret
 * @param userId
 *        the ID of the user that owns the token
 * @param type
 *        the token type
 * @param policy
 *        the optional token policy
 * @author matt
 * @version 1.0
 */
public record TestToken(String tokenId, String tokenSecret, Long userId, SecurityTokenType type,
		@Nullable SecurityPolicy policy) {

	/**
	 * Create a new token with a random ID and secret.
	 *
	 * @param userId
	 *        the ID of the user that owns the token
	 * @param type
	 *        the token type
	 * @param policy
	 *        the optional token policy
	 * @return the new token
	 */
	public static TestToken randomToken(Long userId, SecurityTokenType type,
			@Nullable SecurityPolicy policy) {
		return new TestToken(randomString(20), randomString(), userId, type, policy);
	}

	/**
	 * Insert the token into the database.
	 *
	 * @param jdbcOps
	 *        the JDBC operations
	 */
	public void insert(JdbcOperations jdbcOps) {
		insertSecurityTokenWithPolicy(jdbcOps, tokenId, tokenSecret, userId,
				SecurityTokenStatus.Active.name(), type.name(), policy);
	}

}
