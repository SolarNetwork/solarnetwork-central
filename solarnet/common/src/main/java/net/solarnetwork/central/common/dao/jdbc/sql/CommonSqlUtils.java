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

package net.solarnetwork.central.common.dao.jdbc.sql;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.domain.CodedValue;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Common SQL utilities for SolarNetwork.
 * 
 * @author matt
 * @version 2.5
 */
public final class CommonSqlUtils {

	/**
	 * Regex for a line starting with a {@literal --} SQL style comment
	 * character.
	 */
	public static final Pattern SQL_COMMENT = Pattern.compile("^\\s*--");

	/**
	 * The number of characters to drop from the start of a leading where
	 * clause.
	 */
	public static final int WHERE_COMPONENT_PREFIX_LENGTH = 4;

	/**
	 * Prepare a SQL statement {@code INTEGER} array parameter, optimized to a
	 * non-array parameter if the array holds a single object.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	 * @since 2.4
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, Integer[] value) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, "integer", value);
	}

	/**
	 * Prepare a SQL statement {@code BIGINT} array parameter, optimized to a
	 * non-array parameter if the array holds a single object.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, Long[] value) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, "bigint", value);
	}

	/**
	 * Prepare a SQL statement {@code BIGINT} array parameter, optimized to a
	 * non-array parameter if the array holds a single object.
	 * 
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the array value
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, Long[] value, boolean setNull) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, "text", value, setNull);
	}

	/**
	 * Prepare a SQL statement {@code TEXT} array parameter, optimized to a
	 * non-array parameter if the array holds a single object.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, String[] value) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, "text", value);
	}

	/**
	 * Prepare a SQL statement {@code TEXT} array parameter, optimized to a
	 * non-array parameter if the array holds a single object.
	 * 
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the array value
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, String[] value, boolean setNull) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, "text", value, setNull);
	}

	/**
	 * Prepare a SQL statement array parameter, optimized to a non-array
	 * parameter if the array holds a single object.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, String arrayType, Object[] value) throws SQLException {
		return prepareOptimizedArrayParameter(con, stmt, parameterOffset, arrayType, value, false);
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
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareOptimizedArrayParameter(Connection con, PreparedStatement stmt,
			int parameterOffset, String arrayType, Object[] value, boolean setNull) throws SQLException {
		if ( value != null ) {
			if ( value.length > 1 ) {
				Array array = con.createArrayOf(arrayType, value);
				stmt.setArray(++parameterOffset, array);
				array.free();
			} else {
				stmt.setObject(++parameterOffset, value[0]);
			}
		} else if ( setNull ) {
			stmt.setNull(++parameterOffset, Types.ARRAY);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL statement {@code INTEGER} array parameter.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	 * @since 2.1
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			Integer[] value) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, value, false);
	}

	/**
	 * Prepare a SQL statement {@code INTEGER} array parameter.
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
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			Integer[] value, boolean setNull) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, "integer", value, setNull);
	}

	/**
	 * Prepare a SQL statement {@code BIGINT} array parameter.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	 * @since 2.1
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			Long[] value) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, value, false);
	}

	/**
	 * Prepare a SQL statement {@code BIGINT} array parameter.
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
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			Long[] value, boolean setNull) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, "bigint", value, setNull);
	}

	/**
	 * Prepare a SQL statement {@code TEXT} array parameter.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	 * @since 2.1
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			String[] value) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, "text", value);
	}

	/**
	 * Prepare a SQL statement {@code TEXT} array parameter.
	 * 
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the array value
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			String[] value, boolean setNull) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, "text", value, setNull);
	}

	/**
	 * Prepare a SQL statement array parameter.
	 * 
	 * <p>
	 * The parameter will <b>not</b> be set if {@code value} is {@literal null}.
	 * </p>
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
	 * @since 2.1
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			String arrayType, Object[] value) throws SQLException {
		return prepareArrayParameter(con, stmt, parameterOffset, arrayType, value, false);
	}

	/**
	 * Prepare a SQL statement array parameter.
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
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareArrayParameter(Connection con, PreparedStatement stmt, int parameterOffset,
			String arrayType, Object[] value, boolean setNull) throws SQLException {
		if ( value != null ) {
			Array array = con.createArrayOf(arrayType, value);
			stmt.setArray(++parameterOffset, array);
			array.free();
		} else if ( setNull ) {
			stmt.setNull(++parameterOffset, Types.ARRAY);
		}
		return parameterOffset;
	}

	/**
	 * Generate SQL {@literal FOR UPDATE SKIP LOCKED} criteria to support
	 * locking.
	 * 
	 * @param skipLocked
	 *        {@literal true} to include the {@literal SKIP LOCKED} clause
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	public static void forUpdate(boolean skipLocked, StringBuilder buf) {
		forUpdate(skipLocked, null, buf);
	}

	/**
	 * Generate SQL {@literal FOR UPDATE SKIP LOCKED} criteria to support
	 * locking.
	 * 
	 * @param skipLocked
	 *        {@literal true} to include the {@literal SKIP LOCKED} clause
	 * @param tableNames
	 *        explicit table names to lock, or {@literal null} for default (all
	 *        referenced tables)
	 * @param buf
	 *        the buffer to append the SQL to
	 * @since 2.2
	 */
	public static void forUpdate(boolean skipLocked, String[] tableNames, StringBuilder buf) {
		buf.append("\nFOR UPDATE");
		if ( tableNames != null && tableNames.length > 0 ) {
			buf.append(" OF");
			int i = 0;
			for ( String tableName : tableNames ) {
				if ( i++ > 0 ) {
					buf.append(',');
				}
				buf.append(' ');
				buf.append(tableName);
			}
		}
		if ( skipLocked ) {
			buf.append(" SKIP LOCKED");
		}
	}

	/**
	 * Generate SQL {@literal LIMIT x OFFSET y} criteria to support pagination.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \nLIMIT ? OFFSET ?}.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 */
	public static int limitOffset(PaginationCriteria filter, StringBuilder buf) {
		int result = 0;
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ?");
				result++;
			}
		}
		if ( filter != null && filter.getOffset() != null ) {
			int offset = filter.getOffset();
			if ( offset > 0 ) {
				if ( result < 1 ) {
					buf.append('\n');
				} else {
					buf.append(' ');
				}
				buf.append("OFFSET ?");
				result++;
			}
		}
		return result;
	}

	/**
	 * Prepare a SQL query limit/offset.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param con
	 *        the JDBC connection
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @see #limitOffset(PaginationCriteria, StringBuilder)
	 * @since 2.1
	 */
	public static int prepareLimitOffset(PaginationCriteria filter, Connection con,
			PreparedStatement stmt, int parameterOffset) throws SQLException {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				stmt.setInt(++parameterOffset, max);
			}
		}
		if ( filter != null && filter.getOffset() != null ) {
			int offset = filter.getOffset();
			if ( offset > 0 ) {
				stmt.setInt(++parameterOffset, offset);
			}
		}
		return parameterOffset;
	}

	/**
	 * Wrap a SQL query with a {@literal SELECT COUNT(*)} clause.
	 * 
	 * @param sql
	 *        the SQL query to wrap
	 * @return the wrapped query
	 * @since 2.1
	 */
	public static String wrappedCountQuery(String sql) {
		return "SELECT COUNT(*) FROM (" + sql + ") AS q";
	}

	/**
	 * Generate SQL {@literal LIMIT x OFFSET y} criteria to support pagination
	 * where the limit and offset are generated as literal values.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \nLIMIT x OFFSET y}.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	public static void limitOffsetLiteral(PaginationCriteria filter, StringBuilder buf) {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ").append(max);
			}
			Integer offset = filter.getOffset();
			if ( offset != null ) {
				buf.append(" OFFSET ").append(offset);
			}
		}
	}

	/**
	 * Create a {@link PreparedStatement} with settings appropriate for updating
	 * or not.
	 * 
	 * @param con
	 *        the connection
	 * @param sql
	 *        the SQL statement
	 * @param forUpdate
	 *        {@literal true} to use a scroll-insensitive, updatable
	 *        {@link ResetSet}, or {@literal false} for a forward-only,
	 *        read-only one
	 * @return the prepared statement
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static PreparedStatement createPreparedStatement(Connection con, String sql,
			boolean forUpdate) throws SQLException {
		PreparedStatement stmt;
		if ( forUpdate ) {
			stmt = con.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE);
		} else {
			stmt = con.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
					ResultSet.CLOSE_CURSORS_AT_COMMIT);
		}
		return stmt;
	}

	/**
	 * Generate SQL {@literal WHERE} criteria for an array containment clause.
	 * 
	 * <p>
	 * If {@code array} contains exactly one value, the generated SQL will use a
	 * simple equality comparison. Otherwise an {@code ANY()} comparison will be
	 * generated.
	 * </p>
	 * 
	 * @param array
	 *        the array value to match
	 * @param colName
	 *        the array SQL column name
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @since 2.1
	 * @see #prepareOptimizedArrayParameter(Connection, PreparedStatement, int,
	 *      String, Object[])
	 */
	public static int whereOptimizedArrayContains(Object[] array, String colName, StringBuilder buf) {
		int paramCount = 0;
		if ( array != null && array.length > 0 ) {
			buf.append("\tAND ").append(colName).append(" = ");
			if ( array.length > 1 ) {
				buf.append("ANY(?)");
			} else {
				buf.append("?");
			}
			buf.append("\n");
			paramCount = 1;
		}
		return paramCount;
	}

	/**
	 * Generate SQL {@literal WHERE} criteria for a date range.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal \tAND c = ?\n} for
	 * each clause. The leading tab and {@literal AND} and space characters are
	 * <b>not</b> stripped.
	 * </p>
	 * 
	 * @param filter
	 *        the search criteria
	 * @param column
	 *        the date column name to use, e.g. {@code solardatum.ts}
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of JDBC query parameters generated
	 * @since 2.1
	 */
	public static int whereDateRange(DateRangeCriteria filter, String colName, StringBuilder buf) {
		int paramCount = 0;
		if ( filter.getStartDate() != null ) {
			buf.append("\tAND ").append(colName).append(" >= ?\n");
			paramCount += 1;
		}
		if ( filter.getEndDate() != null ) {
			buf.append("\tAND ").append(colName).append(" < ?\n");
			paramCount += 1;
		}
		return paramCount;
	}

	/**
	 * Prepare a SQL query date range filter.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.1
	 */
	public static int prepareDateRange(DateRangeCriteria filter, PreparedStatement stmt,
			int parameterOffset) throws SQLException {
		if ( filter.getStartDate() != null ) {
			stmt.setTimestamp(++parameterOffset, Timestamp.from(filter.getStartDate()));
		}
		if ( filter.getEndDate() != null ) {
			stmt.setTimestamp(++parameterOffset, Timestamp.from(filter.getEndDate()));
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query JSON string parameter.
	 * 
	 * @param data
	 *        the JSON data; will be serialized via
	 *        {@link JsonUtils#getJSONString(Object, String)}
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code data} serializes
	 *        to {@literal null}, or {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareJsonString(Object data, PreparedStatement stmt, int parameterOffset,
			boolean setNull) throws SQLException {
		String props = JsonUtils.getJSONString(data, null);
		if ( props != null ) {
			stmt.setString(++parameterOffset, props);
		} else if ( setNull ) {
			stmt.setNull(++parameterOffset, Types.VARCHAR);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query {@code CodedValue} parameter as a {@code INTEGER}.
	 * 
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the value to set
	 * @param defaultValue
	 *        the value to set if {@code value} is {@literal null}
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null} and {@literal defaultValue} is {@literal null}, or
	 *        {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int prepareCodedValue(PreparedStatement stmt, int parameterOffset, CodedValue value,
			CodedValue defaultValue, boolean setNull) throws SQLException {
		if ( value != null ) {
			stmt.setInt(++parameterOffset, value.getCode());
		} else if ( defaultValue != null ) {
			stmt.setInt(++parameterOffset, defaultValue.getCode());
		} else if ( setNull ) {
			stmt.setNull(++parameterOffset, Types.INTEGER);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query {@code CodedValue} parameter as a {@code CHARACTER}.
	 * 
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the value to set
	 * @param defaultValue
	 *        the value to set if {@code value} is {@literal null}
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null} and {@literal defaultValue} is {@literal null}, or
	 *        {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.5
	 */
	public static int prepareCodedValueChar(PreparedStatement stmt, int parameterOffset,
			CodedValue value, CodedValue defaultValue, boolean setNull) throws SQLException {
		if ( value != null ) {
			stmt.setObject(++parameterOffset, (char) value.getCode());
		} else if ( defaultValue != null ) {
			stmt.setObject(++parameterOffset, (char) defaultValue.getCode());
		} else if ( setNull ) {
			stmt.setNull(++parameterOffset, Types.CHAR);
		}
		return parameterOffset;
	}

	/**
	 * Prepare a SQL query {@code CodedValue} parameter as a {@code INTEGER}.
	 * 
	 * @param stmt
	 *        the JDBC statement
	 * @param parameterOffset
	 *        the zero-based starting JDBC statement parameter offset
	 * @param value
	 *        the value to set
	 * @param defaultValue
	 *        the value to set if {@code value} is {@literal null}
	 * @param setNull
	 *        {@literal true} to set a NULL parameter if {@code value} is
	 *        {@literal null} and {@literal defaultValue} is {@literal null}, or
	 *        {@literal false} to skip the parameter
	 * @return the new JDBC statement parameter offset
	 * @throws SQLException
	 *         if any SQL error occurs
	 * @since 2.2
	 */
	public static int prepareCodedValuesArray(Connection con, PreparedStatement stmt,
			int parameterOffset, Collection<? extends CodedValue> values, boolean setNull)
			throws SQLException {
		Integer[] codes = null;
		if ( values != null ) {
			codes = values.stream().map(CodedValue::getCode).toArray(Integer[]::new);
		}
		return prepareArrayParameter(con, stmt, parameterOffset, codes, setNull);
	}

	/**
	 * Generate SQL {@literal ORDER BY} criteria for a set of
	 * {@link SortDescriptor}.
	 * 
	 * <p>
	 * The buffer is populated with a pattern of {@literal , key} for each key.
	 * The leading comma and space characters are <b>not</b> stripped, but the
	 * returned value indicates the number of characters to trim from the
	 * results if needed.
	 * </p>
	 * 
	 * @param sorts
	 *        the sorts
	 * @param sortKeyMapping
	 *        the mapping of sort keys to SQL sort names
	 * @param buf
	 *        the buffer to append the SQL to
	 * @return the number of leading "joining" characters added to {@code buf};
	 *         will either be {@literal 0} or {@literal 2}
	 * @since 2.3
	 */
	public static int orderBySorts(Iterable<SortDescriptor> sorts, Map<String, String> sortKeyMapping,
			StringBuilder buf) {
		if ( sorts == null || sortKeyMapping == null || sortKeyMapping.isEmpty() ) {
			return 0;
		}
		boolean appended = false;
		for ( SortDescriptor sort : sorts ) {
			String sqlName = sortKeyMapping.get(sort.getSortKey());
			if ( sqlName != null ) {
				appended = true;
				buf.append(", ").append(sqlName);
				if ( sort.isDescending() ) {
					buf.append(" DESC");
				}
			}
		}
		return (appended ? 2 : 0);
	}

}
