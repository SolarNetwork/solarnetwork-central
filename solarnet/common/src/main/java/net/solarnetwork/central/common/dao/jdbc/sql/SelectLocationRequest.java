/* ==================================================================
 * SelectLocationRequest.java - 19/05/2022 3:08:25 pm
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;

/**
 * Select location request entities.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class SelectLocationRequest implements PreparedStatementCreator, SqlProvider {

	private final Long id;
	private final LocationRequestCriteria filter;

	/**
	 * Select for a specific entity.
	 * 
	 * @param id
	 *        the ID of the entity to fetch
	 */
	public SelectLocationRequest(Long id) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.filter = null;
	}

	/**
	 * Select for matching entities.
	 * 
	 * @param id
	 *        the ID of the entity to fetch
	 */
	public SelectLocationRequest(LocationRequestCriteria filter) {
		super();
		this.id = null;
		this.filter = requireNonNullArgument(filter, "filter");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append(
				"SELECT id, created, modified, user_id, status, jdata, loc_id, message\nFROM solarnet.sn_loc_req\nWHERE ");
		if ( id != null ) {
			buf.append("id = ?");
		} else {
			StringBuilder where = new StringBuilder();
			if ( filter.hasUserCriteria() ) {
				where.append("\tAND user_id = ANY(?)");
			}
			if ( filter.getLocationId() != null ) {
				where.append("\t AND loc_id = ANY(?)");
			}
			if ( filter.hasRequestStatusCriteria() ) {
				where.append("\t AND status = ANY(?)");
			}
			if ( where.length() > 0 ) {
				buf.append(where.substring(4));
			}
			buf.append("\nORDER BY id");
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		if ( id != null ) {
			stmt.setObject(1, id);
		} else {
			int p = 0;
			if ( filter.hasUserCriteria() ) {
				Array array = con.createArrayOf("BIGINT", filter.getUserIds());
				stmt.setArray(++p, array);
				array.free();
			}
			if ( filter.getLocationId() != null ) {
				Array array = con.createArrayOf("BIGINT", filter.getLocationIds());
				stmt.setArray(++p, array);
				array.free();
			}
			if ( filter.hasRequestStatusCriteria() ) {
				String[] statuses = filter.getRequestStatuses().stream()
						.map(e -> String.valueOf((char) e.getCode())).toArray(String[]::new);
				Array array = con.createArrayOf("TEXT", statuses);
				stmt.setArray(++p, array);
				array.free();
			}
		}
		return stmt;
	}

}
