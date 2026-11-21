/* ==================================================================
 * DbStoreLocDatumTests.java - 21/11/2026 2:50:54 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.listStaleAggregateDatum;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils.streamMetadata;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.ConnectionCallback;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@code solardatm.store_loc_datum()} database functions,
 * validating NET-545.
 *
 * <p>
 * A location datum that lacks its stream's first property of a kind must be
 * stored with that kind's array starting at position 1, with NULL in the
 * leading places, as {@code solardatm.store_datum()} does for node datum. A
 * non-numeric accumulating value must leave the instantaneous values alone, and
 * a status property name already in the stream must not replace the stream's
 * status property names.
 * </p>
 *
 * <p>
 * The stored arrays are read in SQL, because reading an array over JDBC hides
 * its lower bound.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class DbStoreLocDatumTests extends BaseDatumJdbcTestSupport {

	/** The source ID of every test stream. */
	private static final String SOURCE_ID = "a";

	/** The datum timestamp used by most tests. */
	private static final Instant TS = Instant.parse("2026-11-21T01:00:00Z");

	/** The SQL to call the location function that takes a JSON datum. */
	private static final String STORE_LOC_DATUM_SQL = """
			SELECT solardatm.store_loc_datum(?::timestamptz, ?::bigint, ?::text, ?::timestamptz
				, ?::text, ?::boolean)::text
			""";

	/** The SQL to call the node function that takes a JSON datum. */
	private static final String STORE_NODE_DATUM_SQL = """
			SELECT solardatm.store_datum(?::timestamptz, ?::bigint, ?::text, ?::timestamptz
				, ?::text, ?::boolean)::text
			""";

	/**
	 * A stored datum property array, read in SQL.
	 *
	 * @param lower
	 *        the array's lower bound, from {@code array_lower()}
	 * @param text
	 *        the array's text form, which shows a lower bound above 1 as a
	 *        dimension decoration such as {@code [2:3]=}
	 * @param elements
	 *        the elements from position 1 to the array's upper bound, read by
	 *        SQL subscript, as text
	 */
	private record StoredArray(@Nullable Integer lower, @Nullable String text,
			List<@Nullable String> elements) {

	}

	/**
	 * The {@code INOUT} and {@code OUT} values of the inner location function.
	 *
	 * @param namesI
	 *        the instantaneous property names returned
	 * @param namesA
	 *        the accumulating property names returned
	 * @param namesS
	 *        the status property names returned
	 * @param inserted
	 *        {@code true} if the datum was inserted rather than updated
	 */
	private record StoreResult(List<String> namesI, List<String> namesA, List<String> namesS,
			boolean inserted) {

	}

	private JdbcDatumEntityDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	private ObjectDatumStreamMetadata createStream(ObjectDatumKind kind, String @Nullable [] namesI,
			String @Nullable [] namesA, String @Nullable [] namesS) {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC",
				kind, (kind == ObjectDatumKind.Location ? TEST_LOC_ID : TEST_NODE_ID), SOURCE_ID, namesI,
				namesA, namesS);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));
		return meta;
	}

	private ObjectDatumStreamMetadata createLocationStream(String @Nullable [] namesI,
			String @Nullable [] namesA, String @Nullable [] namesS) {
		return createStream(ObjectDatumKind.Location, namesI, namesA, namesS);
	}

	private static String[] names(String... names) {
		return names;
	}

	/**
	 * Store a JSON location datum through the function that takes a JSON datum,
	 * as the DAO does.
	 */
	private UUID storeLocDatum(@Nullable Instant ts, String json) {
		String sid = jdbcTemplate.queryForObject(STORE_LOC_DATUM_SQL, String.class,
				(ts != null ? Timestamp.from(ts) : null), TEST_LOC_ID, SOURCE_ID,
				Timestamp.from(Instant.now()), json, true);
		return UUID.fromString(sid);
	}

	/**
	 * Store a JSON node datum through the function that takes a JSON datum, as
	 * the DAO does.
	 */
	private UUID storeNodeDatum(Instant ts, String json) {
		String sid = jdbcTemplate.queryForObject(STORE_NODE_DATUM_SQL, String.class, Timestamp.from(ts),
				TEST_NODE_ID, SOURCE_ID, Timestamp.from(Instant.now()), json, true);
		return UUID.fromString(sid);
	}

	private static @Nullable Array textArray(Connection con, String @Nullable [] values)
			throws SQLException {
		return (values != null ? con.createArrayOf("text", values) : null);
	}

	private static List<String> textList(ResultSet rs, int col) throws SQLException {
		Array a = rs.getArray(col);
		return (a != null ? asList((String[]) a.getArray()) : List.of());
	}

	/**
	 * Call the inner location function directly, with the given property name
	 * arrays standing for the names the caller has.
	 */
	private StoreResult storeLocDatum(UUID streamId, @Nullable String jdataI, @Nullable String jdataA,
			@Nullable String jdataS, String @Nullable [] namesI, String @Nullable [] namesA,
			String @Nullable [] namesS) {
		return jdbcTemplate.execute((ConnectionCallback<StoreResult>) con -> {
			try (PreparedStatement stmt = con.prepareStatement("""
					SELECT p_i, p_a, p_s, is_ins
					FROM solardatm.store_loc_datum(?::uuid, ?::timestamptz, ?::text, ?::timestamptz
						, ?::jsonb, ?::jsonb, ?::jsonb, ?::text[], ?::text[], ?::text[], ?::text[])
					""")) {
				stmt.setString(1, streamId.toString());
				stmt.setTimestamp(2, Timestamp.from(TS));
				stmt.setString(3, SOURCE_ID);
				stmt.setTimestamp(4, Timestamp.from(Instant.now()));
				stmt.setString(5, jdataI);
				stmt.setString(6, jdataA);
				stmt.setString(7, jdataS);
				stmt.setNull(8, Types.ARRAY);
				stmt.setArray(9, textArray(con, namesI));
				stmt.setArray(10, textArray(con, namesA));
				stmt.setArray(11, textArray(con, namesS));
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					return new StoreResult(textList(rs, 1), textList(rs, 2), textList(rs, 3),
							rs.getBoolean(4));
				}
			}
		});
	}

	/**
	 * Read one property array of a stored datum in SQL.
	 *
	 * @param streamId
	 *        the datum stream ID
	 * @param ts
	 *        the datum timestamp
	 * @param column
	 *        the array column: {@code data_i}, {@code data_a}, {@code data_s}
	 *        or {@code data_t}
	 * @return the array
	 */
	private StoredArray storedArray(UUID streamId, Instant ts, String column) {
		final String sql = """
				SELECT array_lower(d.%1$s, 1)
					, d.%1$s::text
					, ARRAY(
						SELECT d.%1$s[s]::text
						FROM generate_series(1, array_upper(d.%1$s, 1)) s
						ORDER BY s
					)
				FROM solardatm.da_datm d
				WHERE d.stream_id = ?::uuid AND d.ts = ?
				""".formatted(column);
		StoredArray result = jdbcTemplate.queryForObject(sql, DbStoreLocDatumTests::mapStoredArray,
				streamId.toString(), Timestamp.from(ts));
		log.debug("Stored {} for stream {} at {}: {}", column, streamId, ts, result);
		return result;
	}

	private static StoredArray mapStoredArray(ResultSet rs, int rowNum) throws SQLException {
		return new StoredArray((Integer) rs.getObject(1), rs.getString(2), textList(rs, 3));
	}

	private static List<String> namesOf(ObjectDatumStreamMetadata meta, DatumSamplesType type) {
		String[] names = meta.propertyNamesForType(type);
		return (names != null ? asList(names) : List.of());
	}

	@Test
	public void instantaneousWithoutFirstProperty() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(names("i1", "i2", "i3"), null, null);

		// WHEN
		storeLocDatum(TS, """
				{"i":{"i2":2,"i3":3}}
				""");

		// THEN
		// @formatter:off
		then(storedArray(meta.getStreamId(), TS, "data_i"))
			.as("Instantaneous array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the place of the missing first property, values in their own places")
			.returns(asList(null, "2", "3"), from(StoredArray::elements))
			.as("Text form has no dimension decoration")
			.returns("{NULL,2,3}", from(StoredArray::text))
			;
		// @formatter:on
	}

	@Test
	public void accumulatingWithoutFirstProperty() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(null, names("a1", "a2", "a3"), null);

		// WHEN
		storeLocDatum(TS, """
				{"a":{"a2":2,"a3":3}}
				""");

		// THEN
		// @formatter:off
		then(storedArray(meta.getStreamId(), TS, "data_a"))
			.as("Accumulating array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the place of the missing first property, values in their own places")
			.returns(asList(null, "2", "3"), from(StoredArray::elements))
			.as("Text form has no dimension decoration")
			.returns("{NULL,2,3}", from(StoredArray::text))
			;
		// @formatter:on
	}

	@Test
	public void statusWithoutFirstProperty() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(null, null, names("s1", "s2", "s3"));

		// WHEN
		storeLocDatum(TS, """
				{"s":{"s2":"two","s3":"three"}}
				""");

		// THEN
		// @formatter:off
		then(storedArray(meta.getStreamId(), TS, "data_s"))
			.as("Status array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the place of the missing first property, values in their own places")
			.returns(asList(null, "two", "three"), from(StoredArray::elements))
			.as("Text form has no dimension decoration")
			.returns("{NULL,two,three}", from(StoredArray::text))
			;
		// @formatter:on
	}

	@Test
	public void withoutFirstTwoProperties() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(names("i1", "i2", "i3"),
				names("a1", "a2", "a3"), names("s1", "s2", "s3"));

		// WHEN
		storeLocDatum(TS, """
				{"i":{"i3":3},"a":{"a3":30},"s":{"s3":"three"}}
				""");

		// THEN
		// @formatter:off
		then(storedArray(meta.getStreamId(), TS, "data_i"))
			.as("Instantaneous array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the places of the two missing instantaneous properties")
			.returns(asList(null, null, "3"), from(StoredArray::elements))
			;
		then(storedArray(meta.getStreamId(), TS, "data_a"))
			.as("Accumulating array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the places of the two missing accumulating properties")
			.returns(asList(null, null, "30"), from(StoredArray::elements))
			;
		then(storedArray(meta.getStreamId(), TS, "data_s"))
			.as("Status array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL in the places of the two missing status properties")
			.returns(asList(null, null, "three"), from(StoredArray::elements))
			;
		// @formatter:on
	}

	@Test
	public void dao_withLeadingNullPropertyArrayValues() throws IOException {
		// GIVEN
		// the location twin of JdbcDatumEntityDao_GenericDaoTests.store_withLeadingNullPropertyArrayValues
		ObjectDatumStreamMetadata meta = createLocationStream(
				names("watts", "current", "dcPower", "voltage", "dcVoltage", "frequency",
						"reactivePower", "neutralCurrent"),
				names("wattHours", "wh"), names("limits", "opState", "opStates", "faults"));

		GeneralNodeDatum nodeDatum = loadJsonDatumResource("test-datum-37.txt", getClass()).get(0);
		GeneralLocationDatum datum = new GeneralLocationDatum(TEST_LOC_ID, nodeDatum.getCreated(),
				SOURCE_ID);
		datum.setSamples(new DatumSamples(nodeDatum.getSamples()));

		// WHEN
		dao.store(datum);

		// THEN
		List<Datum> rows = listDatum(jdbcTemplate);
		DatumProperties expectedProps = DatumProperties.propertiesOf(
				decimalArray(null, null, "36901", "11.5", "833.0", "60.0", null, "0.8"),
				decimalArray(null, "36614420"), new String[] { null, "1", "1289" }, null);
		// @formatter:off
		then(rows)
			.as("Datum stored in table")
			.hasSize(1)
			.element(0)
			.as("Stored in the location stream")
			.returns(meta.getStreamId(), from(Datum::getStreamId))
			.as("Properties read over JDBC have leading NULL elements")
			.returns(expectedProps, from(Datum::getProperties))
			;
		then(storedArray(meta.getStreamId(), datum.getCreated(), "data_i"))
			.as("Instantaneous array starts at position 1")
			.returns(1, from(StoredArray::lower))
			;
		then(storedArray(meta.getStreamId(), datum.getCreated(), "data_a"))
			.as("Accumulating array starts at position 1")
			.returns(1, from(StoredArray::lower))
			;
		then(storedArray(meta.getStreamId(), datum.getCreated(), "data_s"))
			.as("Status array starts at position 1")
			.returns(1, from(StoredArray::lower))
			;
		// @formatter:on
	}

	@Test
	public void accumulatingNotNumeric() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(names("i1", "i2"), names("a1", "a2"),
				null);

		// WHEN
		// 'Infinity' and 'NaN' are valid numeric values, so use a value that is not
		storeLocDatum(TS, """
				{"i":{"i1":1,"i2":2},"a":{"a1":"not a number","a2":20}}
				""");

		// THEN
		// @formatter:off
		then(storedArray(meta.getStreamId(), TS, "data_i"))
			.as("Instantaneous values unchanged by the accumulating value that is not a number")
			.returns(asList("1", "2"), from(StoredArray::elements))
			.as("Instantaneous text form shows both values")
			.returns("{1,2}", from(StoredArray::text))
			;
		then(storedArray(meta.getStreamId(), TS, "data_a"))
			.as("Accumulating array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("NULL stored for the accumulating value that is not a number")
			.returns(asList(null, "20"), from(StoredArray::elements))
			;
		// @formatter:on
	}

	@Test
	public void statusNameInStreamButNotGiven() {
		// GIVEN
		// the stream has status property s2, but the caller's status names lack it, as when another
		// datum added it since the caller read the stream's names
		ObjectDatumStreamMetadata meta = createLocationStream(names("i1", "i2"), names("a1"),
				names("s1", "s2"));

		// WHEN
		StoreResult result = storeLocDatum(meta.getStreamId(), """
				{"i1":1}
				""", null, """
				{"s2":"two"}
				""", names("i1", "i2"), names("a1"), names("s1"));

		// THEN
		// @formatter:off
		then(result)
			.as("Status names returned are the stream's")
			.returns(asList("s1", "s2"), from(StoreResult::namesS))
			.as("Instantaneous names returned are the stream's")
			.returns(asList("i1", "i2"), from(StoreResult::namesI))
			.as("Datum inserted")
			.returns(true, from(StoreResult::inserted))
			;
		then(namesOf(streamMetadata(jdbcTemplate, meta.getStreamId()), DatumSamplesType.Status))
			.as("Stream status names unchanged")
			.containsExactly("s1", "s2")
			;
		then(storedArray(meta.getStreamId(), TS, "data_s"))
			.as("Status array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("Status value in the place of its name in the stream")
			.returns(asList(null, "two"), from(StoredArray::elements))
			;
		// @formatter:on
	}

	@Test
	public void statusNameInStreamButNotGiven_sameNameInstantaneous() {
		// GIVEN
		// as statusNameInStreamButNotGiven, but the status name is an instantaneous name as well, so
		// replacing the stream's status names with its instantaneous names would go unnoticed
		ObjectDatumStreamMetadata meta = createLocationStream(names("watts", "state"), null,
				names("mode", "state"));

		// WHEN
		StoreResult result = storeLocDatum(meta.getStreamId(), """
				{"watts":1}
				""", null, """
				{"state":"on"}
				""", names("watts", "state"), null, names("mode"));

		// THEN
		// @formatter:off
		then(namesOf(streamMetadata(jdbcTemplate, meta.getStreamId()), DatumSamplesType.Status))
			.as("Stream status names not replaced by the instantaneous names")
			.containsExactly("mode", "state")
			;
		then(result)
			.as("Status names returned are the stream's")
			.returns(asList("mode", "state"), from(StoreResult::namesS))
			;
		then(storedArray(meta.getStreamId(), TS, "data_s"))
			.as("Status array starts at position 1")
			.returns(1, from(StoredArray::lower))
			.as("Status value in the place of its name in the stream")
			.returns(asList(null, "on"), from(StoredArray::elements))
			;
		// @formatter:on
	}

	@ParameterizedTest
	@ValueSource(strings = {
	// @formatter:off
		"""
		{"i":{"i1":1,"i2":2,"i3":3},"a":{"a1":10,"a2":20,"a3":30},"s":{"s1":"one","s2":"two","s3":"three"},"t":["t1","t2"]}
		""",
		"""
		{"i":{"i2":2,"i3":3},"a":{"a3":30},"s":{"s2":"two"},"t":["t1"]}
		""",
		"""
		{"i":{"i1":1,"i2":2},"a":{"a1":"not a number","a2":20}}
		""",
		"""
		{"i":{"i3":3,"i4":4},"a":{"a2":20,"a4":40},"s":{"s3":"three","s4":"four"}}
		""",
		// @formatter:on
	})
	public void sameAsNodeStream(String json) {
		// GIVEN
		final String[] namesI = names("i1", "i2", "i3");
		final String[] namesA = names("a1", "a2", "a3");
		final String[] namesS = names("s1", "s2", "s3");
		final ObjectDatumStreamMetadata nodeMeta = createStream(ObjectDatumKind.Node, namesI, namesA,
				namesS);
		final ObjectDatumStreamMetadata locMeta = createLocationStream(namesI, namesA, namesS);

		// WHEN
		storeNodeDatum(TS, json);
		storeLocDatum(TS, json);

		// THEN
		for ( String column : List.of("data_i", "data_a", "data_s", "data_t") ) {
			// @formatter:off
			then(storedArray(locMeta.getStreamId(), TS, column))
				.as("Location %s same as node %s for %s", column, column, json)
				.isEqualTo(storedArray(nodeMeta.getStreamId(), TS, column))
				;
			// @formatter:on
		}
		for ( DatumSamplesType type : List.of(DatumSamplesType.Instantaneous,
				DatumSamplesType.Accumulating, DatumSamplesType.Status) ) {
			// @formatter:off
			then(namesOf(streamMetadata(jdbcTemplate, locMeta.getStreamId()), type))
				.as("Location %s names same as node names for %s", type, json)
				.isEqualTo(namesOf(streamMetadata(jdbcTemplate, nodeMeta.getStreamId()), type))
				;
			// @formatter:on
		}
	}

	@Test
	public void noDatumDate() {
		// GIVEN
		ObjectDatumStreamMetadata meta = createLocationStream(names("i1"), null, null);

		// WHEN
		// a NULL datum date stores the datum at the current time, as the node function does
		UUID streamId = storeLocDatum(null, """
				{"i":{"i1":1}}
				""");

		// THEN
		// the current time is the start of the test transaction
		final Instant now = jdbcTemplate.queryForObject("SELECT now()", Timestamp.class).toInstant();
		// @formatter:off
		then(streamId)
			.as("Stored in the location stream")
			.isEqualTo(meta.getStreamId())
			;
		then(listDatum(jdbcTemplate))
			.as("Datum stored")
			.hasSize(1)
			.element(0)
			.as("Datum stored at the current time")
			.returns(now, from(Datum::getTimestamp))
			;
		then(listStaleAggregateDatum(jdbcTemplate))
			.as("Stale hour inserted")
			.hasSize(1)
			.element(0)
			.as("Stale hour is for the location stream")
			.returns(meta.getStreamId(), from(StaleAggregateDatum::getStreamId))
			.as("Stale hour is an hourly aggregate")
			.returns(Aggregation.Hour, from(StaleAggregateDatum::getKind))
			.as("Stale hour is the datum's hour")
			.returns(now.truncatedTo(ChronoUnit.HOURS), from(StaleAggregateDatum::getTimestamp))
			;
		// @formatter:on
	}

}
