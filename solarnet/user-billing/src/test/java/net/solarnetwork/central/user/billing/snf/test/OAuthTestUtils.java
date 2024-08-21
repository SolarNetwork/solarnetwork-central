/* ==================================================================
 * OAuthTestUtils.java - 19/08/2024 7:15:06 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.test;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Testing utilities for OAuth billing.
 *
 * @author matt
 * @version 1.0
 */
public final class OAuthTestUtils {

	private OAuthTestUtils() {
		// not available
	}

	/**
	 * Save a new Flexibility Provider authorization ID.
	 *
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param userId
	 *        the user ID
	 * @param username
	 *        the username
	 * @param oauth
	 *        the OAuth flag
	 * @return the new ID
	 */
	public static UserLongCompositePK saveInstructionInputCredential(JdbcOperations jdbcOps, Long userId,
			String username, boolean oauth) {
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcOps.update((con) -> {
			PreparedStatement stmt = con.prepareStatement(
					"INSERT INTO solardin.inin_credential (user_id, enabled, username, oauth) VALUES (?, ?, ?, ?) RETURNING id",
					Statement.RETURN_GENERATED_KEYS);
			stmt.setObject(1, userId, Types.BIGINT);
			stmt.setBoolean(2, true);
			stmt.setString(3, username);
			stmt.setBoolean(4, oauth);
			return stmt;
		}, holder);
		return new UserLongCompositePK(userId, holder.getKeyAs(Long.class));
	}

}
