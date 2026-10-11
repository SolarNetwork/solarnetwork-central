/* ==================================================================
 * GnucashSqlUtils.java - 11/10/2026 2:46:24 pm
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncNumeric;

/**
 * Utilities for GnuCash SQL statements.
 *
 * <p>
 * GnuCash stores timestamps in {@code timestamp without time zone} columns,
 * as UTC wall time with second precision.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class GnucashSqlUtils {

	private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

	private GnucashSqlUtils() {
		// not available
	}

	/**
	 * Validate a schema name.
	 *
	 * @param schema
	 *        the schema name
	 * @return the schema name
	 * @throws IllegalArgumentException
	 *         if {@code schema} is {@code null} or not a plain SQL identifier
	 */
	public static String schemaName(String schema) {
		if ( !IDENTIFIER.matcher(requireNonNullArgument(schema, "schema")).matches() ) {
			throw new IllegalArgumentException(
					"The schema [%s] is not a valid SQL identifier.".formatted(schema));
		}
		return schema;
	}

	/**
	 * Set a timestamp parameter, as UTC wall time.
	 *
	 * @param stmt
	 *        the statement
	 * @param p
	 *        the parameter index
	 * @param ts
	 *        the timestamp, or {@code null}
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static void setTimestamp(PreparedStatement stmt, int p, @Nullable Instant ts)
			throws SQLException {
		if ( ts == null ) {
			stmt.setNull(p, Types.TIMESTAMP);
		} else {
			stmt.setObject(p,
					LocalDateTime.ofInstant(ts.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC));
		}
	}

	/**
	 * Get a timestamp column, as UTC wall time.
	 *
	 * @param rs
	 *        the result set
	 * @param col
	 *        the column index
	 * @return the timestamp, or {@code null}
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static @Nullable Instant getTimestamp(ResultSet rs, int col) throws SQLException {
		final LocalDateTime ts = rs.getObject(col, LocalDateTime.class);
		return (ts != null ? ts.toInstant(ZoneOffset.UTC) : null);
	}

	/**
	 * Set a numeric parameter pair.
	 *
	 * @param stmt
	 *        the statement
	 * @param p
	 *        the index of the numerator parameter; the denominator is the next
	 *        parameter
	 * @param value
	 *        the value
	 * @return the index of the denominator parameter
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static int setNumeric(PreparedStatement stmt, int p, GncNumeric value)
			throws SQLException {
		stmt.setLong(p, value.num());
		stmt.setLong(++p, value.denom());
		return p;
	}

	/**
	 * Get a numeric column pair.
	 *
	 * @param rs
	 *        the result set
	 * @param col
	 *        the index of the numerator column; the denominator is the next
	 *        column
	 * @return the value
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static GncNumeric getNumeric(ResultSet rs, int col) throws SQLException {
		return new GncNumeric(rs.getLong(col), rs.getLong(col + 1));
	}

	/**
	 * Get a boolean stored as an integer.
	 *
	 * @param rs
	 *        the result set
	 * @param col
	 *        the column index
	 * @return {@code true} if the value is not zero
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static boolean getBoolean(ResultSet rs, int col) throws SQLException {
		return rs.getInt(col) != 0;
	}

	/**
	 * Get a string column, using an empty string for {@code null}.
	 *
	 * @param rs
	 *        the result set
	 * @param col
	 *        the column index
	 * @return the value, never {@code null}
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static String getString(ResultSet rs, int col) throws SQLException {
		final String s = rs.getString(col);
		return (s != null ? s : "");
	}

}
