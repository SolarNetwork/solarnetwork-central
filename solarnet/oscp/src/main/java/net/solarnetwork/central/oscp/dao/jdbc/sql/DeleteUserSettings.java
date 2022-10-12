/* ==================================================================
 * DeleteUserSettings.java - 10/10/2022 10:24:10 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Delete a user settings entity.
 * 
 * @author matt
 * @version 1.0
 */
public class DeleteUserSettings implements PreparedStatementCreator, SqlProvider {

	private final Long userId;

	/**
	 * Constructor.
	 * 
	 * @param userId
	 *        the ID of the user to select
	 */
	public DeleteUserSettings(Long userId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
	}

	@Override
	public String getSql() {
		return """
				DELETE FROM solaroscp.oscp_user_settings
				WHERE user_id = ?
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, userId, Types.BIGINT);
		return stmt;
	}

}
