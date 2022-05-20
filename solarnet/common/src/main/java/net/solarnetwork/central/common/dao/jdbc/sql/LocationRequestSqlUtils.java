/* ==================================================================
 * LocationRequestSqlUtils.java - 20/05/2022 7:23:29 am
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;

/**
 * LocationRequest SQL support.
 * 
 * @author matt
 * @version 1.0
 */
public final class LocationRequestSqlUtils {

	/**
	 * Append location request criteria WHERE clauses to a buffer.
	 * 
	 * @param id
	 *        the optional ID
	 * @param filter
	 *        the optional filter
	 * @param where
	 *        the buffer to append to
	 * @return the number of SQL parameters generated
	 * @see CommonSqlUtils#WHERE_COMPONENT_PREFIX_LENGTH
	 */
	public static int appendLocationRequestCriteria(Long id, LocationRequestCriteria filter,
			StringBuilder where) {
		int p = 0;
		if ( id != null ) {
			where.append("\tAND id = ?");
			p++;
		}
		if ( filter != null ) {
			if ( filter.hasUserCriteria() ) {
				where.append("\tAND user_id = ANY(?)");
				p++;
			}
			if ( filter.getLocationId() != null ) {
				where.append("\tAND loc_id = ANY(?)");
				p++;
			}
			if ( filter.hasRequestStatusCriteria() ) {
				where.append("\tAND status = ANY(?)");
				p++;
			}
		}
		return p;
	}

	/**
	 * Prepare a location request filter SQL query.
	 * 
	 * @param id
	 *        the optional ID
	 * @param filter
	 *        the optional search criteria
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int prepareLocationRequestCriteria(Long id, LocationRequestCriteria filter,
			Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( id != null ) {
			stmt.setObject(++p, id);
		}
		if ( filter != null ) {
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
		return p;
	}
}
