/* ==================================================================
 * SelectInputDataEntity.java - 5/03/2024 11:02:15 am
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

package net.solarnetwork.central.din.dao.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.din.domain.InputDataEntity;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.util.ObjectUtils;

/**
 * Select {@link InputDataEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class SelectInputDataEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			SELECT user_id, node_id, source_id, created, input_data
			FROM solardin.din_input_data
			WHERE user_id = ?
				 AND node_id = ?
				 AND source_id = ?
			""";

	private final UserLongStringCompositePK key;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectInputDataEntity(UserLongStringCompositePK key) {
		super();
		this.key = ObjectUtils.requireNonNullArgument(key, "key");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setObject(1, key.getUserId());
		stmt.setObject(2, key.getGroupId());
		stmt.setString(3, key.getEntityId());
		return stmt;
	}

}
