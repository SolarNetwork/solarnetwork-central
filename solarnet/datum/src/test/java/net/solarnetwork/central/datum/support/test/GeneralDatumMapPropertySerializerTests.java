/* ==================================================================
 * GeneralDatumMapPropertySerializerTests.java - 20/02/2026 10:25:27 am
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
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.support.GeneralDatumMapPropertySerializer;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test cases for the {@link GeneralDatumMapPropertySerializer} class.
 *
 * @author matt
 * @version 1.0
 */
public class GeneralDatumMapPropertySerializerTests {

	@Test
	public void serialize_GeneralDatum() {
		// GIVEN
		final GeneralDatum d = new GeneralDatum(
				DatumId.nodeId(randomLong(), randomString(), Instant.now()),
				new DatumSamples(Map.of("a", 1, "b", 2), Map.of("c", 3), Map.of("d", "four")));

		// WHEN
		final Object result = new GeneralDatumMapPropertySerializer().serialize(null, null, d);

		// THEN
		// @formatter:off
		then(result)
			.as("Result serialized to Map")
			.isInstanceOf(Map.class)
			.asInstanceOf(map(String.class, Object.class))
			.containsOnly(
				entry("created", d.getTimestamp()),
				entry("kind", d.getKind()),
				entry("objectId", d.getObjectId()),
				entry("sourceId", d.getSourceId()),
				entry("a", d.getSampleValue(Instantaneous, "a")),
				entry("b", d.getSampleValue(Instantaneous, "b")),
				entry("c", d.getSampleValue(Accumulating, "c")),
				entry("d", d.getSampleValue(Status, "d"))
			)
			;
		// @formatter:on
	}

	@Test
	public void serialize_GeneralDatum_withTags() {
		// GIVEN
		final GeneralDatum d = new GeneralDatum(
				DatumId.nodeId(randomLong(), randomString(), Instant.now()),
				new DatumSamples(Map.of("a", 1, "b", 2), Map.of("c", 3), Map.of("d", "four")));
		d.getSamples().setTags(Set.of("foo", "bar"));

		// WHEN
		final Object result = new GeneralDatumMapPropertySerializer().serialize(null, null, d);

		// THEN
		// @formatter:off
		then(result)
			.as("Result serialized to Map, including tags")
			.isInstanceOf(Map.class)
			.asInstanceOf(map(String.class, Object.class))
			.containsOnly(
				entry("created", d.getTimestamp()),
				entry("kind", d.getKind()),
				entry("objectId", d.getObjectId()),
				entry("sourceId", d.getSourceId()),
				entry("a", d.getSampleValue(Instantaneous, "a")),
				entry("b", d.getSampleValue(Instantaneous, "b")),
				entry("c", d.getSampleValue(Accumulating, "c")),
				entry("d", d.getSampleValue(Status, "d")),
				entry("tags", "foo;bar")
			)
			;
		// @formatter:on
	}

	@Test
	public void serialize_ObjectDatum() {
		// GIVEN
		final ObjectDatum d = new ObjectDatum(
				DatumId.nodeId(randomLong(), randomString(), Instant.now()),
				new DatumSamples(Map.of("a", 1, "b", 2), Map.of("c", 3), Map.of("d", "four")),
				UUID.randomUUID(), new DatumProperties());

		// WHEN
		final Object result = new GeneralDatumMapPropertySerializer().serialize(null, null, d);

		// THEN
		// @formatter:off
		then(result)
			.as("Result serialized to Map, including streamId")
			.isInstanceOf(Map.class)
			.asInstanceOf(map(String.class, Object.class))
			.containsOnly(
				entry("created", d.getTimestamp()),
				entry("kind", d.getKind()),
				entry("objectId", d.getObjectId()),
				entry("sourceId", d.getSourceId()),
				entry("streamId", d.getStreamId()),
				entry("a", d.getSampleValue(Instantaneous, "a")),
				entry("b", d.getSampleValue(Instantaneous, "b")),
				entry("c", d.getSampleValue(Accumulating, "c")),
				entry("d", d.getSampleValue(Status, "d"))
			)
			;
		// @formatter:on
	}

}
