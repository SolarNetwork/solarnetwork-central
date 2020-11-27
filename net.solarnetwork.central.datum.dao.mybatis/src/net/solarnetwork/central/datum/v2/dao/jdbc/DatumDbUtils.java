/* ==================================================================
 * DatumDbUtils.java - 23/11/2020 1:39:27 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import static java.util.stream.Collectors.joining;
import static net.solarnetwork.central.datum.v2.domain.DatumProperties.propertiesOf;
import static net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics.statisticsOf;
import static net.solarnetwork.util.JsonUtils.getJSONString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.AuditDatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamFilterResults;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.TypedDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.domain.GeneralDatumSamplesType;
import net.solarnetwork.util.JsonUtils;

/**
 * Utilities for working with datum at the database level.
 * 
 * <p>
 * These utilities are primarily designed to support unit testing.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public final class DatumDbUtils {

	private DatumDbUtils() {
		// don't construct me
	}

	/** Regex for a line starting with a {@literal #} comment character. */
	public static final Pattern COMMENT = Pattern.compile("^\\s*#");

	/**
	 * Regex for a line starting with a {@literal --} SQL style comment
	 * character.
	 */
	public static final Pattern SQL_COMMENT = Pattern.compile("^\\s*--");

	/** Regex for a line containing {@literal "type":"Reset"}. */
	public static final Pattern AUX = Pattern.compile("\"type\"\\s*:\\s*\"Reset\"");

	/**
	 * Regex for a line containing {@literal "kind":"X"} where {@literal X} is
	 * one of {@literal Hour}, {@literal Day}, or {@literal Month}.
	 */
	public static final Pattern AGG = Pattern.compile("\"kind\"\\s*:\\s*\"(?:Hour|Day|Month)\"");

	/** Sort for typed datum by timestamp. */
	public static final Comparator<TypedDatumEntity> SORT_TYPED_DATUM_BY_TS = new SortTypedDatumByTimestamp();

	private static class SortTypedDatumByTimestamp implements Comparator<TypedDatumEntity> {

		@Override
		public int compare(TypedDatumEntity o1, TypedDatumEntity o2) {
			int c = o1.getTimestamp().compareTo(o2.getTimestamp());
			if ( c == 0 ) {
				c = Integer.compare(o1.getType(), o2.getType());
			}
			return c;
		}
	}

	/** UUID sorter that matches how Postgres orders UUIDS (byte by byte). */
	public static final Comparator<UUID> UUID_STRING_ORDER = new UuidByteOrder();

	private static class UuidByteOrder implements Comparator<UUID> {

		@Override
		public int compare(UUID o1, UUID o2) {
			return o1.toString().compareTo(o2.toString());
		}

	}

	/**
	 * Extract a sorted set of stream IDs from filter results.
	 * 
	 * @param results
	 *        the results to extract the stream IDs from
	 * @param comparator
	 *        the comparator to use; for example {@link #UUID_STRING_ORDER}
	 * @return the sorted set
	 */
	public static SortedSet<UUID> sortedStreamIds(DatumStreamFilterResults results,
			Comparator<UUID> comparator) {
		return results.metadataStreamIds().stream().collect(Collectors.toCollection(() -> {
			return new TreeSet<>(comparator);
		}));
	}

	/**
	 * Create a {@link ReadingDatum} out of statistic data.
	 * 
	 * @param streamId
	 *        the stream ID
	 * @param agg
	 *        the aggregate
	 * @param start
	 *        the start date
	 * @param end
	 *        the end date
	 * @param stats
	 *        the aggregate statistics
	 * @return the datum
	 */
	public static ReadingDatum readingWith(UUID streamId, Aggregation agg, ZonedDateTime start,
			ZonedDateTime end, BigDecimal[]... stats) {
		BigDecimal[] acc = new BigDecimal[stats.length];
		for ( int i = 0; i < stats.length; i++ ) {
			acc[i] = stats[i][0];
		}
		return new ReadingDatumEntity(streamId, start.toInstant(), agg, end.toInstant(),
				propertiesOf(null, acc, null, null), statisticsOf(null, stats));
	}

	/**
	 * Extract all elements of a specific class from a list.
	 * 
	 * @param <T>
	 *        the type of element to extract
	 * @param list
	 *        the list to extract from
	 * @param clazz
	 *        the class of elements to extract
	 * @return the list, never {@literal null}
	 */
	public static <T> List<T> elementsOf(List<?> list, Class<T> clazz) {
		return list.stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
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
		return elementsOf(loadJsonDatumAndAuxiliaryResource(resource, clazz), GeneralNodeDatum.class);
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
		return elementsOf(loadJsonDatumAndAuxiliaryResource(resource, clazz),
				GeneralNodeDatumAuxiliary.class);
	}

	/**
	 * Load JSON datum and datum auxiliary objects from a classpath resource.
	 * 
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 * @see #loadJsonDatumAndAuxiliaryResource(String, Class, Function,
	 *      Consumer)
	 */
	public static List<?> loadJsonDatumAndAuxiliaryResource(String resource, Class<?> clazz)
			throws IOException {
		return loadJsonDatumAndAuxiliaryResource(resource, clazz, null, null);
	}

	/**
	 * Load JSON datum and datum auxiliary objects from a classpath resource.
	 * 
	 * <p>
	 * This method loads JSON datum and datum auxiliary records from a resource,
	 * with one JSON datum object per line. Empty lines or those starting with a
	 * {@literal #} character are ignored. An example JSON datum looks like
	 * this:
	 * </p>
	 * 
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","created":"2020-06-01T12:00:00Z","samples":{"i":{"x":1.2},"a":{"w":100}}}</code>
	 * </pre>
	 * 
	 * <p>
	 * An example datum auxiliary looks like this:
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
	 * @param datumMapper
	 *        optional consumer to adjust datum with
	 * @param auxMapper
	 *        optional consumer to adjust auxiliary datum with
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 */
	public static List<?> loadJsonDatumAndAuxiliaryResource(String resource, Class<?> clazz,
			Consumer<GeneralNodeDatum> datumMapper, Consumer<GeneralNodeDatumAuxiliary> auxMapper)
			throws IOException {
		List<Object> result = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}
				if ( AUX.matcher(line).find() ) {
					GeneralNodeDatumAuxiliary d = JsonUtils.getObjectFromJSON(line,
							GeneralNodeDatumAuxiliary.class);
					if ( d != null ) {
						if ( auxMapper != null ) {
							auxMapper.accept(d);
						}
						result.add(d);
					}
				} else {
					GeneralNodeDatum d = JsonUtils.getObjectFromJSON(line, GeneralNodeDatum.class);
					if ( d != null ) {
						if ( datumMapper != null ) {
							datumMapper.accept(d);
						}
						result.add(d);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Load JSON aggregate datum objects from a classpath resource.
	 * 
	 * <p>
	 * This method loads JSON aggregate datum records from a resource, with one
	 * JSON datum object per line. Empty lines or those starting with a
	 * {@literal #} character are ignored. An example JSON aggregate datum looks
	 * like this:
	 * </p>
	 * 
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","ts":"2020-06-01T00:00:00Z","kind":"Hour","samples":{"i":{"x":1.2,"y":2.1},"a":{"w":100}},"stats":{"i":{"x":[6,1.1,3.1],"y":[6,2.0,7.1]},"ra":{"w":[100,200,100]}}}</code>
	 * </pre>
	 * 
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @param metadataProvider
	 *        the metadata provider
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 * @see DatumJsonUtils#parseAggregateDatum(JsonParser,
	 *      ObjectDatumStreamMetadataProvider)
	 */
	public static List<AggregateDatum> loadJsonAggregateDatumResource(String resource, Class<?> clazz,
			ObjectDatumStreamMetadataProvider metadataProvider) throws IOException {
		return loadJsonAggregateDatumResource(resource, clazz, metadataProvider, null);
	}

	/**
	 * Load JSON aggregate datum objects from a classpath resource.
	 * 
	 * <p>
	 * This method loads JSON aggregate datum records from a resource, with one
	 * JSON datum object per line. Empty lines or those starting with a
	 * {@literal #} character are ignored. An example JSON aggregate datum looks
	 * like this:
	 * </p>
	 * 
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","ts":"2020-06-01T00:00:00Z","kind":"Hour","samples":{"i":{"x":1.2,"y":2.1},"a":{"w":100}},"stats":{"i":{"x":[6,1.1,3.1],"y":[6,2.0,7.1]},"ra":{"w":[100,200,100]}}}</code>
	 * </pre>
	 * 
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @param metadataProvider
	 *        the metadata provider
	 * @param an
	 *        optional function to map the parsed datum with
	 * @return the loaded data, never {@literal null}
	 * @throws IOException
	 *         if the resource cannot be found or parsed correctly
	 * @see DatumJsonUtils#parseAggregateDatum(JsonParser,
	 *      ObjectDatumStreamMetadataProvider)
	 */
	public static List<AggregateDatum> loadJsonAggregateDatumResource(String resource, Class<?> clazz,
			ObjectDatumStreamMetadataProvider metadataProvider,
			Function<AggregateDatum, AggregateDatum> mapper) throws IOException {

		List<AggregateDatum> result = new ArrayList<>();
		JsonFactory factory = new JsonFactory();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), Charset.forName("UTF-8")))) {
			while ( true ) {
				String line = r.readLine();
				if ( line == null ) {
					break;
				}
				if ( line.isEmpty() || COMMENT.matcher(line).find() ) {
					// skip empty/comment line
					continue;
				}
				if ( AGG.matcher(line).find() ) {
					JsonParser parser = factory.createParser(line);
					AggregateDatum d = DatumJsonUtils.parseAggregateDatum(parser, metadataProvider);
					if ( mapper != null ) {
						d = mapper.apply(d);
					}
					if ( d != null ) {
						result.add(d);
					}
				}
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
			String timeZoneId, NodeSourcePK nspk) {
		return createMetadata(UUID.randomUUID(), timeZoneId, datums, nspk);
	}

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
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums, String timeZoneId) {
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
							return createMetadata(datums, timeZoneId, k);
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
				insertObjectDatumStreamMetadata(log, con, result.values());
				return null;
			}
		});
		return result;
	}

	/**
	 * Insert a set of datum into the {@literal da_datm} table.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param resource
	 *        the datum resource to parse and insert
	 * @param clazz
	 *        the resource class
	 * @param timeZoneId
	 *        the datum time zone to use
	 * @return the resulting stream metadata
	 * @throws IOException
	 *         if any error occurs parsing the resource
	 */
	public static Map<NodeSourcePK, NodeDatumStreamMetadata> insertDatumStreamWithAuxiliary(Logger log,
			JdbcOperations jdbcTemplate, String resource, Class<?> clazz, String timeZoneId)
			throws IOException {
		List<?> data = loadJsonDatumAndAuxiliaryResource(resource, clazz);
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, NodeDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				timeZoneId);
		UUID streamId = null;
		if ( !meta.isEmpty() ) {
			streamId = meta.values().iterator().next().getStreamId();
			if ( !auxDatums.isEmpty() ) {
				insertDatumAuxiliary(log, jdbcTemplate, streamId, auxDatums);
			}
		}
		return meta;
	}

	/**
	 * Insert a set of datum for a single stream into the {@literal da_datm}
	 * table.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param resource
	 *        the datum resource to parse and insert
	 * @param clazz
	 *        the resource class
	 * @param timeZoneId
	 *        the datum time zone to use
	 * @return the resulting stream ID
	 */
	public static UUID insertOneDatumStreamWithAuxiliary(Logger log, JdbcOperations jdbcTemplate,
			String resource, Class<?> clazz, String timeZoneId) {
		try {
			return insertDatumStreamWithAuxiliary(log, jdbcTemplate, resource, clazz, timeZoneId)
					.values().iterator().next().getStreamId();
		} catch ( IOException e ) {
			throw new RuntimeException("Unable to load datum resource " + resource, e);
		}

	}

	private static String insertMetaStmt(String kind) {
		StringBuilder buf = new StringBuilder();
		buf.append("insert into solardatm.da_");
		buf.append(kind);
		buf.append("_meta (stream_id,");
		if ( kind.startsWith("loc") ) {
			buf.append("loc_id");
		} else {
			buf.append("node_id");
		}
		buf.append(",source_id,names_i,names_a,names_s,jdata) ");
		buf.append("VALUES (?::uuid,?,?,?::text[],?::text[],?::text[],?::jsonb)");
		return buf.toString();
	}

	/**
	 * Insert node or location datum metadata.
	 * 
	 * @param log
	 *        an optional logger
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param metas
	 *        the metadata to insert
	 */
	public static void insertObjectDatumStreamMetadata(Logger log, JdbcOperations jdbcTemplate,
			Iterable<? extends ObjectDatumStreamMetadata> metas) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				insertObjectDatumStreamMetadata(log, con, metas);
				return null;
			}
		});
	}

	/**
	 * Insert datum or location datum stream metadata.
	 * 
	 * @param log
	 *        an optional logger
	 * @param con
	 *        the JDBC connection to use
	 * @param metas
	 *        the metadata to insert, can be either ndoe or location
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	public static void insertObjectDatumStreamMetadata(Logger log, Connection con,
			Iterable<? extends ObjectDatumStreamMetadata> metas) throws SQLException {
		try (PreparedStatement nodeMetaStmt = con.prepareStatement(insertMetaStmt("datm"));
				PreparedStatement locMetaStmt = con.prepareStatement(insertMetaStmt("loc_datm"))) {
			for ( ObjectDatumStreamMetadata meta : metas ) {
				if ( log != null ) {
					log.debug("Inserting ObjectDatumStreamMetadata {}", meta);
				}
				@SuppressWarnings("resource")
				PreparedStatement metaStmt = (meta instanceof LocationDatumStreamMetadata ? locMetaStmt
						: nodeMetaStmt);
				metaStmt.setString(1, meta.getStreamId().toString());
				metaStmt.setObject(2, meta.getObjectId());
				metaStmt.setString(3, meta.getSourceId());

				String[] iNames = meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous);
				if ( iNames == null || iNames.length < 1 ) {
					metaStmt.setNull(4, Types.OTHER);
				} else {
					Array iArray = con.createArrayOf("TEXT", iNames);
					metaStmt.setArray(4, iArray);
				}

				String[] aNames = meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating);
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

				String json = meta.getMetaJson();
				metaStmt.setString(7, json);

				metaStmt.execute();
			}
		}
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
			Iterable<GeneralNodeDatumAuxiliary> datums) {
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
	 * Insert datum records.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	public static void insertDatum(Logger log, JdbcOperations jdbcTemplate, Iterable<Datum> datums) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement("INSERT INTO solardatm.da_datm "
						+ "(stream_id,ts,received,data_i,data_a,data_s,data_t) VALUES "
						+ "(?::uuid,?,?,?::numeric[],?::numeric[],?::text[],?::text[])")) {
					datumStmt.setTimestamp(3, Timestamp.from(Instant.now()));
					for ( Datum d : datums ) {
						if ( log != null ) {
							log.debug("Inserting Datum: {}", d);
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));

						DatumProperties props = d.getProperties();
						if ( props != null && props.getInstantaneousLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", props.getInstantaneous());
							datumStmt.setArray(4, array);
							array.free();
						} else {
							datumStmt.setNull(4, Types.ARRAY);
						}
						if ( props != null && props.getAccumulatingLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", props.getAccumulating());
							datumStmt.setArray(5, array);
							array.free();
						} else {
							datumStmt.setNull(5, Types.ARRAY);
						}
						if ( props != null && props.getStatusLength() > 0 ) {
							Array array = con.createArrayOf("TEXT", props.getStatus());
							datumStmt.setArray(6, array);
							array.free();
						} else {
							datumStmt.setNull(6, Types.ARRAY);
						}
						if ( props != null && props.getTagsLength() > 0 ) {
							Array array = con.createArrayOf("TEXT", props.getTags());
							datumStmt.setArray(7, array);
							array.free();
						} else {
							datumStmt.setNull(7, Types.ARRAY);
						}

						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	private static String insertAggStmt(Aggregation kind) {
		StringBuilder buf = new StringBuilder();
		buf.append("insert into solardatm.agg_datm_");
		switch (kind) {
			case Day:
				buf.append("daily");
				break;

			case Month:
				buf.append("monthly");
				break;

			default:
				buf.append("hourly");
				break;
		}
		buf.append(" (stream_id,ts_start,data_i,data_a,data_s,data_t,stat_i,read_a) ");
		buf.append(
				"VALUES (?::uuid,?,?::numeric[],?::numeric[],?::text[],?::text[],?::numeric[][],?::numeric[][])");
		return buf.toString();
	}

	/**
	 * Insert aggregate datum records.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	public static void insertAggregateDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<AggregateDatum> datums) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement hourStmt = con.prepareStatement(insertAggStmt(Aggregation.Hour));
						PreparedStatement dayStmt = con.prepareStatement(insertAggStmt(Aggregation.Day));
						PreparedStatement monthStmt = con
								.prepareStatement(insertAggStmt(Aggregation.Month))) {
					for ( AggregateDatum d : datums ) {
						Aggregation kind = (d.getAggregation() != null ? d.getAggregation()
								: Aggregation.Hour);
						PreparedStatement datumStmt;
						switch (kind) {
							case Day:
								datumStmt = dayStmt;
								break;

							case Month:
								datumStmt = monthStmt;
								break;

							default:
								datumStmt = hourStmt;
								break;
						}
						if ( log != null ) {
							log.debug("Inserting {} AggregateDatum: {}", kind, d);
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));

						DatumProperties props = d.getProperties();
						if ( props != null && props.getInstantaneousLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", props.getInstantaneous());
							datumStmt.setArray(3, array);
							array.free();
						} else {
							datumStmt.setNull(3, Types.ARRAY);
						}
						if ( props != null && props.getAccumulatingLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", props.getAccumulating());
							datumStmt.setArray(4, array);
							array.free();
						} else {
							datumStmt.setNull(4, Types.ARRAY);
						}
						if ( props != null && props.getStatusLength() > 0 ) {
							Array array = con.createArrayOf("TEXT", props.getStatus());
							datumStmt.setArray(5, array);
							array.free();
						} else {
							datumStmt.setNull(5, Types.ARRAY);
						}
						if ( props != null && props.getTagsLength() > 0 ) {
							Array array = con.createArrayOf("TEXT", props.getTags());
							datumStmt.setArray(6, array);
							array.free();
						} else {
							datumStmt.setNull(6, Types.ARRAY);
						}

						DatumPropertiesStatistics stats = d.getStatistics();
						if ( stats != null && stats.getInstantaneousLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", stats.getInstantaneous());
							datumStmt.setArray(7, array);
							array.free();
						} else {
							datumStmt.setNull(7, Types.ARRAY);
						}
						if ( stats != null && stats.getAccumulatingLength() > 0 ) {
							Array array = con.createArrayOf("NUMERIC", stats.getAccumulating());
							datumStmt.setArray(8, array);
							array.free();
						} else {
							datumStmt.setNull(8, Types.ARRAY);
						}

						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	/**
	 * Insert audit datum records.
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
	public static void insertAuditDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<AuditDatum> datums) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement hourStmt = con
						.prepareStatement(insertAuditStmt(Aggregation.Hour));
						PreparedStatement dayStmt = con
								.prepareStatement(insertAuditStmt(Aggregation.Day));
						PreparedStatement monthStmt = con
								.prepareStatement(insertAuditStmt(Aggregation.Month));
						PreparedStatement accStmt = con
								.prepareStatement(insertAuditStmt(Aggregation.RunningTotal))) {

					for ( AuditDatum d : datums ) {
						if ( log != null ) {
							log.debug("Inserting {} AuditDatum; {}", d.getAggregation(), d);
						}
						PreparedStatement datumStmt;
						switch (d.getAggregation()) {
							case Hour:
								datumStmt = hourStmt;
								break;

							case Day:
								datumStmt = dayStmt;
								break;

							case Month:
								datumStmt = monthStmt;
								break;

							default:
								datumStmt = accStmt;
								break;
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));

						switch (d.getAggregation()) {
							case Hour:
								datumStmt.setObject(3, d.getDatumPropertyCount());
								datumStmt.setObject(4, d.getDatumQueryCount());
								datumStmt.setObject(5, d.getDatumCount());
								break;

							case Day:
								datumStmt.setObject(3, d.getDatumPropertyCount());
								datumStmt.setObject(4, d.getDatumQueryCount());
								datumStmt.setObject(5, d.getDatumCount());
								datumStmt.setObject(6, d.getDatumHourlyCount());
								datumStmt.setBoolean(7,
										d.getDatumDailyCount().intValue() > 0 ? true : false);
								break;

							case Month:
								datumStmt.setObject(3, d.getDatumPropertyCount());
								datumStmt.setObject(4, d.getDatumQueryCount());
								datumStmt.setObject(5, d.getDatumCount());
								datumStmt.setObject(6, d.getDatumHourlyCount());
								datumStmt.setObject(7, d.getDatumDailyCount());
								datumStmt.setBoolean(8,
										d.getDatumMonthlyCount().intValue() > 0 ? true : false);
								break;

							default:
								datumStmt.setObject(3, d.getDatumCount());
								datumStmt.setObject(4, d.getDatumHourlyCount());
								datumStmt.setObject(5, d.getDatumDailyCount());
								datumStmt.setObject(6, d.getDatumMonthlyCount());
								break;
						}

						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	private static String insertAuditStmt(Aggregation kind) {
		StringBuilder buf = new StringBuilder();
		buf.append("insert into solardatm.");
		switch (kind) {
			case Hour:
				buf.append("aud_datm_hourly");
				break;

			case Day:
				buf.append("aud_datm_daily");
				break;

			case Month:
				buf.append("aud_datm_monthly");
				break;

			default:
				buf.append("aud_acc_datm_daily");
				break;
		}
		buf.append(" (stream_id,ts_start,");
		switch (kind) {
			case Hour:
				buf.append("prop_count,datum_q_count,datum_count");
				break;

			case Day:
				buf.append("prop_count,datum_q_count,datum_count,datum_hourly_count,datum_daily_pres");
				break;

			case Month:
				buf.append(
						"prop_count,datum_q_count,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres");
				break;

			default:
				buf.append("datum_count,datum_hourly_count,datum_daily_count,datum_monthly_count");
				break;
		}
		buf.append(") VALUES (?::uuid,?,");
		switch (kind) {
			case Hour:
				buf.append("?,?,?");
				break;

			case Day:
				buf.append("?,?,?,?,?");
				break;

			case Month:
				buf.append("?,?,?,?,?,?");
				break;

			default:
				buf.append("?,?,?,?");
				break;
		}
		buf.append(")");
		return buf.toString();
	}

	/**
	 * Ingest a set of datum auxiliary into the {@literal da_datm_aux} table,
	 * using the {@code solardatm.store_datum_aux()} stored procedure that
	 * includes side effects like "stale" record management.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	public static void ingestDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate,
			Iterable<GeneralNodeDatumAuxiliary> datums) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement datumStmt = con
						.prepareCall("{call solardatm.store_datum_aux(?,?,?,?,?,?,?,?)}")) {
					for ( GeneralNodeDatumAuxiliary d : datums ) {
						if ( log != null ) {
							log.debug("Inserting GeneralNodeDatumAuxiliary {}; {} -> {}", d.getId(),
									d.getSampleDataFinal(), d.getSampleDataStart());
						}
						datumStmt.setTimestamp(1,
								Timestamp.from(Instant.ofEpochMilli(d.getCreated().getMillis())));
						datumStmt.setObject(2, d.getNodeId());
						datumStmt.setString(3, d.getSourceId());
						datumStmt.setString(4, d.getType().name());
						datumStmt.setNull(5, Types.VARCHAR);
						datumStmt.setString(6, d.getSampleJsonFinal());
						datumStmt.setString(7, d.getSampleJsonStart());
						datumStmt.setNull(8, Types.VARCHAR);
						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	/**
	 * Ingest a set of datum auxiliary into the {@literal da_datm_aux} table,
	 * using the {@code solardatm.store_datum_aux()} stored procedure that
	 * includes side effects like "stale" record management.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	public static boolean moveDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate,
			GeneralNodeDatumAuxiliaryPK from, GeneralNodeDatumAuxiliary to) {
		return jdbcTemplate.execute(new ConnectionCallback<Boolean>() {

			@Override
			public Boolean doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement datumStmt = con
						.prepareCall("{? = call solardatm.move_datum_aux(?,?,?,?,?,?,?,?,?,?,?,?)}")) {
					if ( log != null ) {
						log.debug("Moving GeneralNodeDatumAuxiliary {} -> {}", from, to.getId());
					}
					datumStmt.registerOutParameter(1, Types.BOOLEAN);
					datumStmt.setTimestamp(2,
							Timestamp.from(Instant.ofEpochMilli(from.getCreated().getMillis())));
					datumStmt.setObject(3, from.getNodeId());
					datumStmt.setString(4, from.getSourceId());
					datumStmt.setString(5, from.getType().name());

					datumStmt.setTimestamp(6,
							Timestamp.from(Instant.ofEpochMilli(to.getCreated().getMillis())));
					datumStmt.setObject(7, to.getNodeId());
					datumStmt.setString(8, to.getSourceId());
					datumStmt.setString(9, to.getType().name());

					datumStmt.setNull(10, Types.VARCHAR);
					datumStmt.setString(11, to.getSampleJsonFinal());
					datumStmt.setString(12, to.getSampleJsonStart());
					datumStmt.setNull(13, Types.VARCHAR);
					datumStmt.execute();
					return datumStmt.getBoolean(1);
				}
			}
		});
	}

	/**
	 * Insert stale aggregate datum records.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param stales
	 *        the stale datum to insert
	 */
	public static void insertStaleAggregateDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<StaleAggregateDatum> stales) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"INSERT INTO solardatm.agg_stale_datm (stream_id,ts_start,agg_kind) VALUES (?::uuid,?,?)")) {
					for ( StaleAggregateDatum d : stales ) {
						if ( log != null ) {
							log.debug("Inserting {} StaleAggregateDatum: {}", d.getKind(), d);
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));
						datumStmt.setString(3, d.getKind().getKey());

						datumStmt.execute();
					}
				}
				return null;
			}
		});
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

	/**
	 * Insert stale aggregate datum records.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param stales
	 *        the stale datum to insert
	 */
	public static void insertStaleFluxDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<StaleFluxDatum> stales) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"INSERT INTO solardatm.agg_stale_flux (stream_id,agg_kind) VALUES (?::uuid,?)")) {
					for ( StaleFluxDatum d : stales ) {
						if ( log != null ) {
							log.debug("Inserting {} StaleFluxDatum: {}", d.getKind(), d.getStreamId());
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setString(2, d.getKind().getKey());

						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	/**
	 * Insert stale audit datum records.
	 * 
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param stales
	 *        the stale datum to insert
	 */
	public static void insertStaleAuditDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<StaleAuditDatum> stales) {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (PreparedStatement datumStmt = con.prepareStatement(
						"INSERT INTO solardatm.aud_stale_datm (stream_id,ts_start,aud_kind) VALUES (?::uuid,?,?)")) {
					for ( StaleAuditDatum d : stales ) {
						if ( log != null ) {
							log.debug("Inserting {} StaleAuditDatum: {}", d.getKind(), d);
						}
						datumStmt.setString(1, d.getStreamId().toString());
						datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));
						datumStmt.setString(3, d.getKind().getKey());

						datumStmt.execute();
					}
				}
				return null;
			}
		});
	}

	/**
	 * Call the {@code solardatm.process_one_aud_stale_datm} stored procedure to
	 * compute audit data.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @param kinds
	 *        the kinds of stale audit records to process; e.g. {@code None},
	 *        {@code Hour}, {@code Day}, or {@code Month}
	 */
	public static void processStaleAuditDatum(Logger log, JdbcOperations jdbcTemplate,
			Set<Aggregation> kinds) {
		debugStaleAuditDatumTable(log, jdbcTemplate, "Stale audit datum at start");

		List<Aggregation> sortedKinds = kinds.stream().sorted(Aggregation::compareLevel)
				.collect(Collectors.toList());

		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				try (CallableStatement cs = con
						.prepareCall("{? = call solardatm.process_one_aud_stale_datm(?)}")) {
					cs.registerOutParameter(1, Types.INTEGER);
					for ( Aggregation kind : sortedKinds ) {
						int processed = processStaleAuditKind(kind.getKey(), cs);
						log.debug("Processed {} stale {} audit datum", processed, kind.getKey());
						debugStaleAuditDatumTable(log, jdbcTemplate,
								"Stale audit datum after process " + kind.getKey());
					}
				}
				return null;
			}
		});
	}

	/**
	 * Call the {@code solardatm.process_one_aud_stale_datm} stored procedure to
	 * compute audit data for all aggregate kinds.
	 * 
	 * @param log
	 *        the logger to use
	 * @param jdbcTemplate
	 *        the JDBC template to use
	 * @see #processStaleAggregateDatum(Logger, JdbcOperations, Set)
	 */
	public static void processStaleAuditDatum(Logger log, JdbcOperations jdbcTemplate) {
		processStaleAuditDatum(log, jdbcTemplate,
				EnumSet.of(Aggregation.None, Aggregation.Hour, Aggregation.Day, Aggregation.Month));
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

	private static void debugStaleAuditDatumTable(Logger log, JdbcOperations jdbcTemplate, String msg) {
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solardatm.aud_stale_datm ORDER BY ts_start, stream_id");
		log.debug("{}:\n{}", msg, staleRows.stream().map(e -> e.toString()).collect(joining("\n")));
	}

	/**
	 * Get the metadata for a stream.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param streamId
	 *        the stream ID to get metadata for
	 * @return the metadata, or {@literal null}
	 */
	public static ObjectDatumStreamMetadata streamMetadata(JdbcOperations jdbcTemplate, UUID streamId) {
		List<ObjectDatumStreamMetadata> results = jdbcTemplate.query(
				"SELECT stream_id, obj_id, source_id, names_i, names_a, names_s, jdata, kind, time_zone FROM solardatm.find_metadata_for_stream(?::uuid)",
				ObjectDatumStreamMetadataRowMapper.INSTANCE, streamId);
		return (results.isEmpty() ? null : results.get(0));
	}

	/**
	 * Get the available datum auxiliary records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<DatumAuxiliary> listDatumAuxiliary(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts, atype, updated, notes, jdata_af, jdata_as, jmeta FROM solardatm.da_datm_aux ORDER BY stream_id, ts, atype",
				DatumAuxiliaryEntityRowMapper.INSTANCE);
	}

	/**
	 * Get the available stale aggregate datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAggregateDatum> listStaleAggregateDatum(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, agg_kind, created FROM solardatm.agg_stale_datm ORDER BY agg_kind, ts_start, stream_id",
				StaleAggregateDatumEntityRowMapper.INSTANCE);
	}

	/**
	 * Get the available stale aggregate datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param type
	 *        the type of stale aggregate records to get
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAggregateDatum> listStaleAggregateDatum(JdbcOperations jdbcTemplate,
			Aggregation type) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, agg_kind, created FROM solardatm.agg_stale_datm WHERE agg_kind = ? ORDER BY ts_start, stream_id",
				StaleAggregateDatumEntityRowMapper.INSTANCE, type.getKey());
	}

	/**
	 * Get the available stale audit datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAuditDatum> listStaleAuditDatum(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, aud_kind, created FROM solardatm.aud_stale_datm ORDER BY aud_kind, ts_start, stream_id",
				StaleAuditDatumEntityRowMapper.INSTANCE);
	}

	/**
	 * Get the available stale audit datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param type
	 *        the type of stale audit records to get
	 * @return the results, never {@literal null}
	 */
	public static List<StaleAuditDatum> listStaleAuditDatum(JdbcOperations jdbcTemplate,
			Aggregation type) {
		return jdbcTemplate.query(
				"SELECT stream_id, ts_start, aud_kind, created FROM solardatm.aud_stale_datm WHERE aud_kind = ? ORDER BY ts_start, stream_id",
				StaleAuditDatumEntityRowMapper.INSTANCE, type.getKey());
	}

	/**
	 * Get the available stale flux datum records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param type
	 *        the type of stale flux records to get
	 * @return the results, never {@literal null}
	 */
	public static List<StaleFluxDatum> listStaleFluxDatum(JdbcOperations jdbcTemplate,
			Aggregation type) {
		return jdbcTemplate.query(
				"SELECT stream_id, agg_kind FROM solardatm.agg_stale_flux WHERE agg_kind = ? ORDER BY stream_id",
				StaleFluxDatumRowMapper.INSTANCE, type.getKey());
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
	 * Get all available node metadata records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<NodeDatumStreamMetadata> listNodeMetadata(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id,m.node_id,source_id,names_i,names_a,names_s"
						+ ",jdata,'n'::CHARACTER AS kind,COALESCE(l.time_zone, 'UTC') AS time_zone "
						+ "FROM solardatm.da_datm_meta m "
						+ "LEFT OUTER JOIN solarnet.sn_node n ON n.node_id = m.node_id "
						+ "LEFT OUTER JOIN solarnet.sn_loc l ON l.id = n.loc_id ORDER BY node_id",
				ObjectDatumStreamMetadataRowMapper.NODE_INSTANCE);
	}

	/**
	 * Get all available node metadata records.
	 * 
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @return the results, never {@literal null}
	 */
	public static List<LocationDatumStreamMetadata> listLocationMetadata(JdbcOperations jdbcTemplate) {
		return jdbcTemplate.query(
				"SELECT stream_id,m.loc_id,source_id,names_i,names_a,names_s"
						+ ",jdata,'l'::CHARACTER AS kind,COALESCE(l.time_zone, 'UTC') AS time_zone "
						+ "FROM solardatm.da_loc_datm_meta m "
						+ "LEFT OUTER JOIN solarnet.sn_loc l ON l.id = m.loc_id ORDER BY loc_id",
				ObjectDatumStreamMetadataRowMapper.LOCATION_INSTANCE);
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
	public static List<AuditDatumEntity> listAuditDatum(JdbcOperations jdbcTemplate, Aggregation kind) {
		String tableName;
		RowMapper<AuditDatumEntity> mapper;
		switch (kind) {
			case Day:
				tableName = "aud_datm_daily";
				mapper = AuditDatumDailyEntityRowMapper.INSTANCE;
				break;

			case Month:
				tableName = "aud_datm_monthly";
				mapper = AuditDatumMonthlyEntityRowMapper.INSTANCE;
				break;

			case RunningTotal:
				tableName = "aud_acc_datm_daily";
				mapper = AuditDatumAccumulativeEntityRowMapper.INSTANCE;
				break;

			default:
				tableName = "aud_datm_hourly";
				mapper = AuditDatumHourlyEntityRowMapper.INSTANCE;
		}
		return jdbcTemplate.query(
				String.format("SELECT * FROM solardatm.%s ORDER BY stream_id, ts_start", tableName),
				mapper);

	}

}
