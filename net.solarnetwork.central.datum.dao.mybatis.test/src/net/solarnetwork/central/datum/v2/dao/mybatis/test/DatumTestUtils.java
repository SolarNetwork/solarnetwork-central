/* ==================================================================
 * DatumTestUtils.java - 30/10/2020 2:26:18 pm
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

package net.solarnetwork.central.datum.v2.dao.mybatis.test;

import static java.lang.String.format;
import static net.solarnetwork.util.JsonUtils.getJSONString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumHourlyEntity;
import net.solarnetwork.central.datum.v2.dao.StaleAggregateDatumEntity;
import net.solarnetwork.central.datum.v2.dao.jdbc.AuditDatumHourlyEntityRowMapper;
import net.solarnetwork.central.datum.v2.dao.jdbc.StaleAggregateDatumEntityRowMapper;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.util.JsonUtils;

/**
 * Helper methods for datum tests.
 * 
 * @author matt
 * @version 1.0
 */
public final class DatumTestUtils {

	private DatumTestUtils() {
		// don't construct me
	}

	/** Regex for a line starting with a {@literal #} comment character. */
	public static final Pattern COMMENT = Pattern.compile("\\s*#");

	/**
	 * Create a {@link Matcher} for an array of {@link BigDecimal} values.
	 * 
	 * @param nums
	 *        the string numbers, which will be parsed as {@link BigDecimal}
	 *        instances
	 * @return the matcher
	 */
	public static Matcher<BigDecimal[]> arrayOfDecimals(String... nums) {
		BigDecimal[] vals = new BigDecimal[nums.length];
		for ( int i = 0; i < nums.length; i++ ) {
			vals[i] = new BigDecimal(nums[i]);
		}
		return Matchers.arrayContaining(vals);
	}

	/**
	 * Load JSON datum from a classpath resource.
	 * 
	 * <p>
	 * This method loads JSON datum records from a resource, with one JSON datum
	 * object per line. Empty lines or those starting with a {@literal #}
	 * character are ignored. An example JSON datum looks like this:
	 * </p>
	 * 
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","created":"2020-06-01T12:00:00Z","samples":{"i":{"x":1.2},"a":{"w":100}}}</code>
	 * </pre>
	 * 
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 */
	public static List<GeneralNodeDatum> loadJsonDatumResource(String resource, Class<?> clazz)
			throws IOException {
		List<GeneralNodeDatum> result = new ArrayList<>();
		int row = 0;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				row++;
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}
				GeneralNodeDatum d = JsonUtils.getObjectFromJSON(line, GeneralNodeDatum.class);
				assertThat(format("Parsed JSON datum in row %d", row), d, notNullValue());
				result.add(d);
			}
		}
		return result;
	}

	/**
	 * Load JSON datum auxiliary from a classpath resource.
	 * 
	 * <p>
	 * This method loads JSON datum auxiliary records from a resource, with one
	 * JSON datum object per line. Empty lines or those starting with a
	 * {@literal #} character are ignored. An example JSON datum looks like
	 * this:
	 * </p>
	 * 
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","type":"Reset","created":"2020-06-01T12:00:00Z","final":{"a":{"w":100}},"start":{"a":{"w":10}}}</code>
	 * </pre>
	 * 
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 */
	public static List<GeneralNodeDatumAuxiliary> loadJsonDatumAuxiliaryResource(String resource,
			Class<?> clazz) throws IOException {
		List<GeneralNodeDatumAuxiliary> result = new ArrayList<>();
		int row = 0;
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				row++;
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}
				GeneralNodeDatumAuxiliary d = JsonUtils.getObjectFromJSON(line,
						GeneralNodeDatumAuxiliary.class);
				assertThat(format("Parsed JSON datum auxiliary in row %d", row), d, notNullValue());
				result.add(d);
			}
		}
		return result;
	}

	/**
	 * Create a {@link NodeDatumStreamMetadata} out of a collection of
	 * {@link GeneralNodeDatum} instances.
	 * 
	 * @param datums
	 *        the datums
	 * @param nspk
	 *        the specific node+source to create the metadata for
	 * @return the metadata
	 */
	public static NodeDatumStreamMetadata createMetadata(Iterable<GeneralNodeDatum> datums,
			NodeSourcePK nspk) {
		return createMetadata(UUID.randomUUID(), datums, nspk);
	}

	/**
	 * Create a {@link NodeDatumStreamMetadata} out of a collection of
	 * {@link GeneralNodeDatum} instances.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param datums
	 *        the datums
	 * @param nspk
	 *        the specific node+source to create the metadata for
	 * @return the metadata
	 */
	public static NodeDatumStreamMetadata createMetadata(UUID streamId,
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
		return new BasicNodeDatumStreamMetadata(streamId, nspk.getNodeId(), nspk.getSourceId(),
				iNames.isEmpty() ? null : iNames.toArray(new String[iNames.size()]),
				aNames.isEmpty() ? null : aNames.toArray(new String[aNames.size()]),
				sNames.isEmpty() ? null : sNames.toArray(new String[sNames.size()]));
	}

	/**
	 * Insert a set of datum into the {@literal da_datm} table.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param datums
	 *        the datum to insert
	 * @return the resulting stream metadata
	 */
	public static Map<NodeSourcePK, NodeDatumStreamMetadata> insertDatumStream(Logger log,
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums) {
		final Map<NodeSourcePK, NodeDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"insert into solardatm.da_datm (stream_id,ts,received,data_i,data_a,data_s,data_t) "
								+ "VALUES (?::uuid,?,?,?::numeric[],?::numeric[],?::text[],?::text[])")) {
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
						NodeDatumStreamMetadata meta = result.computeIfAbsent(nspk, k -> {
							return createMetadata(datums, k);
						});
						datumStmt.setString(1, meta.getStreamId().toString());
						datumStmt.setTimestamp(2,
								Timestamp.from(Instant.ofEpochMilli(d.getCreated().getMillis())));
						datumStmt.setTimestamp(3, now);

						String[] iNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
						if ( iNames == null || iNames.length < 1 ) {
							datumStmt.setNull(4, Types.OTHER);
						} else {
							BigDecimal[] numbers = new BigDecimal[iNames.length];
							for ( int i = 0; i < iNames.length; i++ ) {
								numbers[i] = s.getInstantaneousSampleBigDecimal(iNames[i]);
							}
							Array iArray = con.createArrayOf("NUMERIC", numbers);
							datumStmt.setArray(4, iArray);
						}

						String[] aNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
						if ( aNames == null || aNames.length < 1 ) {
							datumStmt.setNull(5, Types.OTHER);
						} else {
							BigDecimal[] numbers = new BigDecimal[aNames.length];
							for ( int i = 0; i < aNames.length; i++ ) {
								numbers[i] = s.getAccumulatingSampleBigDecimal(aNames[i]);
							}
							Array aArray = con.createArrayOf("NUMERIC", numbers);
							datumStmt.setArray(5, aArray);
						}

						String[] sNames = meta.propertyNamesForType(GeneralDatumSamplesType.Status);
						if ( sNames == null || sNames.length < 1 ) {
							datumStmt.setNull(6, Types.OTHER);
						} else {
							String[] strings = new String[sNames.length];
							for ( int i = 0; i < sNames.length; i++ ) {
								strings[i] = s.getStatusSampleString(sNames[i]);
							}
							Array aArray = con.createArrayOf("TEXT", strings);
							datumStmt.setArray(6, aArray);
						}

						Set<String> tags = s.getTags();
						if ( tags == null || tags.isEmpty() ) {
							datumStmt.setNull(7, Types.OTHER);
						} else {
							String[] strings = tags.toArray(new String[tags.size()]);
							Array aArray = con.createArrayOf("TEXT", strings);
							datumStmt.setArray(7, aArray);
						}

						datumStmt.execute();
					}
				}
				try (PreparedStatement metaStmt = con.prepareStatement(
						"insert into solardatm.da_datm_meta (stream_id,node_id,source_id,names_i,names_a,names_s) "
								+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])")) {
					for ( NodeDatumStreamMetadata meta : result.values() ) {
						if ( log != null ) {
							log.debug("Inserting NodeDatumStreamMetadata {}", meta);
						}
						metaStmt.setString(1, meta.getStreamId().toString());
						metaStmt.setObject(2, meta.getNodeId());
						metaStmt.setString(3, meta.getSourceId());

						String[] iNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
						if ( iNames == null || iNames.length < 1 ) {
							metaStmt.setNull(4, Types.OTHER);
						} else {
							Array iArray = con.createArrayOf("TEXT", iNames);
							metaStmt.setArray(4, iArray);
						}

						String[] aNames = meta
								.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
						if ( aNames == null || aNames.length < 1 ) {
							metaStmt.setNull(5, Types.OTHER);
						} else {
							Array aArray = con.createArrayOf("TEXT", aNames);
							metaStmt.setArray(5, aArray);
						}

						String[] sNames = meta.propertyNamesForType(GeneralDatumSamplesType.Status);
						if ( sNames == null || sNames.length < 1 ) {
							metaStmt.setNull(6, Types.OTHER);
						} else {
							Array aArray = con.createArrayOf("TEXT", sNames);
							metaStmt.setArray(6, aArray);
						}

						metaStmt.execute();
					}
				}
				return null;
			}
		});
		return result;
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
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums) {
		final Map<NodeSourcePK, NodeDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement datumStmt = con
						.prepareCall("{? = call solardatm.store_datum(?,?,?,?,?)}")) {
					datumStmt.registerOutParameter(1, Types.VARCHAR);
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

						UUID streamId = UUID.fromString(datumStmt.getString(1));
						result.computeIfAbsent(nspk, k -> {
							return createMetadata(streamId, datums, k);
						});
					}
				}
				return null;
			}
		});
		return result;
	}

	/**
	 * Insert auxiliary datum records for a given stream.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param streamId
	 *        the stream ID to use
	 * @param datums
	 *        the datum to insert
	 */
	public static void insertDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate, UUID streamId,
			List<GeneralNodeDatumAuxiliary> datums) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"insert into solardatm.da_datm_aux (stream_id,ts,atype,jdata_af,jdata_as) "
								+ "VALUES (?::uuid,?,?::solardatm.da_datm_aux_type,?::jsonb,?::jsonb)")) {
					datumStmt.setString(1, streamId.toString());
					for ( GeneralNodeDatumAuxiliary d : datums ) {
						if ( log != null ) {
							log.debug("Inserting GeneralNodeDatumAuxiliary {}; {} -> {}", d.getId(),
									d.getSampleDataFinal(), d.getSampleDataStart());
						}
						datumStmt.setTimestamp(2,
								Timestamp.from(Instant.ofEpochMilli(d.getCreated().getMillis())));
						datumStmt.setString(3, d.getType().name());
						datumStmt.setString(4, getJSONString(d.getSamplesFinal().getA(), null));
						datumStmt.setString(5, getJSONString(d.getSamplesStart().getA(), null));
						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	/**
	 * Get the available stale aggregate datum records.
	 * 
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAggregateDatumEntity> staleAggregateDatumStreams(
			JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, agg_kind, created FROM solardatm.agg_stale_datm ORDER BY agg_kind, ts_start, stream_id",
				StaleAggregateDatumEntityRowMapper.INSTANCE);
	}

	/**
	 * Get the available stale aggregate datum records.
	 * 
	 * @param type
	 *        the type of stale aggregate records to get
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAggregateDatumEntity> staleAggregateDatumStreams(JdbcOperations jdbcTemplate,
			Aggregation type) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, agg_kind, created FROM solardatm.agg_stale_datm WHERE agg_kind = ? ORDER BY ts_start, stream_id",
				StaleAggregateDatumEntityRowMapper.INSTANCE, type.getKey());
	}

	/**
	 * Get the available stale aggregate datum records.
	 * 
	 * @return the results, never {@literal null}
	 */
	public static List<AuditDatumHourlyEntity> auditDatumHourly(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, datum_count, prop_count, datum_q_count FROM solardatm.aud_datm_hourly ORDER BY stream_id, ts_start",
				AuditDatumHourlyEntityRowMapper.INSTANCE);
	}

}
