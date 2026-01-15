/* ==================================================================
 * ObjectDatumIdTests.java - 11/02/2025 3:05:51 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link ObjectDatumId} class.
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumIdTests {

	private Clock clock;

	@BeforeEach
	public void setup() {
		clock = Clock.fixed(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
	}

	@Test
	public void json_ser_node() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);

		// WHEN
		var json = JsonUtils.getJSONString(id);

		// THEN
		JSONAssert.assertEquals("""
				{
					"kind" : "n",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp())), json, true);
	}

	@Test
	public void json_ser_location() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.locationId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);

		// WHEN
		var json = JsonUtils.getJSONString(id);

		// THEN
		JSONAssert.assertEquals("""
				{
					"kind" : "l",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp())), json, true);
	}

	@Test
	public void json_deser_node() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var json = """
					{
					"kind" : "n",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp()));

		// WHEN
		var result = JsonUtils.getObjectFromJSON(json, ObjectDatumId.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Subclass parsed")
			.isInstanceOf(ObjectDatumId.NodeDatumId.class)
			.as("Kind parsed")
			.returns(ObjectDatumKind.Node, from(ObjectDatumId::getKind))
			.as("Stream ID parsed")
			.returns(id.getStreamId(), from(ObjectDatumId::getStreamId))
			.as("Object ID parsed")
			.returns(id.getObjectId(), from(ObjectDatumId::getObjectId))
			.as("Source ID parsed")
			.returns(id.getSourceId(), from(ObjectDatumId::getSourceId))
			.as("Timestamp parsed")
			.returns(id.getTimestamp(), from(ObjectDatumId::getTimestamp))
			.as("Aggregation parsed")
			.returns(id.getAggregation(), from(ObjectDatumId::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void json_deser_node_fullType() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var json = """
					{
					"kind" : "Node",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp()));

		// WHEN
		var result = JsonUtils.getObjectFromJSON(json, ObjectDatumId.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Subclass parsed")
			.isInstanceOf(ObjectDatumId.NodeDatumId.class)
			.as("Kind parsed")
			.returns(ObjectDatumKind.Node, from(ObjectDatumId::getKind))
			.as("Stream ID parsed")
			.returns(id.getStreamId(), from(ObjectDatumId::getStreamId))
			.as("Object ID parsed")
			.returns(id.getObjectId(), from(ObjectDatumId::getObjectId))
			.as("Source ID parsed")
			.returns(id.getSourceId(), from(ObjectDatumId::getSourceId))
			.as("Timestamp parsed")
			.returns(id.getTimestamp(), from(ObjectDatumId::getTimestamp))
			.as("Aggregation parsed")
			.returns(id.getAggregation(), from(ObjectDatumId::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void json_deser_noKind() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var json = """
					{
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp()));

		// WHEN
		var result = JsonUtils.getObjectFromJSON(json, ObjectDatumId.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Subclass defaulted to node")
			.isInstanceOf(ObjectDatumId.NodeDatumId.class)
			.as("Kind parsed")
			.returns(ObjectDatumKind.Node, from(ObjectDatumId::getKind))
			.as("Stream ID parsed")
			.returns(id.getStreamId(), from(ObjectDatumId::getStreamId))
			.as("Object ID parsed")
			.returns(id.getObjectId(), from(ObjectDatumId::getObjectId))
			.as("Source ID parsed")
			.returns(id.getSourceId(), from(ObjectDatumId::getSourceId))
			.as("Timestamp parsed")
			.returns(id.getTimestamp(), from(ObjectDatumId::getTimestamp))
			.as("Aggregation parsed")
			.returns(id.getAggregation(), from(ObjectDatumId::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void json_deser_location() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.locationId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var json = """
					{
					"kind" : "l",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp()));

		// WHEN
		var result = JsonUtils.getObjectFromJSON(json, ObjectDatumId.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Subclass parsed")
			.isInstanceOf(ObjectDatumId.LocationDatumId.class)
			.as("Kind parsed")
			.returns(ObjectDatumKind.Location, from(ObjectDatumId::getKind))
			.as("Stream ID parsed")
			.returns(id.getStreamId(), from(ObjectDatumId::getStreamId))
			.as("Object ID parsed")
			.returns(id.getObjectId(), from(ObjectDatumId::getObjectId))
			.as("Source ID parsed")
			.returns(id.getSourceId(), from(ObjectDatumId::getSourceId))
			.as("Timestamp parsed")
			.returns(id.getTimestamp(), from(ObjectDatumId::getTimestamp))
			.as("Aggregation parsed")
			.returns(id.getAggregation(), from(ObjectDatumId::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void json_deser_location_fullType() throws JSONException {
		// GIVEN
		var id = ObjectDatumId.locationId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var json = """
					{
					"kind" : "Location",
					"streamId" : "%s",
					"objectId" : %d,
					"sourceId" :  "%s",
					"timestamp" : "%s",
					"aggregation" : "None"
				}
				""".formatted(id.getStreamId(), id.getObjectId(), id.getSourceId(),
				ISO_DATE_TIME_ALT_UTC.format(id.getTimestamp()));

		// WHEN
		var result = JsonUtils.getObjectFromJSON(json, ObjectDatumId.class);

		// THEN
		// @formatter:off
		then(result)
			.as("Subclass parsed")
			.isInstanceOf(ObjectDatumId.LocationDatumId.class)
			.as("Kind parsed")
			.returns(ObjectDatumKind.Location, from(ObjectDatumId::getKind))
			.as("Stream ID parsed")
			.returns(id.getStreamId(), from(ObjectDatumId::getStreamId))
			.as("Object ID parsed")
			.returns(id.getObjectId(), from(ObjectDatumId::getObjectId))
			.as("Source ID parsed")
			.returns(id.getSourceId(), from(ObjectDatumId::getSourceId))
			.as("Timestamp parsed")
			.returns(id.getTimestamp(), from(ObjectDatumId::getTimestamp))
			.as("Aggregation parsed")
			.returns(id.getAggregation(), from(ObjectDatumId::getAggregation))
			;
		// @formatter:on
	}

	@Test
	public void fullySpeced_streamId() {
		// GIVEN
		var id = ObjectDatumId.nodeId(randomUUID(), null, null, clock.instant(), Aggregation.None);

		// THEN
		then(id.isFullySpecified()).as("ID with kind, stream ID, and timestamp is fully specified")
				.isTrue();
	}

	@Test
	public void fullySpeced_objSourceIds() {
		// GIVEN
		var id = ObjectDatumId.nodeId(null, randomLong(), randomString(), clock.instant(),
				Aggregation.None);

		// THEN
		then(id.isFullySpecified())
				.as("ID with kind, object ID, source ID, and timestamp is fully specified").isTrue();
	}

	@Test
	public void notFullySpeced_noIds() {
		// GIVEN
		var id = ObjectDatumId.nodeId(null, null, null, clock.instant(), Aggregation.None);

		// THEN
		then(id.isFullySpecified()).as("ID without any identifiers is not fully specified").isFalse();
	}

	@Test
	public void notFullySpeced_noObjectId() {
		// GIVEN
		var id = ObjectDatumId.nodeId(null, null, randomString(), clock.instant(), Aggregation.None);

		// THEN
		then(id.isFullySpecified()).as("ID with source ID but no object ID is not fully specified")
				.isFalse();
	}

	@Test
	public void notFullySpeced_noSourceId() {
		// GIVEN
		var id = ObjectDatumId.nodeId(null, randomLong(), null, clock.instant(), Aggregation.None);

		// THEN
		then(id.isFullySpecified()).as("ID with object ID but no source ID is not fully specified")
				.isFalse();
	}

	@Test
	public void equivalent_sameInstance() {
		// GIVEN
		var id = ObjectDatumId.nodeId(null, randomLong(), null, clock.instant(), Aggregation.None);

		// THEN
		then(id.isEquivalent(id)).as("Same instance is equivalent").isTrue();
	}

	@Test
	public void equivalent_sameStreamIds() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(randomUUID(), null, null, clock.instant(), Aggregation.None);
		var id2 = ObjectDatumId.nodeId(id1.getStreamId(), null, null, clock.instant(), Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2)).as("Same stream IDs are equivalent").isTrue();
	}

	@Test
	public void equivalent_sameStreamIds_ignoreObjectSource() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var id2 = ObjectDatumId.nodeId(id1.getStreamId(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2))
				.as("Same stream IDs are equivalent even when object/source IDs differ").isTrue();
		then(id1).as("Even though equivalent, not equal").isNotEqualTo(id2);
	}

	@Test
	public void equivalent_sameStreamIds_missingStreamIds() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(null, randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var id2 = ObjectDatumId.nodeId(null, id1.getObjectId(), id1.getSourceId(), clock.instant(),
				Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2))
				.as("Same stream IDs are equivalent by object/source IDs when stream IDs null").isTrue();
	}

	@Test
	public void equivalent_differentStreamIds() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(randomUUID(), randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var id2 = ObjectDatumId.nodeId(randomUUID(), id1.getObjectId(), id1.getSourceId(),
				clock.instant(), Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2))
				.as("Different stream IDs are not equivalent, even when object/source IDs are same")
				.isFalse();
	}

	@Test
	public void equivalent_differentObjectIds() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(null, randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var id2 = ObjectDatumId.nodeId(null, randomLong(), id1.getSourceId(), clock.instant(),
				Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2)).as("Different object IDs are not equivalent").isFalse();
	}

	@Test
	public void equivalent_differentSourceIds() {
		// GIVEN
		var id1 = ObjectDatumId.nodeId(null, randomLong(), randomString(), clock.instant(),
				Aggregation.None);
		var id2 = ObjectDatumId.nodeId(null, id1.getObjectId(), randomString(), clock.instant(),
				Aggregation.None);

		// THEN
		then(id1.isEquivalent(id2)).as("Different source IDs are not equivalent").isFalse();
	}

}
