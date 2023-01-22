/* ==================================================================
 * CommonDbUtils.java - 22/01/2023 2:37:57 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.common.dao.jdbc.AuditNodeServiceValueRowMapper;
import net.solarnetwork.central.common.dao.jdbc.StaleAuditNodeServiceValueRowMapper;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.domain.StaleAuditNodeServiceValue;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Common database utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class CommonDbTestUtils {

	private static final Logger log = LoggerFactory.getLogger(CommonDbTestUtils.class);

	private CommonDbTestUtils() {
		// not available
	}

	/**
	 * Get a list of all hourly {@link AuditNodeServiceValue} entities in the
	 * database.
	 * 
	 * <p>
	 * The values are ordered by node ID, service, and timestamp.
	 * </p>
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @return the values, never {@literal null}
	 */
	public static List<AuditNodeServiceValue> listAuditNodeServiceValueHourly(JdbcOperations jdbcOps) {
		return jdbcOps.query("""
				SELECT ts_start, node_id, service, 'h' AS agg_kind, cnt
				FROM solardatm.aud_node_io
				ORDER BY node_id, service, ts_start
				""", AuditNodeServiceValueRowMapper.INSTANCE);
	}

	/**
	 * Get a list of all daily {@link AuditNodeServiceValue} entities in the
	 * database.
	 * 
	 * <p>
	 * The values are ordered by node ID, service, and timestamp.
	 * </p>
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @return the values, never {@literal null}
	 */
	public static List<AuditNodeServiceValue> listAuditNodeServiceValueDaily(JdbcOperations jdbcOps) {
		return jdbcOps.query("""
				SELECT ts_start, node_id, service, 'd' AS agg_kind, cnt
				FROM solardatm.aud_node_daily
				ORDER BY node_id, service, ts_start
				""", AuditNodeServiceValueRowMapper.INSTANCE);
	}

	/**
	 * Get a list of all daily {@link AuditNodeServiceValue} entities in the
	 * database.
	 * 
	 * <p>
	 * The values are ordered by node ID, service, and timestamp.
	 * </p>
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @return the values, never {@literal null}
	 */
	public static List<AuditNodeServiceValue> listAuditNodeServiceValueMonthly(JdbcOperations jdbcOps) {
		return jdbcOps.query("""
				SELECT ts_start, node_id, service, 'M' AS agg_kind, cnt
				FROM solardatm.aud_node_monthly
				ORDER BY node_id, service, ts_start
				""", AuditNodeServiceValueRowMapper.INSTANCE);
	}

	/**
	 * Upsert an audit node service values.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @param stales
	 *        the stale values to upsert
	 */
	public static void insertAuditNodeServiceValues(JdbcOperations jdbcOps,
			Iterable<AuditNodeServiceValue> stales) {
		Map<Aggregation, List<AuditNodeServiceValue>> groups = new HashMap<>(3);
		for ( AuditNodeServiceValue s : stales ) {
			groups.computeIfAbsent(s.getAggregation(), a -> new ArrayList<>()).add(s);
		}
		for ( Map.Entry<Aggregation, List<AuditNodeServiceValue>> me : groups.entrySet() ) {
			final Aggregation agg = me.getKey();
			final String tableName = "aud_node_" + switch (agg) {
				case Hour -> "io";
				case Day -> "daily";
				case Month -> "monthly";
				default -> throw new IllegalArgumentException("Unsupported aggregation [" + agg + "]");
			};
			final StringBuilder sql = new StringBuilder("INSERT INTO solardatm.");
			sql.append(tableName);
			sql.append(' ');
			sql.append("""
					(node_id, service, ts_start, cnt)
					VALUES (?, ?, ?, ?)
					ON CONFLICT (node_id, service, ts_start) DO UPDATE
					SET cnt =
					""");
			sql.append(tableName).append(".cnt + EXCLUDED.cnt");
			jdbcOps.execute(new ConnectionCallback<Void>() {

				@Override
				public Void doInConnection(Connection con) throws SQLException, DataAccessException {
					try (PreparedStatement datumStmt = con.prepareStatement(sql.toString())) {
						for ( AuditNodeServiceValue d : stales ) {
							if ( log != null ) {
								log.debug("Inserting {} AuditNodeServiceValue: {}", agg, d);
							}
							datumStmt.setObject(1, d.getNodeId());
							datumStmt.setString(2, d.getService());
							datumStmt.setTimestamp(3, Timestamp.from(d.getTimestamp()));
							datumStmt.setLong(4, d.getCount());
							datumStmt.execute();
						}
					}
					return null;
				}
			});
		}
	}

	/**
	 * Audit a node service.
	 * 
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param nodeId
	 *        the node ID
	 * @param service
	 *        the service name
	 * @param ts
	 *        the timestamp
	 * @param count
	 *        the count to add
	 */
	public static void auditNodeService(JdbcOperations jdbcOps, Long nodeId, String service, Instant ts,
			int count) {
		jdbcOps.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				log.debug("Incrementing audit node service count for node {} service {} @ {}: +{}",
						nodeId, service, ts, count);
				try (CallableStatement stmt = con
						.prepareCall("{call solardatm.audit_increment_node_count(?,?,?,?)}")) {
					stmt.setObject(1, nodeId);
					stmt.setString(2, service);
					stmt.setTimestamp(3, Timestamp.from(ts));
					stmt.setInt(4, count);
					stmt.execute();
				}
				return null;
			}
		});
	}

	/**
	 * Assert that an {@link AuditNodeServiceValue} has properties that match a
	 * given instance.
	 * 
	 * @param prefix
	 *        a test message prefix
	 * @param actual
	 *        the actual value
	 * @param expected
	 *        the expected value
	 */
	public static void assertAuditNodeServiceValue(String prefix, AuditNodeServiceValue actual,
			AuditNodeServiceValue expected) {
		assertThat(prefix + " exists", actual, is(notNullValue()));
		assertThat(prefix + " node ID", actual.getNodeId(), is(equalTo(expected.getNodeId())));
		assertThat(prefix + " service", actual.getService(), is(equalTo(expected.getService())));
		assertThat(prefix + " timestamp", actual.getTimestamp(), is(equalTo(expected.getTimestamp())));
		assertThat(prefix + " aggregation", actual.getAggregation(),
				is(equalTo(expected.getAggregation())));
		assertThat(prefix + " count", actual.getCount(), is(equalTo(expected.getCount())));
	}

	/**
	 * Get a list of all hourly {@link AuditNodeServiceValue} entities in the
	 * database.
	 * 
	 * <p>
	 * The values are ordered by aggregation, service, timestamp, and node ID.
	 * </p>
	 * 
	 * @param jdbcOps
	 *        the JDBC operations to use
	 * @return the values, never {@literal null}
	 */
	public static List<StaleAuditNodeServiceValue> listStaleAuditNodeServiceValues(
			JdbcOperations jdbcOps) {
		return jdbcOps.query("""
				SELECT ts_start, node_id, service, aud_kind, created
				FROM solardatm.aud_stale_node
				ORDER BY aud_kind, service, ts_start, node_id
				""", StaleAuditNodeServiceValueRowMapper.INSTANCE);
	}

	/**
	 * Assert that an {@link StaleAuditNodeServiceValue} has properties that
	 * match a given instance.
	 * 
	 * @param prefix
	 *        a test message prefix
	 * @param actual
	 *        the actual value
	 * @param expected
	 *        the expected value
	 */
	public static void assertStaleAuditNodeServiceValue(String prefix, StaleAuditNodeServiceValue actual,
			StaleAuditNodeServiceValue expected) {
		assertThat(prefix + " exists", actual, is(notNullValue()));
		assertThat(prefix + " node ID", actual.getNodeId(), is(equalTo(expected.getNodeId())));
		assertThat(prefix + " service", actual.getService(), is(equalTo(expected.getService())));
		assertThat(prefix + " timestamp", actual.getTimestamp(), is(equalTo(expected.getTimestamp())));
		assertThat(prefix + " aggregation", actual.getAggregation(),
				is(equalTo(expected.getAggregation())));
	}

	/**
	 * Call the {@code solardatm.process_one_aud_stale_node} stored procedure to
	 * compute audit data.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @param kinds
	 *        the kinds of stale audit records to process; e.g. {@code Day} or
	 *        {@code Month}
	 */
	public static void processStaleAuditNodeService(Logger log, JdbcOperations jdbcOps,
			Set<Aggregation> kinds) {
		debugStaleAuditNodeServiceTable(log, jdbcOps, "Stale audit datum at start");

		List<Aggregation> sortedKinds = kinds.stream().sorted(Aggregation::compareLevel)
				.collect(Collectors.toList());

		jdbcOps.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.process_one_aud_stale_node(?)}")) {
					cs.registerOutParameter(1, Types.INTEGER);
					for ( Aggregation kind : sortedKinds ) {
						int processed = processStaleAuditKind(kind.getKey(), cs);
						log.debug("Processed {} stale {} audit node", processed, kind.getKey());
						debugStaleAuditNodeServiceTable(log, jdbcOps,
								"Stale audit node after process " + kind.getKey());
					}
				}
				return null;
			}
		});
	}

	/**
	 * Call the {@code solardatm.process_one_aud_stale_node} stored procedure to
	 * compute audit data for all aggregate kinds.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcOps
	 *        the JDBC template to use
	 * @see #processStaleAggregateDatum(Logger, JdbcOperations, Set)
	 */
	public static void processStaleAuditNodeService(Logger log, JdbcOperations jdbcOps) {
		processStaleAuditNodeService(log, jdbcOps, EnumSet.of(Aggregation.Day, Aggregation.Month));
	}

	private static int processStaleAuditKind(String kind, CallableStatement cs) throws SQLException {
		int processed = 0;
		while ( true ) {
			cs.setString(2, kind);
			if ( cs.execute() ) {
				processed = cs.getInt(1);
			} else {
				break;
			}
		}
		return processed;
	}

	/**
	 * Log the contents of the stale audit node service table.
	 * 
	 * @param log
	 *        the logger to log to
	 * @param jdbcOps
	 *        the JDBC operations
	 * @param msg
	 *        a log message
	 */
	public static void debugStaleAuditNodeServiceTable(Logger log, JdbcOperations jdbcOps, String msg) {
		List<Map<String, Object>> staleRows = jdbcOps.queryForList(
				"SELECT * FROM solardatm.aud_stale_node ORDER BY aud_kind, service, ts_start, node_id");
		log.debug("{}:\n{}", msg, staleRows.stream().map(e -> e.toString()).collect(joining("\n")));
	}

}
