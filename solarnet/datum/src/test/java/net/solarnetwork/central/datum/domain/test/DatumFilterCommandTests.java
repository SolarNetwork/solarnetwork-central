/* ==================================================================
 * DatumFilterCommandTests.java - 21/03/2018 11:54:59 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.CombiningType;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link DatumFilterCommand} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumFilterCommandTests {

	private static final LocalDateTime TEST_START_DATE = LocalDateTime.of(2017, 3, 21, 12, 0, 0);
	private static final String TEST_START_DATE_STRING = "2017-03-21 12:00:00";
	private static final Instant TEST_START_TIMESTAMP = TEST_START_DATE.toInstant(ZoneOffset.UTC);
	private static final String TEST_START_TIMESTAMP_STRING = TEST_START_DATE_STRING + "Z";
	private static final LocalDateTime TEST_END_DATE = LocalDateTime.of(2017, 3, 28, 12, 0, 0);
	private static final Instant TEST_END_TIMESTAMP = TEST_END_DATE.toInstant(ZoneOffset.UTC);
	private static final String TEST_END_DATE_STRING = "2017-03-28 12:00:00";
	private static final String TEST_END_TIMESTAMP_STRING = TEST_END_DATE_STRING + "Z";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = JsonUtils.newDatumObjectMapper();
	}

	@Test
	public void serializeJson() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setAggregate(Aggregation.Day);
		cmd.setNodeId(1L);
		cmd.setSourceId("test");
		cmd.setStartDate(TEST_START_TIMESTAMP);
		cmd.setEndDate(TEST_END_TIMESTAMP);

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		assertThat(json, equalTo(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"aggregationKey\":\"d\""
						+ ",\"partialAggregationKey\":\"0\"" + ",\"mostRecent\":false,\"startDate\":\""
						+ TEST_START_TIMESTAMP_STRING + "\",\"endDate\":\"" + TEST_END_TIMESTAMP_STRING
						+ "\",\"location\":{},\"withoutTotalResultsCount\":false}"));
	}

	@Test
	public void serializeJsonWithPartialAggregation() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setAggregate(Aggregation.Day);
		cmd.setPartialAggregation(Aggregation.Hour);
		cmd.setNodeId(1L);
		cmd.setSourceId("test");
		cmd.setStartDate(LocalDateTime.of(2017, 3, 21, 12, 0, 0).toInstant(ZoneOffset.UTC));
		cmd.setEndDate(LocalDateTime.of(2017, 3, 28, 12, 0, 0).toInstant(ZoneOffset.UTC));

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		assertThat(json, equalTo(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"aggregationKey\":\"d\""
						+ ",\"partialAggregation\":\"Hour\",\"partialAggregationKey\":\"h\""
						+ ",\"mostRecent\":false,\"startDate\":\"" + TEST_START_TIMESTAMP_STRING
						+ "\",\"endDate\":\"" + TEST_END_TIMESTAMP_STRING
						+ "\",\"location\":{},\"withoutTotalResultsCount\":false}"));
	}

	@Test
	public void serializeJsonWithLocalDates() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setAggregate(Aggregation.Day);
		cmd.setNodeId(1L);
		cmd.setSourceId("test");
		cmd.setLocalStartDate(LocalDateTime.of(2017, 3, 21, 12, 0, 0));
		cmd.setLocalEndDate(LocalDateTime.of(2017, 3, 28, 12, 0, 0));

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		assertThat(json, equalTo(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"aggregationKey\":\"d\""
						+ ",\"partialAggregationKey\":\"0\""
						+ ",\"mostRecent\":false,\"localStartDate\":\"" + TEST_START_DATE_STRING
						+ "\",\"localEndDate\":\"" + TEST_END_DATE_STRING
						+ "\",\"location\":{},\"withoutTotalResultsCount\":false}"));
	}

	@Test
	public void serializeJsonWithCombining() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setCombiningType(CombiningType.Sum);
		cmd.setNodeIdMaps(new String[] { "100:1,2,3" });
		cmd.setSourceIdMaps(new String[] { "CON:A,B,C", "GEN:E,F,G" });

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		// @formatter:off
		assertThat(json, equalTo("{\"aggregationKey\":\"0\""
				+ ",\"partialAggregationKey\":\"0\""
				+ ",\"combiningType\":\"Sum\",\"combiningTypeKey\":\"s\""
				+ ",\"nodeIdMappings\":{\"100\":[1,2,3]}"
				+ ",\"sourceIdMappings\":{\"CON\":[\"A\",\"B\",\"C\"],\"GEN\":[\"E\",\"F\",\"G\"]}"
				+ ",\"mostRecent\":false,\"location\":{},\"withoutTotalResultsCount\":false}"));
		// @formatter:on
	}

	@Test
	public void serializeJsonWithRollupTypes() throws IOException {
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setDatumRollupTypes(new DatumRollupType[] { DatumRollupType.Time, DatumRollupType.Node });

		String json = objectMapper.writeValueAsString(cmd);
		assertThat(json, notNullValue());
		// @formatter:off
		assertThat(json, equalTo("{\"aggregationKey\":\"0\""
				+ ",\"partialAggregationKey\":\"0\""
				+ ",\"rollupTypes\":[\"Time\",\"Node\"]"
				+ ",\"rollupTypeKeys\":[\"t\",\"n\"]"
				+ ",\"mostRecent\":false,\"location\":{},\"withoutTotalResultsCount\":false}"));
		// @formatter:on
	}

	@Test
	public void deserializeJson() throws IOException {
		DatumFilterCommand cmd = objectMapper.readValue(
				"{\"nodeIds\":[1],\"sourceIds\":[\"test\"],\"aggregation\":\"Day\",\"mostRecent\":false,\"startDate\":\""
						+ TEST_START_TIMESTAMP_STRING + "\",\"endDate\":\"" + TEST_END_TIMESTAMP_STRING
						+ "\",\"location\":{}}",
				DatumFilterCommand.class);
		assertThat(cmd, notNullValue());
		assertThat(cmd.getAggregation(), equalTo(Aggregation.Day));
		assertThat(cmd.getNodeId(), equalTo(1L));
		assertThat(cmd.getSourceId(), equalTo("test"));
		assertThat(cmd.getStartDate(),
				equalTo(LocalDateTime.of(2017, 3, 21, 12, 0, 0).toInstant(ZoneOffset.UTC)));
		assertThat(cmd.getEndDate(),
				equalTo(LocalDateTime.of(2017, 3, 28, 12, 0, 0).toInstant(ZoneOffset.UTC)));
	}

	@Test
	public void deserializeJsonWithCombining() throws IOException {
		DatumFilterCommand cmd = objectMapper.readValue("{\"combiningTypeKey\":\"s\""
				+ ",\"nodeIdMappings\":{\"100\":[1,2,3]}"
				+ ",\"sourceIdMappings\":{\"CON\":[\"A\",\"B\",\"C\"],\"GEN\":[\"E\",\"F\",\"G\"]}"
				+ ",\"mostRecent\":false,\"location\":{},\"withoutTotalResultsCount\":false}",
				DatumFilterCommand.class);
		assertThat(cmd, notNullValue());
		assertThat(cmd.getCombiningType(), equalTo(CombiningType.Sum));

		assertThat(cmd.getNodeIdMappings().keySet(), hasSize(1));
		assertThat(cmd.getNodeIdMappings(),
				hasEntry(100L, (Set<Long>) new LinkedHashSet<Long>(Arrays.asList(1L, 2L, 3L))));

		assertThat(cmd.getSourceIdMappings().keySet(), hasSize(2));
		assertThat(cmd.getSourceIdMappings(),
				hasEntry("CON", (Set<String>) new LinkedHashSet<String>(Arrays.asList("A", "B", "C"))));
		assertThat(cmd.getSourceIdMappings(),
				hasEntry("GEN", (Set<String>) new LinkedHashSet<String>(Arrays.asList("E", "F", "G"))));
	}

	@Test
	public void setSourceMaps() {
		String[] sourceMaps = new String[] { "GEN:A,B,C", "CON:D" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIdMaps(sourceMaps);

		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("GEN", new LinkedHashSet<String>(Arrays.asList("A", "B", "C")));
		expected.put("CON", new LinkedHashSet<String>(Arrays.asList("D")));
		assertThat("Source ID mapping", cmd.getSourceIdMappings(), equalTo(expected));
	}

	@Test
	public void setSourceMapAllErrorsToNull() {
		String[] sourceMaps = new String[] { "GEN=1,2,3", "CON=D" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIdMaps(sourceMaps);

		assertThat("Source ID mapping", cmd.getSourceIdMappings(), nullValue());
	}

	@Test
	public void setSourceMapsNoColon() {
		String[] sourceMaps = new String[] { "GEN=1,2,3", "CON:D" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIdMaps(sourceMaps);

		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("CON", new LinkedHashSet<String>(Arrays.asList("D")));
		assertThat("Source ID mapping", cmd.getSourceIdMappings(), equalTo(expected));
	}

	@Test
	public void setSourceMapsNoReal() {
		String[] sourceMaps = new String[] { "GEN:", "CON:D" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIdMaps(sourceMaps);

		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("CON", new LinkedHashSet<String>(Arrays.asList("D")));
		assertThat("Source ID mapping", cmd.getSourceIdMappings(), equalTo(expected));
	}

	@Test
	public void setSourceMapsSpringSpecialCase() {
		String[] sourceMaps = new String[] { "GEN:A", "B", "C" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIdMaps(sourceMaps);

		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("GEN", new LinkedHashSet<String>(Arrays.asList("A", "B", "C")));
		assertThat("Source ID mapping", cmd.getSourceIdMappings(), equalTo(expected));
	}

	@Test
	public void setNodeMaps() {
		String[] nodeMaps = new String[] { "100:1,2,3", "200:4" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIdMaps(nodeMaps);

		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(100L, new LinkedHashSet<Long>(Arrays.asList(1L, 2L, 3L)));
		expected.put(200L, new LinkedHashSet<Long>(Arrays.asList(4L)));
		assertThat("Node ID mapping", cmd.getNodeIdMappings(), equalTo(expected));
	}

	@Test
	public void setNodeMapAllErrorsToNull() {
		String[] nodeMaps = new String[] { "100=1,2,3", "200=4" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIdMaps(nodeMaps);

		assertThat("Node ID mapping", cmd.getNodeIdMappings(), nullValue());
	}

	@Test
	public void setNodeMapsNoColon() {
		String[] nodeMaps = new String[] { "100=1,2,3", "200:4" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIdMaps(nodeMaps);

		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(200L, new LinkedHashSet<Long>(Arrays.asList(4L)));
		assertThat("Node ID mapping", cmd.getNodeIdMappings(), equalTo(expected));
	}

	@Test
	public void setNodeMapsNoReal() {
		String[] nodeMaps = new String[] { "100:", "200:4" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIdMaps(nodeMaps);

		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(200L, new LinkedHashSet<Long>(Arrays.asList(4L)));
		assertThat("Node ID mapping", cmd.getNodeIdMappings(), equalTo(expected));
	}

	@Test
	public void setNodeMapsSpringSpecialCase() {
		String[] nodeMaps = new String[] { "100:1", "2", "3" };
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIdMaps(nodeMaps);

		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(100L, new LinkedHashSet<Long>(Arrays.asList(1L, 2L, 3L)));
		assertThat("Node ID mapping", cmd.getNodeIdMappings(), equalTo(expected));
	}

}
