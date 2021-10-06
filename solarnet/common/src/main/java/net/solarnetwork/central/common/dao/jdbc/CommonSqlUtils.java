/* ==================================================================
 * CommonSqlUtils.java - 6/10/2021 10:04:35 AM
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Common SQL utilities for SolarNetwork.
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonSqlUtils {

	/**
	 * Regex for a line starting with a {@literal --} SQL style comment
	 * character.
	 */
	public static final Pattern SQL_COMMENT = Pattern.compile("^\\s*--");

	/**
	 * Prepare a SQL statement array parameter, optimized to a non-array
	 * parameter if the array holds a single object.
	 * 
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the array value
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt, int p,
			Long[] value) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, p, "bigint", value);
	}

	/**
	 * Prepare a SQL statement array parameter, optimized to a non-array
	 * parameter if the array holds a single object.
	 * 
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param arrayType
	 *        the SQL array type to use
	 * @param value
	 *        the array value
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt, int p,
			String arrayType, Object[] value) throws SQLException {
		if ( value != null ) {
			if ( value.length > 1 ) {
				Array array = con.createArrayOf(arrayType, value);
				stmt.setArray(++p, array);
				array.free();
			} else {
				stmt.setObject(++p, value[0]);
			}
		}
		return p;
	}

}
