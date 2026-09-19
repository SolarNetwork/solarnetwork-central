/* ==================================================================
 * PropertyRestrictedObjectDatumSteramMetadataTests.java - 29/01/2026 7:23:10 am
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

package net.solarnetwork.central.datum.v2.domain.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomDecimal;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumSamplesType.Accumulating;
import static net.solarnetwork.domain.datum.DatumSamplesType.Instantaneous;
import static net.solarnetwork.domain.datum.DatumSamplesType.Status;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.PropertyRestrictedObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics.AccumulatingStatistic;
import net.solarnetwork.domain.datum.DatumPropertiesStatistics.InstantaneousStatistic;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link PropertyRestrictedObjectDatumStreamMetadata} class.
 *
 * @author matt
 * @version 1.0
 */
public class PropertyRestrictedObjectDatumStreamMetadataTests {

	@Test
	public void restricted_partial() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta,
				Set.of("two", "four", "six"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(new String[] {"two", "four", "six"}, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(new String[] {"two"}, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(new String[] {"four"}, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(new String[] {"six"}, from(m -> m.propertyNamesForType(Status)))
			.as("Instantaneous mapping returned")
			.returns(new int[] {1}, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("Accumulating mapping returned")
			.returns(new int[] {0}, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Status mapping returned")
			.returns(new int[] {0}, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_partial_more() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three", "four" },
				new String[] { "five", "six", "seven", "eight" },
				new String[] { "nine", "ten", "eleven", "twelve" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta,
				Set.of("two", "four", "six", "eight", "eleven"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(new String[] {"two", "four", "six", "eight", "eleven"}, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(new String[] {"two", "four"}, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(new String[] {"six", "eight"}, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(new String[] {"eleven"}, from(m -> m.propertyNamesForType(Status)))
			.as("Instantaneous mapping returned")
			.returns(new int[] {1, 3}, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("Accumulating mapping returned")
			.returns(new int[] {1, 3}, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Status mapping returned")
			.returns(new int[] {2}, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_toEmpty() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta, Set.of("foo", "bar"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(null, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Status)))
			.as("Empty instantaneous mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("Accumulating mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Status mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_toEmpty_instantaneous() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta, Set.of("five", "six"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(new String[] {"five", "six"}, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(new String[] {"five"}, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(new String[] {"six"}, from(m -> m.propertyNamesForType(Status)))
			.as("Empty instantaneous mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("Accumulating mapping returned")
			.returns(new int[] {1}, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Status mapping returned")
			.returns(new int[] {0}, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_toEmpty_accumulating() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta, Set.of("two", "six"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(new String[] {"two", "six"}, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(new String[] {"two"}, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(new String[] {"six"}, from(m -> m.propertyNamesForType(Status)))
			.as("Instantaneous mapping returned")
			.returns(new int[] {1}, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("Empty accumulating mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Status mapping returned")
			.returns(new int[] {0}, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_toEmpty_status() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta, Set.of("two", "four"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Restricted property names returned")
			.returns(new String[] {"two", "four"}, from(PropertyRestrictedObjectDatumStreamMetadata::getPropertyNames))
			.as("Restricted instantaneous property names returned")
			.returns(new String[] {"two"}, from(m -> m.propertyNamesForType(Instantaneous)))
			.as("Restricted accumulating property names returned")
			.returns(new String[] {"four"}, from(m -> m.propertyNamesForType(Accumulating)))
			.as("Restricted status property names returned")
			.returns(null, from(m -> m.propertyNamesForType(Status)))
			.as("Instantaneous mapping returned")
			.returns(new int[] {1}, from(m -> m.propertyMappingForType(Instantaneous)))
			.as("accumulating mapping returned")
			.returns(new int[] {0}, from(m -> m.propertyMappingForType(Accumulating)))
			.as("Empty status mapping returned")
			.returns(null, from(m -> m.propertyMappingForType(Status)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_values() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		final var iData = new BigDecimal[] { randomDecimal(), randomDecimal(), randomDecimal() };
		final var aData = new BigDecimal[] { randomDecimal(), randomDecimal() };
		final var sData = new String[] { randomString(), randomString() };

		final var data = new DatumProperties();
		data.setInstantaneous(iData);
		data.setAccumulating(aData);
		data.setStatus(sData);

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta,
				Set.of("two", "four", "six", "seven"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Mapping Instantaneous value returned")
			.returns(iData[1], from(m -> m.value(data, Instantaneous, 0)))
			.as("Mapping Instantaneous value returned (invalid)")
			.returns(null, from(m -> m.value(data, Instantaneous, 1)))
			.as("Mapping Accumulating value returned")
			.returns(aData[0], from(m -> m.value(data, Accumulating, 0)))
			.as("Mapping Accumulating value returned (invalid)")
			.returns(null, from(m -> m.value(data, Accumulating, 1)))
			.as("Mapping Status value returned")
			.returns(sData[0], from(m -> m.value(data, Status, 0)))
			.as("Mapping Status value returned (invalid)")
			.returns(sData[1], from(m -> m.value(data, Status, 1)))
			.as("Mapping Status value returned (invalid)")
			.returns(null, from(m -> m.value(data, Status, 2)))
			;
		// @formatter:on
	}

	@Test
	public void restricted_stats() {
		// GIVEN
		final var srcMeta = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC", ObjectDatumKind.Node,
				randomLong(), randomString(), new String[] { "one", "two", "three" },
				new String[] { "four", "five" }, new String[] { "six", "seven" });

		// @formatter:off
		final var iStats = new BigDecimal[][] {
			new BigDecimal[] {randomDecimal(), randomDecimal(), randomDecimal() },
			new BigDecimal[] {randomDecimal(), randomDecimal(), randomDecimal() },
			new BigDecimal[] {randomDecimal(), randomDecimal(), randomDecimal() }
		};
		final var aStats = new BigDecimal[][] {
			new BigDecimal[] {randomDecimal(), randomDecimal(), randomDecimal() },
			new BigDecimal[] {randomDecimal(), randomDecimal(), randomDecimal() }
		};
		// @formatter:on

		final var stats = new DatumPropertiesStatistics();
		stats.setInstantaneous(iStats);
		stats.setAccumulating(aStats);

		// WHEN
		final var meta = new PropertyRestrictedObjectDatumStreamMetadata(srcMeta,
				Set.of("two", "four", "six", "seven"));

		// THEN
		// @formatter:off
		then(meta)
			.as("Mapping Instantaneous Count value returned")
			.returns(iStats[1][0], from(m -> m.stat(stats, InstantaneousStatistic.Count, 0)))
			.as("Mapping Instantaneous Minimum value returned")
			.returns(iStats[1][1], from(m -> m.stat(stats, InstantaneousStatistic.Minimum, 0)))
			.as("Mapping Instantaneous Maximum value returned")
			.returns(iStats[1][2], from(m -> m.stat(stats, InstantaneousStatistic.Maximum, 0)))
			.as("Mapping Instantaneous value returned (invalid)")
			.returns(null, from(m -> m.stat(stats, InstantaneousStatistic.Count, 1)))
			.as("Mapping Accumulating Difference value returned")
			.returns(aStats[0][0], from(m -> m.stat(stats, AccumulatingStatistic.Difference, 0)))
			.as("Mapping Accumulating Start value returned")
			.returns(aStats[0][1], from(m -> m.stat(stats, AccumulatingStatistic.Start, 0)))
			.as("Mapping Accumulating End value returned")
			.returns(aStats[0][2], from(m -> m.stat(stats, AccumulatingStatistic.End, 0)))
			.as("Mapping Accumulating value returned (invalid)")
			.returns(null, from(m -> m.stat(stats, AccumulatingStatistic.Difference, 1)))
			;
		// @formatter:on
	}

}
