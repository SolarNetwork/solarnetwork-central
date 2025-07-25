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

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static net.solarnetwork.codec.JsonUtils.getJSONString;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.ReadingDatumEntity;
import net.solarnetwork.central.datum.v2.dao.TypedDatumEntity;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.domain.ReadingDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.domain.StaleAuditDatum;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.central.domain.AuditNodeServiceValue;
import net.solarnetwork.central.domain.AuditUserServiceValue;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataProvider;

/**
 * Utilities for working with datum at the database level.
 *
 * <p>
 * These utilities are primarily designed to support unit testing.
 * </p>
 *
 * @author matt
 * @version 2.6
 * @since 3.8
 */
public final class DatumDbUtils {

	private DatumDbUtils() {
		// don't construct me
	}

	/** Regex for a line starting with a {@literal #} comment character. */
	public static final Pattern COMMENT = Pattern.compile("^\\s*#");

	/** Regex for a line containing {@literal "type":"Reset"}. */
	public static final Pattern AUX = Pattern.compile("\"type\"\\s*:\\s*\"Reset\"");

	/**
	 * Regex for a line containing {@literal "kind":"X"} where {@literal X} is
	 * one of {@literal Hour}, {@literal Day}, or {@literal Month}.
	 */
	public static final Pattern AGG = Pattern.compile("\"kind\"\\s*:\\s*\"(?:Hour|Day|Month|None)\"");

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
	 * @param provider
	 *        the results to extract the stream IDs from
	 * @param comparator
	 *        the comparator to use; for example {@link #UUID_STRING_ORDER}
	 * @return the sorted set
	 */
	public static SortedSet<UUID> sortedStreamIds(ObjectDatumStreamMetadataProvider provider,
			Comparator<UUID> comparator) {
		return provider.metadataStreamIds().stream()
				.collect(Collectors.toCollection(() -> new TreeSet<>(comparator)));
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
		return readingWith(streamId, agg, start, end, acc, stats);
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
	 * @param acc
	 *        the accumulating data to use
	 * @param stats
	 *        the aggregate statistics
	 * @return the datum
	 */
	public static ReadingDatum readingWith(UUID streamId, Aggregation agg, ZonedDateTime start,
			ZonedDateTime end, BigDecimal[] acc, BigDecimal[][] stats) {
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
	 * @see #loadJsonDatumAndAuxiliaryResource(String, Class, Consumer,
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
		assert clazz != null;
		List<Object> result = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(
				new InputStreamReader(clazz.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
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
	 * <p>
	 * Note that "raw" datum can be loaded by specifying
	 * {@literal "kind":"None"} and leaving out the {@literal "stats"} object.
	 * For example:
	 * </p>
	 *
	 * <pre>
	 * <code>{"nodeId":1,"sourceId":"a","ts":"2020-06-01T01:02:03Z","kind":"None","samples":{"i":{"x":1.2,"y":2.1},"a":{"w":100}}}</code>
	 * </pre>
	 *
	 * @param resource
	 *        the name of the resource to load
	 * @param clazz
	 *        the class to load the resource from
	 * @param metadataProvider
	 *        the metadata provider
	 * @param mapper
	 *        an optional function to map the parsed datum with
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
				new InputStreamReader(clazz.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
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
	 * Create a {@link ObjectDatumStreamMetadata} out of a collection of
	 * {@link GeneralNodeDatum} instances.
	 *
	 * @param datums
	 *        the datums
	 * @param timeZoneId
	 *        the time zone ID
	 * @param nspk
	 *        the specific node+source to create the metadata for
	 * @return the metadata
	 */
	public static ObjectDatumStreamMetadata createMetadata(Iterable<GeneralNodeDatum> datums,
			String timeZoneId, NodeSourcePK nspk) {
		return createMetadata(UUID.randomUUID(), timeZoneId, datums, nspk);
	}

	/**
	 * Create a {@link ObjectDatumStreamMetadata} out of a collection of
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
	public static ObjectDatumStreamMetadata createMetadata(UUID streamId, String timeZoneId,
			Iterable<GeneralNodeDatum> datums, NodeSourcePK nspk) {
		Set<String> iNames = new LinkedHashSet<>(4);
		Set<String> aNames = new LinkedHashSet<>(4);
		Set<String> sNames = new LinkedHashSet<>(4);
		for ( GeneralNodeDatum d : datums ) {
			if ( d.getSamples() == null || !(d.getNodeId().equals(nspk.getNodeId())
					&& d.getSourceId().equals(nspk.getSourceId())) ) {
				continue;
			}
			DatumSamples s = d.getSamples();
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
		return new BasicObjectDatumStreamMetadata(streamId, timeZoneId, ObjectDatumKind.Node,
				nspk.getNodeId(), nspk.getSourceId(),
				iNames.isEmpty() ? null : iNames.toArray(String[]::new),
				aNames.isEmpty() ? null : aNames.toArray(String[]::new),
				sNames.isEmpty() ? null : sNames.toArray(String[]::new));
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
	 * @param timeZoneId
	 *        the time zone ID
	 * @return the resulting stream metadata
	 */
	public static Map<NodeSourcePK, ObjectDatumStreamMetadata> insertDatumStream(Logger log,
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums, String timeZoneId) {
		final Map<NodeSourcePK, ObjectDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement datumStmt = con.prepareStatement("""
					INSERT INTO solardatm.da_datm (stream_id,ts,received,data_i,data_a,data_s,data_t)
					VALUES (?::uuid,?,?,?::numeric[],?::numeric[],?::text[],?::text[])""")) {
				final Timestamp now = Timestamp.from(Instant.now());
				for ( GeneralNodeDatum d : datums ) {
					final DatumSamples s = d.getSamples();
					if ( s == null || s.isEmpty() ) {
						continue;
					}
					if ( log != null ) {
						log.debug("Inserting Datum {}", d);
					}

					NodeSourcePK nspk = new NodeSourcePK(d.getNodeId(), d.getSourceId());
					ObjectDatumStreamMetadata meta = result.computeIfAbsent(nspk,
							k -> createMetadata(datums, timeZoneId, k));
					datumStmt.setString(1, meta.getStreamId().toString());
					datumStmt.setTimestamp(2, Timestamp.from(d.getCreated()));
					datumStmt.setTimestamp(3, now);

					String[] iNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
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

					String[] aNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
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

					String[] sNames = meta.propertyNamesForType(DatumSamplesType.Status);
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
						String[] strings = tags.toArray(String[]::new);
						Array aArray = con.createArrayOf("TEXT", strings);
						datumStmt.setArray(7, aArray);
					}

					datumStmt.execute();
				}
			}
			insertObjectDatumStreamMetadata(log, con, result.values());
			return null;
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
	public static Map<NodeSourcePK, ObjectDatumStreamMetadata> insertDatumStreamWithAuxiliary(Logger log,
			JdbcOperations jdbcTemplate, String resource, Class<?> clazz, String timeZoneId)
			throws IOException {
		List<?> data = loadJsonDatumAndAuxiliaryResource(resource, clazz);
		log.debug("Got test data: {}", data);
		List<GeneralNodeDatum> datums = elementsOf(data, GeneralNodeDatum.class);
		List<GeneralNodeDatumAuxiliary> auxDatums = elementsOf(data, GeneralNodeDatumAuxiliary.class);
		Map<NodeSourcePK, ObjectDatumStreamMetadata> meta = insertDatumStream(log, jdbcTemplate, datums,
				timeZoneId);
		UUID streamId;
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

	/**
	 * Get a SQL INSERT statement for the {@code solardatm.da_*_meta} table.
	 *
	 * <p>
	 * The order of columns is:
	 * </p>
	 *
	 * <ol>
	 * <li>stream_id (string as UUID)</li>
	 * <li>object_id (bigint)</li>
	 * <li>source_id (text)</li>
	 * <li>names_i (text[])</li>
	 * <li>names_a (text[])</li>
	 * <li>names_s (text[])</li>
	 * <li>jdata (string as JSONB)</li>
	 * </ol>
	 *
	 * @param kind
	 *        the meta kind
	 * @return the SQL
	 * @since 2.6
	 */
	public static String insertDatumMetaSql(ObjectDatumKind kind) {
		return """
				INSERT INTO solardatm.%s (stream_id,%s,source_id,names_i,names_a,names_s,jdata)
				VALUES (?::uuid,?,?,?::text[],?::text[],?::text[],?::jsonb)
				""".formatted(switch (kind) {
			case Location -> "da_loc_datm_meta";
			default -> "da_datm_meta";
		}, switch (kind) {
			case Location -> "loc_id";
			default -> "node_id";
		});
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			insertObjectDatumStreamMetadata(log, con, metas);
			return null;
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
		try (PreparedStatement nodeMetaStmt = con
				.prepareStatement(insertDatumMetaSql(ObjectDatumKind.Node));
				PreparedStatement locMetaStmt = con
						.prepareStatement(insertDatumMetaSql(ObjectDatumKind.Location))) {
			for ( ObjectDatumStreamMetadata meta : metas ) {
				if ( log != null ) {
					log.debug("Inserting ObjectDatumStreamMetadata {}", meta);
				}
				@SuppressWarnings("resource")
				PreparedStatement metaStmt = (meta.getKind() == ObjectDatumKind.Location ? locMetaStmt
						: nodeMetaStmt);
				metaStmt.setString(1, meta.getStreamId().toString());
				metaStmt.setObject(2, meta.getObjectId());
				metaStmt.setString(3, meta.getSourceId());

				String[] iNames = meta.propertyNamesForType(DatumSamplesType.Instantaneous);
				if ( iNames == null || iNames.length < 1 ) {
					metaStmt.setNull(4, Types.OTHER);
				} else {
					Array iArray = con.createArrayOf("TEXT", iNames);
					metaStmt.setArray(4, iArray);
				}

				String[] aNames = meta.propertyNamesForType(DatumSamplesType.Accumulating);
				if ( aNames == null || aNames.length < 1 ) {
					metaStmt.setNull(5, Types.OTHER);
				} else {
					Array aArray = con.createArrayOf("TEXT", aNames);
					metaStmt.setArray(5, aArray);
				}

				String[] sNames = meta.propertyNamesForType(DatumSamplesType.Status);
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
	 * @param timeZoneId
	 *        the time zone ID
	 * @return the resulting stream metadata
	 */
	public static Map<NodeSourcePK, ObjectDatumStreamMetadata> ingestDatumStream(Logger log,
			JdbcOperations jdbcTemplate, Iterable<GeneralNodeDatum> datums, String timeZoneId) {
		final Map<NodeSourcePK, ObjectDatumStreamMetadata> result = new LinkedHashMap<>();
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (CallableStatement datumStmt = con
					.prepareCall("{? = call solardatm.store_datum(?,?,?,?,?)}")) {
				datumStmt.registerOutParameter(1, Types.OTHER);
				final Timestamp now = Timestamp.from(Instant.now());
				for ( GeneralNodeDatum d : datums ) {
					final DatumSamples s = d.getSamples();
					if ( s == null || s.isEmpty() ) {
						continue;
					}
					if ( log != null ) {
						log.debug("Inserting Datum {}", d);
					}

					NodeSourcePK nspk = new NodeSourcePK(d.getNodeId(), d.getSourceId());
					datumStmt.setTimestamp(2, Timestamp.from(d.getCreated()));
					datumStmt.setObject(3, nspk.getNodeId());
					datumStmt.setString(4, nspk.getSourceId());
					datumStmt.setTimestamp(5, now);

					String json = getJSONString(s, null);
					datumStmt.setString(6, json);
					datumStmt.execute();

					Object id = datumStmt.getObject(1);
					UUID streamId = (id instanceof UUID uuid ? uuid
							: id != null ? UUID.fromString(id.toString()) : null);
					result.computeIfAbsent(nspk, k -> streamMetadata(jdbcTemplate, streamId));
				}
			}
			return null;
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
		List<DatumAuxiliary> converted = StreamSupport.stream(datums.spliterator(), false)
				.map(d -> new DatumAuxiliaryEntity(streamId, d.getCreated(), d.getType(), d.getCreated(),
						d.getSamplesFinal(), d.getSamplesStart(), d.getNotes(), d.getMeta()))
				.collect(Collectors.toList());
		insertDatumAuxiliary(log, jdbcTemplate, converted);
	}

	/**
	 * Insert auxiliary datum records for a given stream.
	 *
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	public static void insertDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate,
			Iterable<DatumAuxiliary> datums) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement datumStmt = con.prepareStatement(
					"insert into solardatm.da_datm_aux (stream_id,ts,atype,jdata_af,jdata_as) "
							+ "VALUES (?::uuid,?,?::solardatm.da_datm_aux_type,?::jsonb,?::jsonb)")) {
				for ( DatumAuxiliary d : datums ) {
					String sf = getJSONString(d.getSamplesFinal().getA(), null);
					String ss = getJSONString(d.getSamplesStart().getA(), null);
					if ( log != null ) {
						log.debug("Inserting DatumAuxiliary {}; {} -> {}", d.getId(), sf, ss);
					}
					datumStmt.setString(1, d.getStreamId().toString());
					datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));
					datumStmt.setString(3, d.getType().name());
					datumStmt.setString(4, sf);
					datumStmt.setString(5, ss);
					datumStmt.execute();
				}
			}
			return null;
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement datumStmt = con.prepareStatement(insertDatumSql())) {
				final Timestamp now = Timestamp.from(Instant.now());
				for ( Datum d : datums ) {
					if ( log != null ) {
						log.debug("Inserting Datum: {}", d);
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

					datumStmt.setTimestamp(7, now);

					datumStmt.execute();
				}
			}
			return null;
		});
	}

	/**
	 * Get the SQL to insert into the {@code solardatm.da_datm} table.
	 *
	 * <p>
	 * The column order is:
	 * </p>
	 *
	 * <ol>
	 * <li>stream_id (string)</li>
	 * <li>ts (timestamp)</li>
	 * <li>data_i (numeric[])</li>
	 * <li>data_a (numeric[])</li>
	 * <li>data_s (text[])</li>
	 * <li>data_t (text[])</li>
	 * <li>received (timestamp)</li>
	 * </ol>
	 *
	 * @return the insert SQL
	 * @since 2.6
	 */
	public static String insertDatumSql() {
		return """
				INSERT INTO solardatm.da_datm (stream_id,ts,data_i,data_a,data_s,data_t,received)
				VALUES (?::uuid,?,?::numeric[],?::numeric[],?::text[],?::text[],?)
				""";
	}

	/**
	 * Get the SQL to insert into a {@code solardatm.agg_datm_*} table.
	 *
	 * <p>
	 * The column order is:
	 * </p>
	 *
	 * <ol>
	 * <li>stream_id (string)</li>
	 * <li>ts_start (timestamp)</li>
	 * <li>data_i (numeric[])</li>
	 * <li>data_a (numeric[])</li>
	 * <li>data_s (text[])</li>
	 * <li>data_t (text[])</li>
	 * <li>stat_i (numeric[][])</li>
	 * <li>read_a (numeric[][])</li>
	 * </ol>
	 *
	 * @param kind
	 *        the aggregation kind; only {@code Hour}, {@code Day}, and
	 *        {@code Month} are supported
	 * @return the insert SQL
	 * @since 2.6
	 */
	public static String insertAggDatumSql(Aggregation kind) {
		return """
				INSERT INTO solardatm.agg_datm_%s (stream_id,ts_start,data_i,data_a,data_s,data_t,stat_i,read_a)
				VALUES (?::uuid,?,?::numeric[],?::numeric[],?::text[],?::text[],?::numeric[][],?::numeric[][])
				"""
				.formatted(switch (kind) {
					case Day -> "daily";
					case Month -> "monthly";
					default -> "hourly";
				});
	}

	/**
	 * Insert aggregate datum records.
	 *
	 * <p>
	 * Note that {@link Aggregation#None} is supported, which loads the datum
	 * into the {@code da_datm} table.
	 * </p>
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement rawStmt = con.prepareStatement(insertDatumSql());
					PreparedStatement hourStmt = con
							.prepareStatement(insertAggDatumSql(Aggregation.Hour));
					PreparedStatement dayStmt = con.prepareStatement(insertAggDatumSql(Aggregation.Day));
					PreparedStatement monthStmt = con
							.prepareStatement(insertAggDatumSql(Aggregation.Month))) {
				final Timestamp now = Timestamp.from(Instant.now());
				for ( AggregateDatum d : datums ) {
					Aggregation kind = (d.getAggregation() != null ? d.getAggregation()
							: Aggregation.Hour);
					@SuppressWarnings("resource")
					PreparedStatement datumStmt = switch (kind) {
						case Hour -> hourStmt;
						case Day -> dayStmt;
						case Month -> monthStmt;
						default -> rawStmt;
					};
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

					if ( d.getAggregation() == Aggregation.None ) {
						datumStmt.setTimestamp(7, now);
					} else {
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
					}

					datumStmt.execute();
				}
			}
			return null;
		});
	}

	/**
	 * Insert a set of daily {@link AuditNodeServiceValue} entities in the
	 * database.
	 *
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param data
	 *        the data to insert
	 * @since 2.5
	 */
	public static void insertAuditNodeServiceValueDaily(Logger log, JdbcOperations jdbcTemplate,
			Iterable<AuditNodeServiceValue> data) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement stmt = con.prepareStatement("""
					INSERT INTO solardatm.aud_node_daily
						(node_id, service, ts_start, cnt)
					VALUES (?, ?, ?, ?)
					""")) {
				for ( AuditNodeServiceValue v : data ) {
					if ( log != null ) {
						log.debug("Inserting AuditNodeServiceValue: {}", v);
					}
					stmt.setObject(1, v.getNodeId());
					stmt.setObject(2, v.getService());
					stmt.setTimestamp(3, Timestamp.from(v.getTimestamp()));
					stmt.setObject(4, v.getCount());
					stmt.execute();
				}
			}
			return null;
		});
	}

	/**
	 * Insert a set of daily {@link AuditNodeServiceValue} entities in the
	 * database.
	 *
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param data
	 *        the data to insert
	 * @since 2.5
	 */
	public static void insertAuditUserServiceValueDaily(Logger log, JdbcOperations jdbcTemplate,
			Iterable<AuditUserServiceValue> data) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement stmt = con.prepareStatement("""
					INSERT INTO solardatm.aud_user_daily
						(user_id, service, ts_start, cnt)
					VALUES (?, ?, ?, ?)
					""")) {
				for ( AuditUserServiceValue v : data ) {
					if ( log != null ) {
						log.debug("Inserting AuditNodeServiceValue: {}", v);
					}
					stmt.setObject(1, v.getUserId());
					stmt.setObject(2, v.getService());
					stmt.setTimestamp(3, Timestamp.from(v.getTimestamp()));
					stmt.setObject(4, v.getCount());
					stmt.execute();
				}
			}
			return null;
		});
	}

	/**
	 * Insert audit datum records.
	 *
	 * @param log
	 *        a logger for debug message
	 * @param jdbcTemplate
	 *        the JDBC template
	 * @param datums
	 *        the datum to insert
	 */
	@SuppressWarnings("StatementSwitchToExpressionSwitch")
	public static void insertAuditDatum(Logger log, JdbcOperations jdbcTemplate,
			Iterable<AuditDatum> datums) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (PreparedStatement hourStmt = con.prepareStatement(insertAuditStmt(Aggregation.Hour));
					PreparedStatement dayStmt = con.prepareStatement(insertAuditStmt(Aggregation.Day));
					PreparedStatement monthStmt = con
							.prepareStatement(insertAuditStmt(Aggregation.Month));
					PreparedStatement accStmt = con
							.prepareStatement(insertAuditStmt(Aggregation.RunningTotal))) {

				for ( AuditDatum d : datums ) {
					if ( log != null ) {
						log.debug("Inserting {} AuditDatum; {}", d.getAggregation(), d);
					}
					@SuppressWarnings("resource")
					PreparedStatement datumStmt = switch (d.getAggregation()) {
						case Hour -> hourStmt;
						case Day -> dayStmt;
						case Month -> monthStmt;
						default -> accStmt;
					};
					datumStmt.setString(1, d.getStreamId().toString());
					datumStmt.setTimestamp(2, Timestamp.from(d.getTimestamp()));

					switch (d.getAggregation()) {
						case Hour:
							datumStmt.setObject(3, d.getDatumPropertyCount());
							datumStmt.setObject(4, d.getDatumPropertyUpdateCount());
							datumStmt.setObject(5, d.getDatumQueryCount());
							datumStmt.setObject(6, d.getFluxDataInCount());
							datumStmt.setObject(7, d.getDatumCount());
							break;

						case Day:
							datumStmt.setObject(3, d.getDatumPropertyCount());
							datumStmt.setObject(4, d.getDatumPropertyUpdateCount());
							datumStmt.setObject(5, d.getDatumQueryCount());
							datumStmt.setObject(6, d.getFluxDataInCount());
							datumStmt.setObject(7, d.getDatumCount());
							datumStmt.setObject(8, d.getDatumHourlyCount());
							datumStmt.setBoolean(9, d.getDatumDailyCount() > 0);
							break;

						case Month:
							datumStmt.setObject(3, d.getDatumPropertyCount());
							datumStmt.setObject(4, d.getDatumPropertyUpdateCount());
							datumStmt.setObject(5, d.getDatumQueryCount());
							datumStmt.setObject(6, d.getFluxDataInCount());
							datumStmt.setObject(7, d.getDatumCount());
							datumStmt.setObject(8, d.getDatumHourlyCount());
							datumStmt.setObject(9, d.getDatumDailyCount());
							datumStmt.setBoolean(10, d.getDatumMonthlyCount() > 0);
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
		});
	}

	private static String insertAuditStmt(Aggregation kind) {
		StringBuilder buf = new StringBuilder();
		buf.append("insert into solardatm.");
		buf.append(switch (kind) {
			case Hour -> "aud_datm_io";
			case Day -> "aud_datm_daily";
			case Month -> "aud_datm_monthly";
			default -> "aud_acc_datm_daily";
		});
		buf.append(" (stream_id,ts_start,");
		buf.append(switch (kind) {
			case Hour -> "prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count";
			case Day -> "prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count,datum_hourly_count,datum_daily_pres";
			case Month -> "prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres";
			default -> "datum_count,datum_hourly_count,datum_daily_count,datum_monthly_count";
		});
		buf.append(") VALUES (?::uuid,?,");
		buf.append(switch (kind) {
			case Hour -> "?,?,?,?,?";
			case Day -> "?,?,?,?,?,?,?";
			case Month -> "?,?,?,?,?,?,?,?";
			default -> "?,?,?,?";
		});
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
	 * @param streamId
	 *        the stream ID
	 * @param datums
	 *        the datum to insert
	 */
	public static void ingestDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate, UUID streamId,
			Iterable<GeneralNodeDatumAuxiliary> datums) {
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
			try (CallableStatement stmt = con.prepareCall(
					"{call solardatm.store_datum_aux(?,?,?::solardatm.da_datm_aux_type,?,?::jsonb,?::jsonb,?::jsonb)}")) {
				for ( GeneralNodeDatumAuxiliary d : datums ) {
					if ( log != null ) {
						log.debug("Inserting GeneralNodeDatumAuxiliary {}; {} -> {}", d.getId(),
								d.getSampleDataFinal(), d.getSampleDataStart());
					}
					stmt.setObject(1, streamId, Types.OTHER);
					stmt.setTimestamp(2, Timestamp.from(d.getCreated()));
					stmt.setString(3, d.getType().name());
					stmt.setNull(4, Types.VARCHAR);
					stmt.setString(5, d.getSampleJsonFinal());
					stmt.setString(6, d.getSampleJsonStart());
					stmt.setNull(7, Types.VARCHAR);
					stmt.execute();
				}
			}
			return null;
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
	 * @param from
	 *        the datum ID to move
	 * @param to
	 *        the updated datum value
	 * @return {@literal true} if a datum was found and moved
	 */
	public static boolean moveDatumAuxiliary(Logger log, JdbcOperations jdbcTemplate,
			DatumAuxiliaryPK from, DatumAuxiliary to) {
		return jdbcTemplate.execute((ConnectionCallback<Boolean>) con -> {
			try (CallableStatement stmt = con.prepareCall(
					"{? = call solardatm.move_datum_aux(?,?,?::solardatm.da_datm_aux_type,?,?,?::solardatm.da_datm_aux_type,?,?::jsonb,?::jsonb,?::jsonb)}")) {
				if ( log != null ) {
					log.debug("Moving GeneralNodeDatumAuxiliary {} -> {}", from, to.getId());
				}
				stmt.registerOutParameter(1, Types.BOOLEAN);
				stmt.setObject(2, from.getStreamId(), Types.OTHER);
				stmt.setTimestamp(3, Timestamp.from(from.getTimestamp()));
				stmt.setString(4, from.getKind().name());
				stmt.setObject(5, to.getStreamId(), Types.OTHER);
				stmt.setTimestamp(6, Timestamp.from(to.getTimestamp()));
				stmt.setString(7, to.getType().name());
				stmt.setString(8, to.getNotes());
				stmt.setString(9, getJSONString(to.getSamplesFinal(), null));
				stmt.setString(10, getJSONString(to.getSamplesStart(), null));
				stmt.setString(11, getJSONString(to.getMetadata(), null));
				stmt.execute();
				return stmt.getBoolean(1);
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
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
		});
	}

	private static void debugStaleAggregateDatumTable(Logger log, JdbcOperations jdbcTemplate,
			String msg) {
		List<Map<String, Object>> staleRows = jdbcTemplate
				.queryForList("SELECT * FROM solardatm.agg_stale_datm ORDER BY ts_start, stream_id");
		log.debug("{}:\n{}", msg, staleRows.stream().map(Object::toString).collect(joining("\n")));
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

		List<Aggregation> sortedKinds = kinds.stream().sorted(Aggregation::compareLevel).toList();

		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
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
		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
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

		List<Aggregation> sortedKinds = kinds.stream().sorted(Aggregation::compareLevel).toList();

		jdbcTemplate.execute((ConnectionCallback<Void>) con -> {
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
		log.debug("{}:\n{}", msg, staleRows.stream().map(Object::toString).collect(joining("\n")));
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
		return (results.isEmpty() ? null : results.getFirst());
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
	public static List<ObjectDatumStreamMetadata> listNodeMetadata(JdbcOperations jdbcTemplate) {
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
	public static List<ObjectDatumStreamMetadata> listLocationMetadata(JdbcOperations jdbcTemplate) {
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
		RowMapper<AggregateDatum> mapper = switch (kind) {
			case Day -> {
				tableName = "daily";
				yield AggregateDatumEntityRowMapper.DAY_INSTANCE;
			}
			case Month -> {
				tableName = "monthly";
				yield AggregateDatumEntityRowMapper.MONTH_INSTANCE;
			}
			default -> {
				tableName = "hourly";
				yield AggregateDatumEntityRowMapper.HOUR_INSTANCE;
			}
		};
		return jdbcTemplate.query(String.format(
				"SELECT * FROM solardatm.agg_datm_%s ORDER BY stream_id, ts_start", tableName), mapper);
	}

	/**
	 * Get all available audit datum records.
	 *
	 * <p>
	 * For I/O audit records, use the {@code Hour} aggregation kind.
	 * </p>
	 *
	 * @param jdbcTemplate
	 *        the JDBC accessor
	 * @param kind
	 *        the aggregation kind to load, e.g. {@code Hour}, {@code Day}, or
	 *        {@code Month}
	 * @return the results, never {@literal null}
	 */
	public static List<AuditDatum> listAuditDatum(JdbcOperations jdbcTemplate, Aggregation kind) {
		String tableName;
		String rowNames;
		RowMapper<AuditDatum> mapper = switch (kind) {
			case Day -> {
				tableName = "aud_datm_daily";
				rowNames = "stream_id,ts_start,prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count,datum_hourly_count,datum_daily_pres";
				yield AuditDatumDailyEntityRowMapper.INSTANCE;
			}
			case Month -> {
				tableName = "aud_datm_monthly";
				rowNames = "stream_id,ts_start,prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_pres";
				yield AuditDatumMonthlyEntityRowMapper.INSTANCE;
			}
			case RunningTotal -> {
				tableName = "aud_acc_datm_daily";
				rowNames = "stream_id,ts_start,datum_count,datum_hourly_count,datum_daily_count,datum_monthly_count";
				yield AuditDatumAccumulativeEntityRowMapper.INSTANCE;
			}
			default -> {
				tableName = "aud_datm_io";
				rowNames = "stream_id,ts_start,prop_count,prop_u_count,datum_q_count,flux_byte_count,datum_count";
				yield AuditDatumIoEntityRowMapper.INSTANCE;
			}
		};
		return jdbcTemplate.query(
				format("SELECT %s FROM solardatm.%s ORDER BY stream_id, ts_start", rowNames, tableName),
				mapper);
	}

}
