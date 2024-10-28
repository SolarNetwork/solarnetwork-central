/* ==================================================================
 * SelectUserSettingsEntity.java - 28/10/2024 7:49:22 am
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;

/**
 * Support for DELETE for {@link UserSettingsEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class DeleteUserSettingsEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			DELETE FROM solardin.cin_user_settings
			WHERE user_id = ?
			""";

	private final Long userId;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 */
	public DeleteUserSettingsEntity(Long userId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, userId);
		return stmt;
	}

}
