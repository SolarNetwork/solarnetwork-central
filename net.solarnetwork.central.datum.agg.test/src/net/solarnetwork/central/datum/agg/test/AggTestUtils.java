/* ==================================================================
 * AggTestUtils.java - 23/11/2020 7:07:47 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg.test;

import static java.util.stream.Collectors.joining;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.jdbc.AggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.ObjectDatumIdRowMapper;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.util.JsonUtils;

/**
 * Helper methods for agg tests.
 * 
 * @author matt
 * @version 1.0
 */
public class AggTestUtils {

	/**
	 * Create a {@link NodeDatumStreamMetadata} out of a collection of
	 * {@link GeneralNodeDatum} instances.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param timeZoneId
	 *        the time zone ID
	 * @param datums
	 *        the datums
	 * @param nspk
	 *        the specific node+source to create the metadata for
	 * @return the metadata
	 */
	public static NodeDatumStreamMetadata createMetadata(UUID streamId, String timeZoneId,
			Iterable<GeneralNodeDatum> datums, NodeSourcePK nspk) {
		Set<String> iNames = new LinkedHashSet<>(4);
		Set<String> aNames = new LinkedHashSet<>(4);
		Set<String> sNames = new LinkedHashSet<>(4);
		for ( GeneralNodeDatum d : datums ) {
			if ( d.getSamples() == null || !(d.getNodeId().equals(nspk.getNodeId())
					&& d.getSourceId().equals(nspk.getSourceId())) ) {
				continue;
			}
			GeneralDatumSamples s = d.getSamples();
			if ( s.getInstantaneous() != null ) {
				iNames.addAll(s.getInstantaneous().keySet());
			}
			if ( s.getAccumulating() != null ) {
				aNames.addAll(s.getAccumulating().keySet());
			}
			if ( s.getStatus() != null ) {
				sNames.addAll(s.getStatus().keySet());
			}
		}
		return new BasicNodeDatumStreamMetadata(streamId, timeZoneId, nspk.getNodeId(),
				nspk.getSourceId(), iNames.isEmpty() ? null : iNames.toArray(new String[iNames.size()]),
				aNames.isEmpty() ? null : aNames.toArray(new String[aNames.size()]),
				sNames.isEmpty() ? null : sNames.toArray(new String[sNames.size()]));
	}

	/**
	 * Ingest a set of datum into the {@literal da_datm} table, using the
	 * {@code solardatm.store_datum()} stored procedure that includes side
	 * effects like "stale" and audit record management.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param datums
	 *        the datum to insert
	 * @return the resulting stream metadata
	 */
	public static Map<NodeSourcePK, NodeDatumStreamMetadata> ingestDatumStream(Logger log,
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums, String timeZoneId) {
		final Map<NodeSourcePK, NodeDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement datumStmt = con
						.prepareCall("{? = call solardatm.store_datum(?,?,?,?,?)}")) {
					datumStmt.registerOutParameter(1, Types.OTHER);
					final Timestamp now = Timestamp.from(Instant.now());
					for ( GeneralNodeDatum d : datums ) {
						final GeneralDatumSamples s = d.getSamples();
						if ( s == null || s.isEmpty() ) {
							continue;
						}
						if ( log != null ) {
							log.debug("Inserting Datum {}", d);
						}

						NodeSourcePK nspk = new NodeSourcePK(d.getNodeId(), d.getSourceId());
						datumStmt.setTimestamp(2,
								Timestamp.from(Instant.ofEpochMilli(d.getCreated().getMillis())));
						datumStmt.setObject(3, nspk.getNodeId());
						datumStmt.setString(4, nspk.getSourceId());
						datumStmt.setTimestamp(5, now);

						String json = JsonUtils.getJSONString(s, null);
						datumStmt.setString(6, json);
						datumStmt.execute();

						Object id = datumStmt.getObject(1);
						UUID streamId = (id instanceof UUID ? (UUID) id
								: id != null ? UUID.fromString(id.toString()) : null);
						result.computeIfAbsent(nspk, k -> {
							return createMetadata(streamId, timeZoneId, datums, k);
						});
					}
				}
				return null;
			}
		});
		return result;
	}

	/**
	 * Get all available datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<Datum> listDatum(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query("SELECT * FROM solardatm.da_datm ORDER BY stream_id, ts",
				DatumEntityRowMapper.INSTANCE);
	}

	/**
	 * Get all available aggregate datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param kind
	 *        the aggregation kind to load, e.g. {@code Hour}, {@code Day}, or
	 *        {@code Month}
	 * @return the results, never {@literal null}
	 */
	public static List<AggregateDatum> listAggregateDatum(JdbcOperations jdbcTemplate,
			Aggregation kind) {
		String tableName;
		RowMapper<AggregateDatum> mapper;
		switch (kind) {
			case Day:
				tableName = "daily";
				mapper = AggregateDatumEntityRowMapper.DAY_INSTANCE;
				break;

			case Month:
				tableName = "monthly";
				mapper = AggregateDatumEntityRowMapper.MONTH_INSTANCE;
				break;

			default:
				tableName = "hourly";
				mapper = AggregateDatumEntityRowMapper.HOUR_INSTANCE;
		}
		return jdbcTemplate.query(String.format(
				"SELECT * FROM solardatm.agg_datm_%s ORDER BY stream_id, ts_start", tableName), mapper);
	}

	private static void debugStaleAggregateDatumTable(Logger log, JdbcOperations jdbcTemplate,
			String msg) {
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solardatm.agg_stale_datm ORDER BY ts_start, stream_id");
		log.debug("{}:\n{}", msg, staleRows.stream().map(e -> e.toString()).collect(joining("\n")));
	}

	/**
	 * Call the {@code solardatm.process_one_agg_stale_datm} stored procedure to
	 * compute aggregate data.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param kinds
	 *        the kinds of stale aggregate records to process; e.g.
	 *        {@code Hour}, {@code Day}, or {@code Month}
	 */
	public static void processStaleAggregateDatum(Logger log, JdbcOperations jdbcTemplate,
			Set<Aggregation> kinds) {
		debugStaleAggregateDatumTable(log, jdbcTemplate, "Stale datum at start");

		List<Aggregation> sortedKinds = kinds.stream().sorted(Aggregation::compareLevel)
				.collect(Collectors.toList());

		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{call solardatm.process_one_agg_stale_datm(?)}")) {
					for ( Aggregation kind : sortedKinds ) {
						int processed = processStaleAggregateKind(log, kind.getKey(), cs);
						log.debug("Processed {} stale {} datum", processed, kind.getKey());
						debugStaleAggregateDatumTable(log, jdbcTemplate,
								"Stale datum after process " + kind.getKey());
					}
				}
				return null;
			}
		});
	}

	/**
	 * Call the {@code solardatm.process_one_agg_stale_datm} stored procedure to
	 * compute aggregate data for all aggregate kinds.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @see #processStaleAggregateDatum(Logger, JdbcOperations, Set)
	 */
	public static void processStaleAggregateDatum(Logger log, JdbcOperations jdbcTemplate) {
		processStaleAggregateDatum(log, jdbcTemplate,
				EnumSet.of(Aggregation.Hour, Aggregation.Day, Aggregation.Month));
	}

	private static int processStaleAggregateKind(Logger log, String kind, CallableStatement cs)
			throws SQLException {
		int processed = 0;
		while ( true ) {
			cs.setString(1, kind);
			if ( cs.execute() ) {
				try (ResultSet rs = cs.getResultSet()) {
					if ( rs.next() ) {
						ObjectDatumId id = ObjectDatumIdRowMapper.INSTANCE.mapRow(rs, 1);
						log.debug("Processed stale agg row: {}", id);
						processed++;
					} else {
						break;
					}
				}
			} else {
				break;
			}
		}
		return processed;
	}

}
