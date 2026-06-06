/* ==================================================================
 * OrderedDatumSamplesBufferTests.java - 6/06/2026 1:48:50 pm
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

import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.support.OrderedDatumSamplesBuffer;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumStreamId.DatumStreamIdent;
import net.solarnetwork.domain.datum.DatumStreamIdentity;
import net.solarnetwork.domain.datum.GeneralDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link OrderedDatumSamplesBuffer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OrderedDatumSamplesBufferTests {

	private SortedMap<DatumStreamIdentity, SortedMap<Instant, DatumSamples>> data;
	private MutableBoolean isNew;

	@BeforeEach
	public void setup() {
		data = new TreeMap<>();
		isNew = new MutableBoolean(false);
	}

	@Test
	public void getOrCreate_create() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer(data, _ -> new TreeMap<>());
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());
		final var now = Instant.now();

		// WHEN
		isNew.setFalse();
		final var result = buffer.getOrCreate(streamId, now, isNew);

		// THEN
		// @formatter:off
		then(result)
			.as("Samples created new")
			.isNotNull()
			.as("Samples is empty")
			.returns(true, from(DatumSamples::isEmpty))
			;
		
		then(isNew.booleanValue()).as("New flag set").isTrue();
		// @formatter:on
	}

	@Test
	public void getOrCreate_get() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer(data, _ -> new TreeMap<>());
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());
		final var now = Instant.now();
		final var samples = new DatumSamples();

		// populate an existing value for our key/ts
		data.put(streamId, new TreeMap<>(Map.of(now, samples)));

		// WHEN
		isNew.setTrue();
		final var result = buffer.getOrCreate(streamId, now, isNew);

		// THEN
		// @formatter:off
		then(result)
			.as("Existing samples returned")
			.isSameAs(samples)
			;
		
		then(isNew.booleanValue()).as("New flag unset").isFalse();
		// @formatter:on
	}

	@Test
	public void datum_empty() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer();

		// WHEN
		final List<GeneralDatum> result = buffer.datum(GeneralDatum::new);

		// THEN
		then(result).as("Empty datum list returned when no datum present").isEmpty();
	}

	@Test
	public void datum_empty_streamMapPresent() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer(data, _ -> new TreeMap<>());
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());

		// populate an existing empty stream map
		data.put(streamId, new TreeMap<>());

		// WHEN
		final List<GeneralDatum> result = buffer.datum(GeneralDatum::new);

		// THEN
		then(result).as("Empty datum list returned when no datum present").isEmpty();
	}

	@Test
	public void previousTimestamp_empty() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer();
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());

		// WHEN
		final Instant result = buffer.previousTimestamp(streamId, Instant.now());

		// THEN
		then(result).as("Null result when buffer is empty");
	}

	@Test
	public void previousTimestamp_none() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer();
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());
		final Instant ts = Instant.now();

		buffer.getOrCreate(streamId, ts);

		// WHEN
		final Instant result = buffer.previousTimestamp(streamId, ts);

		// THEN
		then(result).as("Null result when no earlier mapping exists");
	}

	@Test
	public void previousTimestamp() {
		// GIVEN
		final var buffer = new OrderedDatumSamplesBuffer();
		final var streamId = new DatumStreamIdent(ObjectDatumKind.Node, randomLong(), randomSourceId());
		final Instant start = Instant.now();

		List<Instant> timestamps = new ArrayList<>();
		for ( int i = 0; i < 10; i++ ) {
			Instant ts = start.plusSeconds(i);
			buffer.getOrCreate(streamId, ts);
			timestamps.add(ts);
		}

		for ( int i = 0; i < timestamps.size(); i++ ) {
			// WHEN
			final Instant result = buffer.previousTimestamp(streamId, timestamps.get(i));

			// THEN
			if ( i == 0 ) {
				then(result).as("Null result when no earlier mapping exists");
			} else {
				then(result).as("Next earlier timestamp returned").isEqualTo(timestamps.get(i - 1));
			}
		}
	}

	@Test
	public void datum() {
		// GIVEN
		final int objCount = RNG.nextInt(10) + 1;
		final int srcCount = RNG.nextInt(10) + 1;
		final int tsCount = RNG.nextInt(10) + 1;

		final var buffer = new OrderedDatumSamplesBuffer(data, _ -> new TreeMap<>());

		final List<GeneralDatum> expectedDatum = new ArrayList<>(objCount * srcCount * tsCount);

		for ( int objIdx = 0; objIdx < objCount; objIdx++ ) {
			final Long objectId = randomLong();
			for ( int srcIdx = 0; srcIdx < srcCount; srcIdx++ ) {
				final String sourceId = randomSourceId();
				final DatumStreamIdent streamId = new DatumStreamIdent(
						RNG.nextBoolean() ? ObjectDatumKind.Node : ObjectDatumKind.Location, objectId,
						sourceId);
				final Instant tsStart = Instant.now().truncatedTo(ChronoUnit.SECONDS)
						.minusSeconds(RNG.nextLong(1000L));
				Instant ts = tsStart;
				for ( int tsIdx = 0; tsIdx < tsCount; tsIdx++ ) {
					DatumSamples s = buffer.getOrCreate(streamId, ts);
					s.putInstantaneousSampleValue("o", objIdx);
					s.putInstantaneousSampleValue("s", srcIdx);
					s.putInstantaneousSampleValue("t", tsIdx);
					expectedDatum.add(new GeneralDatum(streamId.datumIdentity(ts), s));
					ts = ts.plusSeconds(RNG.nextLong(600) + 1);
				}
			}
		}

		expectedDatum.sort(null);

		// WHEN
		final List<GeneralDatum> result = buffer.datum(GeneralDatum::new);

		// THEN
		then(result).as("Datum list generated from data").containsExactlyElementsOf(expectedDatum);
	}

	@Test
	public void greatestTimestampsPerStream() {
		// GIVEN
		final int objCount = RNG.nextInt(10) + 1;
		final int srcCount = RNG.nextInt(10) + 1;
		final int tsCount = RNG.nextInt(10) + 1;

		final var buffer = new OrderedDatumSamplesBuffer(data, _ -> new TreeMap<>());

		final SortedMap<DatumStreamIdentity, Instant> expectedTs = new TreeMap<>();

		for ( int objIdx = 0; objIdx < objCount; objIdx++ ) {
			final Long objectId = randomLong();
			for ( int srcIdx = 0; srcIdx < srcCount; srcIdx++ ) {
				final String sourceId = randomSourceId();
				final DatumStreamIdent streamId = new DatumStreamIdent(
						RNG.nextBoolean() ? ObjectDatumKind.Node : ObjectDatumKind.Location, objectId,
						sourceId);
				final Instant tsStart = Instant.now().truncatedTo(ChronoUnit.SECONDS)
						.minusSeconds(RNG.nextLong(1000L));
				Instant ts = tsStart;
				for ( int tsIdx = 0; tsIdx < tsCount; tsIdx++ ) {
					ts = ts.plusSeconds(RNG.nextLong(600) + 1);
					DatumSamples s = buffer.getOrCreate(streamId, ts);
					s.putInstantaneousSampleValue("o", objIdx);
					s.putInstantaneousSampleValue("s", srcIdx);
					s.putInstantaneousSampleValue("t", tsIdx);
				}
				expectedTs.put(streamId, ts);
			}
		}

		// WHEN
		final SortedMap<DatumStreamIdentity, Instant> result = buffer.greatestTimestampPerStream();

		// THEN
		then(result).as("Greatest timestamp per stream returend").containsExactlyEntriesOf(expectedTs);
	}

	@Test
	public void leastTimestamp_empty() {
		// WHEN
		final Instant result = OrderedDatumSamplesBuffer.leastTimestamp(List.of());

		// THEN
		then(result).as("Null timestamp returned for empty list").isNull();
	}

	@Test
	public void leastTimestamp() {
		// GIVEN
		final var now = Instant.now();
		final List<Instant> data = new ArrayList<>();
		long maxSecs = 0;
		for ( int i = 0; i < 10; i++ ) {
			long secs = RNG.nextLong(100);
			if ( secs > maxSecs ) {
				maxSecs = secs;
			}
			data.add(now.minusSeconds(secs));
		}

		// WHEN
		final Instant result = OrderedDatumSamplesBuffer.leastTimestamp(data);

		// THEN
		then(result).as("Least timestamp returned").isEqualTo(now.minusSeconds(maxSecs));
	}

	@Test
	public void greatestTimestamp_empty() {
		// WHEN
		final Instant result = OrderedDatumSamplesBuffer.greatestTimestamp(List.of());

		// THEN
		then(result).as("Null timestamp returned for empty list").isNull();
	}

	@Test
	public void greatestTimestamp() {
		// GIVEN
		final var now = Instant.now();
		final List<Instant> data = new ArrayList<>();
		long maxSecs = 0;
		for ( int i = 0; i < 10; i++ ) {
			long secs = RNG.nextLong(100);
			if ( secs > maxSecs ) {
				maxSecs = secs;
			}
			data.add(now.plusSeconds(secs));
		}

		// WHEN
		final Instant result = OrderedDatumSamplesBuffer.greatestTimestamp(data);

		// THEN
		then(result).as("Greatest timestamp returned").isEqualTo(now.plusSeconds(maxSecs));
	}

}
