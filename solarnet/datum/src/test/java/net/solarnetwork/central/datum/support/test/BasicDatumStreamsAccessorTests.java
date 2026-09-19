/* ==================================================================
 * BasicDatumStreamsAccessorTests.java - 15/11/2024 8:41:33 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.datum.support.BasicDatumStreamsAccessor;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test cases for the {@link BasicDatumStreamsAccessor} class.
 *
 * @author matt
 * @version 1.5
 */
public class BasicDatumStreamsAccessorTests {

	private PathMatcher sourceIdPathMatcher;
	private Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
	private Long nodeId;

	@BeforeEach
	public void setup() {
		var pm = new AntPathMatcher();
		pm.setCachePatterns(false);
		pm.setCaseSensitive(false);
		sourceIdPathMatcher = pm;

		nodeId = CommonTestUtils.randomLong();
	}

	private static String testSource(int idx) {
		return "test/" + idx;
	}

	private List<GeneralDatum> testData(int sourceCount, int datumCount) {
		List<GeneralDatum> result = new ArrayList<>(sourceCount * datumCount);
		for ( int s = 0; s < sourceCount; s++ ) {
			String sourceId = testSource(s);
			for ( int i = 0; i < datumCount; i++ ) {
				Instant ts = now.minusSeconds(datumCount - i - 1); // generate in time order
				DatumSamples ds = new DatumSamples();
				ds.putInstantaneousSampleValue("v", i);
				result.add(GeneralDatum.nodeDatum(nodeId, sourceId, ts, ds));
			}
		}
		return result;
	}

	@Test
	public void offset() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = testSource(sourceIdx);
		final int offset = RNG.nextInt(datumCount);
		Datum result = accessor.offset(Node, nodeId, sourceId, offset);

		// THEN
		// @formatter:off
		then(result)
			.as("Offset %d for source returned", offset)
			.isSameAs(data.get((sourceIdx * sourceCount) + (datumCount - offset - 1)))
			;
		// @formatter:on
	}

	@Test
	public void offsetMatching() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "test/*";
		final int offset = RNG.nextInt(datumCount);
		Collection<Datum> result = accessor.offsetMatching(Node, nodeId, sourceIdPath, offset);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.add(data.get((sourceIdx * sourceCount) + (datumCount - offset - 1)));
		}
		then(result)
			.as("Offset %d for source returned", offset)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void latest_time() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = testSource(sourceIdx);
		final int offset = RNG.nextInt(datumCount);
		Datum result = accessor.latest(Node, nodeId, sourceId, now.minusSeconds(offset));

		// THEN
		// @formatter:off
		then(result)
			.as("Latest from %s (offset %d) for source returned", now.minusSeconds(offset), offset)
			.isSameAs(data.get((sourceIdx * sourceCount) + (datumCount - offset - 1)))
			;
		// @formatter:on
	}

	@Test
	public void latestMatching_time() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "test/*";
		final int offset = RNG.nextInt(datumCount);
		Collection<Datum> result = accessor.latestMatching(Node, nodeId, sourceIdPath,
				now.minusSeconds(offset));

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.add(data.get((sourceIdx * sourceCount) + (datumCount - offset - 1)));
		}
		then(result)
			.as("Latest from %s (offset %d) for source returned", now.minusSeconds(offset), offset)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void offset_time() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = testSource(sourceIdx);
		final int timeOffset = RNG.nextInt(datumCount);
		final int offset = datumCount - timeOffset - 1 > 0 ? RNG.nextInt(datumCount - timeOffset - 1)
				: 0;
		Datum result = accessor.offset(Node, nodeId, sourceId, now.minusSeconds(timeOffset), offset);

		// THEN
		// @formatter:off
		then(result)
			.as("Offset %d from %s for source returned", offset, now.minusSeconds(timeOffset))
			.isSameAs(data.get((sourceIdx * sourceCount) + (datumCount - timeOffset - offset - 1)))
			;
		// @formatter:on
	}

	@Test
	public void offset_time_duplicateInput() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = testSource(sourceIdx);
		final int timeOffset = RNG.nextInt(datumCount);
		final int offset = datumCount - timeOffset - 1 > 0 ? RNG.nextInt(datumCount - timeOffset - 1)
				: 0;

		// insert a duplicate datum, for our timestamp
		final int keyIndex = (sourceIdx * sourceCount) + (datumCount - timeOffset - offset - 1);
		final GeneralDatum keyDatum = data.get(keyIndex);
		final GeneralDatum keyDatumDuplicate = keyDatum.clone();
		data.add(keyIndex, keyDatumDuplicate);

		// WHEN
		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);
		Datum result = accessor.offset(Node, nodeId, sourceId, now.minusSeconds(timeOffset), offset);

		// THEN
		// @formatter:off
		then(result)
			.as("Duplicate offset %d from %s for source returned because original discarded",
					offset, now.minusSeconds(timeOffset))
			.isSameAs(keyDatumDuplicate)
			;
		// @formatter:on
	}

	@Test
	public void offsetMatching_time() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "test/*";
		final int timeOffset = RNG.nextInt(datumCount);
		final int offset = datumCount - timeOffset - 1 > 0 ? RNG.nextInt(datumCount - timeOffset - 1)
				: 0;
		Collection<Datum> result = accessor.offsetMatching(Node, nodeId, sourceIdPath,
				now.minusSeconds(timeOffset), offset);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.add(data.get((sourceIdx * sourceCount) + (datumCount - timeOffset - offset - 1)));
		}
		then(result)
			.as("Offset %d from %s for source returned", offset, now.minusSeconds(offset))
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void at() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = "test/%d".formatted(sourceIdx);
		final int timeOffset = RNG.nextInt(datumCount);
		final Instant ts = now.minusSeconds(timeOffset);
		Datum result = accessor.at(Node, nodeId, sourceId, ts);

		// THEN
		// @formatter:off
		Datum expected = data.get((sourceIdx * sourceCount) + (datumCount - timeOffset - 1));
		then(result)
			.as("Timestamp %s for source returned", ts)
			.isEqualTo(expected)
			;
		// @formatter:on
	}

	@Test
	public void at_notFoundForTimestamp() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int sourceIdx = RNG.nextInt(sourceCount);
		final String sourceId = "test/%d".formatted(sourceIdx);
		final Instant ts = now.plusSeconds(1);
		Datum result = accessor.at(Node, nodeId, sourceId, ts);

		// THEN
		// @formatter:off
		then(result)
			.as("Datum for timestamp %s does not exist", ts)
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void at_notFoundForSourceId() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceId = "test/does/not/exist";
		final Instant ts = now;
		Datum result = accessor.at(Node, nodeId, sourceId, ts);

		// THEN
		// @formatter:off
		then(result)
			.as("Datum for sourceId %s does not exist", sourceId)
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void atMatching() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "test/*";
		final int timeOffset = RNG.nextInt(datumCount);
		final Instant ts = now.minusSeconds(timeOffset);
		Collection<Datum> result = accessor.atMatching(Node, nodeId, sourceIdPath, ts);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.add(data.get((sourceIdx * sourceCount) + (datumCount - timeOffset - 1)));
		}
		then(result)
			.as("Timestamp %s for source returned", ts)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void atMatching_notFoundForTimestamp() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "test/*";
		final Instant ts = now.plusSeconds(1);
		Collection<Datum> result = accessor.atMatching(Node, nodeId, sourceIdPath, ts);

		// THEN
		// @formatter:off
		then(result)
			.as("Timestamp %s not found", ts)
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void atMatching_notFoundForSourceId() {
		// GIVEN
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final String sourceIdPath = "none/*";
		final Instant ts = now.plusSeconds(1);
		Collection<Datum> result = accessor.atMatching(Node, nodeId, sourceIdPath, ts);

		// THEN
		// @formatter:off
		then(result)
			.as("Source %s not found", sourceIdPath)
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void findRangeMatching_within() {
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int startOffset = 3;
		final int endOffset = 1;
		final Instant from = now.minusSeconds(startOffset);
		final Instant to = now.minusSeconds(endOffset);
		final String sourceIdPath = "test/*";
		Collection<Datum> result = accessor.rangeMatching(Node, nodeId, sourceIdPath, from, to);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.addAll(data.subList(
					(sourceIdx * sourceCount) + (datumCount - startOffset - 1),
					(sourceIdx * sourceCount) + (datumCount - endOffset - 1)));
		}
		then(result)
			.as("Range [%s - %s] (offset [%d - %d]) returned", from, to, startOffset, endOffset)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void findRangeMatching_leading() {
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int startOffset = 8;
		final int endOffset = 3;
		final Instant from = now.minusSeconds(startOffset);
		final Instant to = now.minusSeconds(endOffset);
		final String sourceIdPath = "test/*";
		Collection<Datum> result = accessor.rangeMatching(Node, nodeId, sourceIdPath, from, to);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.addAll(data.subList(
					(sourceIdx * sourceCount),
					(sourceIdx * sourceCount) + (datumCount - endOffset - 1)));
		}
		then(result)
			.as("Range [%s - %s] (offset [%d - %d]) returned", from, to, startOffset, endOffset)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

	@Test
	public void findRangeMatching_trailing() {
		final int sourceCount = 5;
		final int datumCount = 5;
		final List<GeneralDatum> data = testData(sourceCount, datumCount);

		var accessor = new BasicDatumStreamsAccessor(sourceIdPathMatcher, data);

		// WHEN
		final int startOffset = 3;
		final int endOffset = -5;
		final Instant from = now.minusSeconds(startOffset);
		final Instant to = now.minusSeconds(endOffset);
		final String sourceIdPath = "test/*";
		Collection<Datum> result = accessor.rangeMatching(Node, nodeId, sourceIdPath, from, to);

		// THEN
		// @formatter:off
		List<Datum> expected = new ArrayList<>(sourceCount);
		for ( int sourceIdx = 0; sourceIdx < sourceCount; sourceIdx++)  {
			expected.addAll(data.subList(
					(sourceIdx * sourceCount) + (datumCount - startOffset - 1),
					(sourceIdx * sourceCount) + datumCount));
		}
		then(result)
			.as("Range [%s - %s] (offset [%d - %d]) returned", from, to, startOffset, endOffset)
			.hasSameElementsAs(expected)
			;
		// @formatter:on
	}

}
