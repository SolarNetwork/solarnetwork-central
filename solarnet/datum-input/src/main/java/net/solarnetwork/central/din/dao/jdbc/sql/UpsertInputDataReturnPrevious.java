/* ==================================================================
 * UpsertInputDataReturnPrevious.java - 5/03/2024 12:21:50 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongStringCompositePK;

/**
 * SQL to insert input data and return the previous results.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertInputDataReturnPrevious implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			WITH prev AS (
			    SELECT input_data FROM solardin.din_input_data
			    WHERE user_id = ?
			    	AND node_id = ?
			    	AND source_id = ?
			)
			INSERT INTO solardin.din_input_data (user_id, node_id, source_id, created, input_data)
			VALUES (?, ?, ?, ?, ?)
			ON CONFLICT (user_id, node_id, source_id) DO UPDATE
				SET created = COALESCE(EXCLUDED.created, CURRENT_TIMESTAMP)
					, input_data = EXCLUDED.input_data
			RETURNING (SELECT input_data FROM prev)
			""";

	private UserLongStringCompositePK key;
	private byte[] data;

	/**
	 * Constructor.
	 *
	 * @param key
	 *        the key
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertInputDataReturnPrevious(UserLongStringCompositePK key, byte[] data) {
		super();
		this.key = requireNonNullArgument(key, "key");
		this.data = requireNonNullArgument(data, "data");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(Instant.now());
		stmt.setObject(1, key.getUserId());
		stmt.setObject(2, key.getGroupId());
		stmt.setObject(3, key.getEntityId());
		stmt.setObject(4, key.getUserId());
		stmt.setObject(5, key.getGroupId());
		stmt.setObject(6, key.getEntityId());
		stmt.setTimestamp(7, ts);
		stmt.setBytes(8, data);
		return stmt;
	}

}
