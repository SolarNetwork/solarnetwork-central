/* ==================================================================
 * BasicSecurityPolicyTests.java - 9/10/2016 8:49:36 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.fail;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link BasicSecurityPolicy} class.
 * 
 * @author matt
 * @version 2.0
 */
public class BasicSecurityPolicyTests {

	private static final Instant TEST_DATE = LocalDateTime.of(2018, 5, 30, 10, 30, 0)
			.toInstant(ZoneOffset.UTC);

	@Test
	public void buildNodeIdsPolicy() {
		Set<Long> nodeIds = new HashSet<Long>(Arrays.asList(1L, 2L, 3L));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(nodeIds).build();
		assertThat("Node ID set", policy.getNodeIds(), is(nodeIds));
		try {
			policy.getNodeIds().add(-1L);
			fail("Node ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedNodeIdsPolicy() {
		Set<Long> nodeIds = new HashSet<Long>(Arrays.asList(1L, 2L, 3L));
		Set<Long> additionalNodeIds = new HashSet<Long>(Arrays.asList(3L, 4L, 5L));
		Set<Long> merged = new HashSet<Long>(Arrays.asList(1L, 2L, 3L, 4L, 5L));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeIds(nodeIds)
				.withMergedNodeIds(additionalNodeIds).build();
		assertThat("Node ID set", policy.getNodeIds(), is(merged));
		try {
			policy.getNodeIds().add(-1L);
			fail("Node ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildSourceIdsPolicy() {
		Set<String> sourceIds = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withSourceIds(sourceIds).build();
		assertThat("Source ID set", policy.getSourceIds(), is(sourceIds));
		try {
			policy.getSourceIds().add("no");
			fail("Source ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedSourceIdsPolicy() {
		Set<String> sourceIds = new HashSet<String>(Arrays.asList("one", "two", "three"));
		Set<String> additionalSourceIds = new HashSet<String>(Arrays.asList("three", "four", "five"));
		Set<String> merged = new HashSet<String>(Arrays.asList("one", "two", "three", "four", "five"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withSourceIds(sourceIds)
				.withMergedSourceIds(additionalSourceIds).build();
		assertThat("Source ID set", policy.getSourceIds(), is(merged));
		try {
			policy.getSourceIds().add("no");
			fail("Source ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildNodeMetadataPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeMetadataPaths(paths).build();
		assertThat("Node metadata path set", policy.getNodeMetadataPaths(), is(paths));
		try {
			policy.getNodeMetadataPaths().add("no");
			fail("Node metadata path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedNodeMetadataPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		Set<String> additionalPaths = new HashSet<String>(Arrays.asList("three", "four", "five"));
		Set<String> merged = new HashSet<String>(Arrays.asList("one", "two", "three", "four", "five"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNodeMetadataPaths(paths)
				.withMergedNodeMetadataPaths(additionalPaths).build();
		assertThat("Node metadata path set", policy.getNodeMetadataPaths(), is(merged));
		try {
			policy.getNodeMetadataPaths().add("no");
			fail("Node metadata path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildUserMetadataPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withUserMetadataPaths(paths).build();
		Assert.assertEquals("User metadata path set", paths, policy.getUserMetadataPaths());
		try {
			policy.getUserMetadataPaths().add("no");
			fail("User metadata path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedUserMetadataPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		Set<String> additionalPaths = new HashSet<String>(Arrays.asList("three", "four", "five"));
		Set<String> merged = new HashSet<String>(Arrays.asList("one", "two", "three", "four", "five"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withUserMetadataPaths(paths)
				.withMergedUserMetadataPaths(additionalPaths).build();
		assertThat("User metadata path set", policy.getUserMetadataPaths(), is(merged));
		try {
			policy.getUserMetadataPaths().add("no");
			Assert.fail("User metadata path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildApiPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withApiPaths(paths).build();
		Assert.assertEquals("Api path set", paths, policy.getApiPaths());
		try {
			policy.getApiPaths().add("no");
			fail("Api path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedApiPathsPolicy() {
		Set<String> paths = new HashSet<String>(Arrays.asList("one", "two", "three"));
		Set<String> additionalPaths = new HashSet<String>(Arrays.asList("three", "four", "five"));
		Set<String> merged = new HashSet<String>(Arrays.asList("one", "two", "three", "four", "five"));
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withApiPaths(paths)
				.withMergedApiPaths(additionalPaths).build();
		assertThat("Api path set", policy.getApiPaths(), is(merged));
		try {
			policy.getApiPaths().add("no");
			Assert.fail("Api path set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinAggregationPolicy() {
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Month)
				.build();
		assertThat("Minimum aggregation set", policy.getAggregations(),
				contains(Aggregation.Month, Aggregation.Year, Aggregation.RunningTotal));
		try {
			policy.getAggregations().add(Aggregation.Minute);
			fail("Aggregation set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMergedMinAggregationPolicy() {
		BasicSecurityPolicy orig = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Month)
				.build();
		BasicSecurityPolicy patch = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Day)
				.build();
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withPolicy(orig)
				.withMergedPolicy(patch).build();
		assertThat("Minimum aggregation", policy.getMinAggregation(), is(Aggregation.Day));
		assertThat("Minimum aggregation set", policy.getAggregations(),
				contains(Aggregation.Day, Aggregation.DayOfWeek, Aggregation.SeasonalDayOfWeek,
						Aggregation.Week, Aggregation.WeekOfYear, Aggregation.Month, Aggregation.Year,
						Aggregation.RunningTotal));
		try {
			policy.getAggregations().add(Aggregation.Minute);
			fail("Aggregation set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinAggregationPolicyWithCache() {
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Month)
				.build();
		BasicSecurityPolicy policy2 = BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Month)
				.build();
		assertThat("Cached minimum aggregation set", policy2.getAggregations(),
				is(sameInstance(policy.getAggregations())));
	}

	@Test
	public void buildMinLocationPrecisionPolicy() {
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		Assert.assertEquals("Minimum location precision set",
				EnumSet.of(LocationPrecision.PostalCode, LocationPrecision.Locality,
						LocationPrecision.Region, LocationPrecision.StateOrProvince,
						LocationPrecision.TimeZone, LocationPrecision.Country),
				policy.getLocationPrecisions());
		try {
			policy.getLocationPrecisions().add(LocationPrecision.LatLong);
			fail("LocationPrecision set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinLocationPrecisionPolicyWithCache() {
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		BasicSecurityPolicy policy2 = BasicSecurityPolicy.builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		assertThat("Cached minimum location precision set", policy2.getLocationPrecisions(),
				is(sameInstance(policy.getLocationPrecisions())));
	}

	@Test
	public void buildExpiringPolicy() {
		BasicSecurityPolicy policy = BasicSecurityPolicy.builder().withNotAfter(TEST_DATE)
				.withRefreshAllowed(true).build();
		assertThat("Not after set", TEST_DATE, equalTo(policy.getNotAfter()));
		assertThat("Refresh allowed set", policy.getRefreshAllowed(), equalTo(true));
		assertThat("Valid before", policy.isValidAt(TEST_DATE.plusMillis(-1)), equalTo(true));
		assertThat("Valid at", policy.isValidAt(TEST_DATE), equalTo(true));
		assertThat("Not valid after", policy.isValidAt(TEST_DATE.plusMillis(1)), equalTo(false));
	}

	@Test
	public void parseJsonNodeIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"nodeIds\":[1,2,3]}",
				BasicSecurityPolicy.class);
		assertThat("Node ID set", policy.getNodeIds(), contains(1L, 2L, 3L));
	}

	@Test
	public void parseJsonEmptyNodeIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"nodeIds\":[]}", BasicSecurityPolicy.class);
		assertThat("Node ID set null instead of empty set", policy.getNodeIds(), is(nullValue()));
	}

	@Test
	public void parseJsonSourceIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"sourceIds\":[\"one\",\"two\",\"three\"]}",
				BasicSecurityPolicy.class);
		assertThat("Source ID set", policy.getSourceIds(), contains("one", "two", "three"));
	}

	@Test
	public void parseJsonMinAggregationPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"minAggregation\":\"Month\"}",
				BasicSecurityPolicy.class);
		assertThat("Minimum aggregation set", policy.getAggregations(),
				containsInAnyOrder(Aggregation.Month, Aggregation.Year, Aggregation.RunningTotal));
	}

	@Test
	public void parseJsonAggregationPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"aggregations\":[\"Month\"]}",
				BasicSecurityPolicy.class);
		assertThat("Exact aggregation set", policy.getAggregations(), contains(Aggregation.Month));
	}

	@Test
	public void parseJsonMinLocationPrecisionPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"minLocationPrecision\":\"TimeZone\"}",
				BasicSecurityPolicy.class);
		assertThat("Minimum location precision set", policy.getLocationPrecisions(),
				containsInAnyOrder(LocationPrecision.TimeZone, LocationPrecision.Country));
	}

	@Test
	public void parseJsonNotAfterPolicy() throws Exception {
		ObjectMapper mapper = JsonUtils.newObjectMapper();
		mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
		BasicSecurityPolicy policy = mapper.readValue("{\"notAfter\":" + TEST_DATE.toEpochMilli() + "}",
				BasicSecurityPolicy.class);
		assertThat("Not after set", policy.getNotAfter(), is(equalTo(TEST_DATE)));
	}

	@Test
	public void parseJsonRefreshAllowedPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"refreshAllowed\":true}",
				BasicSecurityPolicy.class);
		assertThat("Refresh allowed set", policy.getRefreshAllowed(), equalTo(true));
	}

	@Test
	public void parseJsonLocationPrecisionPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"locationPrecisions\":[\"TimeZone\"]}",
				BasicSecurityPolicy.class);
		assertThat("Exact location precision set", policy.getLocationPrecisions(),
				contains(LocationPrecision.TimeZone));
	}

}
