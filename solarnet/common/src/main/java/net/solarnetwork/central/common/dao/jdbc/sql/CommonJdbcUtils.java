/* ==================================================================
 * CommonJdbcUtils.java - 3/08/2022 11:12:21 am
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

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientException;
import java.time.Period;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.postgresql.util.PGInterval;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.support.FilteredResultsProcessor;

/**
 * Common JDBC utilities.
 *
 * @author matt
 * @version 2.6
 */
public final class CommonJdbcUtils {

	private CommonJdbcUtils() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Execute a streaming query.
	 *
	 * @param <T>
	 *        the entity type
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param processor
	 *        the stream processor
	 * @param sql
	 *        the prepared statement creator
	 * @param mapper
	 *        the row mapper
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.2
	 */
	public static <T> void executeStreamingQuery(JdbcOperations jdbcOps,
			FilteredResultsProcessor<T> processor, PreparedStatementCreator sql, RowMapper<T> mapper)
			throws IOException {
		executeStreamingQuery(jdbcOps, processor, sql, mapper, null, null, null, Map.of());
	}

	/**
	 * Execute a streaming query.
	 *
	 * @param <T>
	 *        the entity type
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param processor
	 *        the stream processor
	 * @param sql
	 *        the prepared statement creator
	 * @param mapper
	 *        the row mapper
	 * @param totalResultCount
	 *        the total result count (or {@code null})
	 * @param startingOffset
	 *        the starting offset (or {@code null})
	 * @param expectedResultCount
	 *        the expected result count (or {@code null})
	 * @param attributes
	 *        the attributes (or {@code null})
	 * @throws IOException
	 *         if any IO error occurs
	 * @since 1.2
	 */
	public static <T> void executeStreamingQuery(JdbcOperations jdbcOps,
			FilteredResultsProcessor<T> processor, PreparedStatementCreator sql, RowMapper<T> mapper,
			@Nullable Long totalResultCount, @Nullable Integer startingOffset,
			@Nullable Integer expectedResultCount, @Nullable Map<String, ?> attributes)
			throws IOException {
		processor.start(totalResultCount, startingOffset, expectedResultCount, attributes);
		try {
			jdbcOps.execute(sql, (PreparedStatement ps) -> {
				try (ResultSet rs = ps.executeQuery()) {
					int row = 0;
					while ( rs.next() ) {
						T entity = mapper.mapRow(rs, ++row);
						processor.handleResultItem(entity);
					}
				} catch ( IOException e ) {
					throw new RuntimeException(e);
				}
				return null;
			});
		} catch ( RuntimeException e ) {
			if ( e.getCause() instanceof IOException ) {
				throw (IOException) e.getCause();
			}
			throw e;
		}

	}

	/**
	 * Get a Period from an INTERVAL column value.
	 *
	 * @param rs
	 *        the result set to read from
	 * @param column
	 *        the column number to get as a Period
	 * @return the period, or {@code null} if the column value is null
	 * @throws IllegalArgumentException
	 *         if {@code value} is not of a supported type
	 * @throws SQLException
	 *         if an error occurs
	 * @since 2.2
	 */
	public static @Nullable Period getIntervalPeriod(ResultSet rs, int column) throws SQLException {
		Object o = rs.getObject(column);
		try {
			return getIntervalPeriod(o);
		} catch ( IllegalArgumentException e ) {
			throw new IllegalArgumentException(
					"Unsupported Period type in column %d: %s".formatted(column, o.getClass()));
		}
	}

	/**
	 * Get a Period from an INTERVAL column value.
	 *
	 * @param rs
	 *        the result set to read from
	 * @param column
	 *        the column number to get as a Period
	 * @return the period (presumed non-null)
	 * @throws IllegalArgumentException
	 *         if {@code value} is not of a supported type
	 * @throws SQLException
	 *         if an error occurs
	 * @since 2.3
	 */
	@SuppressWarnings("NullAway")
	public static Period intervalPeriod(ResultSet rs, int column) throws SQLException {
		return getIntervalPeriod(rs, column);
	}

	/**
	 * Get a Period from an INTERVAL column value.
	 *
	 * @param value
	 *        the column number to get as a Period
	 * @return the period, or {@code null} if the column value is null
	 * @throws IllegalArgumentException
	 *         if {@code value} is not of a supported type
	 * @since 2.2
	 */
	public static @Nullable Period getIntervalPeriod(Object value) {
		if ( value == null ) {
			return null;
		} else if ( value instanceof PGInterval pg ) {
			if ( pg.isNull() ) {
				return null;
			}
			return Period.of(pg.getYears(), pg.getMonths(), pg.getDays());
		}
		throw new IllegalArgumentException("Unsupported Period type: %s".formatted(value.getClass()));
	}

	/**
	 * Test if an exception from a JDBC operation looks transient, so the
	 * operation might succeed if tried again later.
	 *
	 * <p>
	 * An exception is considered transient if it is a
	 * {@link SQLTransientException} or {@link SQLRecoverableException}, or has
	 * a SQL state for a connection exception ({@code 08}), a transaction
	 * rollback such as a deadlock ({@code 40}), insufficient resources
	 * ({@code 53}), a lock not available ({@code 55P03}), or operator
	 * intervention such as a statement timeout ({@code 57}).
	 * </p>
	 *
	 * @param t
	 *        the exception to test
	 * @return {@code true} if {@code t} looks transient
	 * @since 2.5
	 */
	public static boolean isTransientException(@Nullable Throwable t) {
		if ( t instanceof SQLTransientException || t instanceof SQLRecoverableException ) {
			return true;
		}
		if ( t instanceof SQLException sqlEx ) {
			final String state = sqlEx.getSQLState();
			if ( state != null ) {
				return (state.startsWith("08") || state.startsWith("40") || state.startsWith("53")
						|| state.equals("55P03") || state.startsWith("57"));
			}
		}
		return false;
	}

}
