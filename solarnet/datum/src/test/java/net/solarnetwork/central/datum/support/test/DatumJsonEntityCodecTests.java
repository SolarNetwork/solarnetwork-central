/* ==================================================================
 * DatumJsonEntityCodecTests.java - 19/03/2026 12:23:02 pm
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

package net.solarnetwork.central.datum.support.test;

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.DatumJsonEntityCodec;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumPK;
import net.solarnetwork.central.datum.v2.support.DatumJsonUtils;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StatTracker;
import tools.jackson.databind.json.JsonMapper;

/**
 * Test cases for the {@link DatumJsonEntityCodec} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumJsonEntityCodecTests {

	private static final JsonMapper JSON_MAPPER = DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER;

	private static final Logger log = LoggerFactory.getLogger(DatumJsonEntityCodecTests.class);

	private StatTracker stats;

	@BeforeEach
	public void setup() {
		stats = new StatTracker("DatumJsonEntityCodec", null, log, 10);

	}

	@Test
	public void codec_Datum_Node() {
		// GIVEN
		final var service = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralDatum entity = GeneralDatum.nodeDatum(nodeId, sourceId, ts,
				new DatumSamples(iData, aData, null));

		// WHEN
		String serialized = service.serialize(entity);
		DatumPK id = service.entityId(entity);
		Object deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("created", ISO_DATE_TIME_ALT_UTC.format(ts)),
				entry("nodeId", nodeId),
				entry("sourceId", sourceId),
				entry("i", iData),
				entry("a", aData)
			)
			;

		then(id)
			.as("DatumPK for unassigned stream returned")
			.isEqualTo(ObjectDatumPK.unassignedStream(
				ObjectDatumKind.Node,
				nodeId,
				sourceId,
				ts)
			)
			;

		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to GeneralDatum instance")
			.isEqualTo(entity)
			;
		// @formatter:on
	}

	@Test
	public void codec_Datum_Location() {
		// GIVEN
		final var service = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		final Long locId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralDatum entity = GeneralDatum.locationDatum(locId, sourceId, ts,
				new DatumSamples(iData, aData, null));

		// WHEN
		String serialized = service.serialize(entity);
		DatumPK id = service.entityId(entity);
		Object deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("created", ISO_DATE_TIME_ALT_UTC.format(ts)),
				entry("locationId", locId),
				entry("sourceId", sourceId),
				entry("i", iData),
				entry("a", aData)
			)
			;

		then(id)
			.as("DatumPK for unassigned stream returned")
			.isEqualTo(ObjectDatumPK.unassignedStream(
				ObjectDatumKind.Location,
				locId,
				sourceId,
				ts)
			)
			;

		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to GeneralDatum instance")
			.isEqualTo(entity)
			;
		// @formatter:on
	}

	@Test
	public void codec_GeneralObjectDatum_Node() {
		// GIVEN
		final var service = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		final Long nodeId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralNodeDatum entity = new GeneralNodeDatum(nodeId, ts, sourceId);
		entity.setSamples(new DatumSamples(iData, aData, null));

		// WHEN
		String serialized = service.serialize(entity);
		DatumPK id = service.entityId(entity);
		Object deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("created", ISO_DATE_TIME_ALT_UTC.format(ts)),
				entry("nodeId", nodeId),
				entry("sourceId", sourceId),
				entry("i", iData),
				entry("a", aData)
			)
			;

		then(id)
			.as("DatumPK for unassigned stream returned")
			.isEqualTo(ObjectDatumPK.unassignedStream(
				ObjectDatumKind.Node,
				nodeId,
				sourceId,
				ts)
			)
			;

		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to GeneralDatum instance")
			.isEqualTo(new GeneralDatum(
				DatumId.datumId(ObjectDatumKind.Node, nodeId, sourceId, ts),
				entity.getSamples())
			);
			;
		// @formatter:on
	}

	@Test
	public void codec_GeneralObjectDatum_Location() {
		// GIVEN
		final var service = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		final Long locId = randomLong();
		final String sourceId = randomString();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final Map<String, Number> iData = Map.of("a", 1);
		final Map<String, Number> aData = Map.of("b", 2);
		final GeneralLocationDatum entity = new GeneralLocationDatum(locId, ts, sourceId);
		entity.setSamples(new DatumSamples(iData, aData, null));

		// WHEN
		String serialized = service.serialize(entity);
		DatumPK id = service.entityId(entity);
		Object deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isObject()
			.containsOnly(
				entry("created", ISO_DATE_TIME_ALT_UTC.format(ts)),
				entry("locationId", locId),
				entry("sourceId", sourceId),
				entry("i", iData),
				entry("a", aData)
			)
			;

		then(id)
			.as("DatumPK for unassigned stream returned")
			.isEqualTo(ObjectDatumPK.unassignedStream(
				ObjectDatumKind.Location,
				locId,
				sourceId,
				ts)
			)
			;

		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to GeneralDatum instance")
			.isEqualTo(new GeneralDatum(
				DatumId.datumId(ObjectDatumKind.Location, locId, sourceId, ts),
				entity.getSamples())
			);
			;
		// @formatter:on
	}

	@Test
	public void codec_StreamDatum() {
		// GIVEN
		final var service = new DatumJsonEntityCodec(stats, JSON_MAPPER);

		final UUID streamId = UUID.randomUUID();
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final DatumProperties properties = DatumProperties
				.propertiesOf(NumberUtils.decimalArray("1.0", "2.0"), null, null, null);
		final BasicStreamDatum entity = new BasicStreamDatum(streamId, ts, properties);

		// WHEN
		String serialized = service.serialize(entity);
		DatumPK id = service.entityId(entity);
		Object deserialized = service.deserialize(serialized);

		// THEN
		// @formatter:off
		then(serialized)
			.asInstanceOf(JSON)
			.isArray()
			.containsExactly(
				ts.toEpochMilli(),
				streamId.getMostSignificantBits(),
				streamId.getLeastSignificantBits(),
				properties.getInstantaneous(),
				null,
				null,
				null
			)
			;

		then(id)
			.as("DatumPK for stream returned")
			.isEqualTo(new DatumPK(streamId, ts))
			;

		then(deserialized)
			.usingRecursiveComparison()
			.as("Deserialized back to StreamDatum instance")
			.isEqualTo(entity)
			;
		// @formatter:on
	}

}
