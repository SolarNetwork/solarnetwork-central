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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;
import net.solarnetwork.central.security.BasicSecurityPolicy;

/**
 * Test cases for the {@link BasicSecurityPolicy} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicSecurityPolicyTests {

	@Test
	public void buildNodeIdsPolicy() {
		Set<Long> nodeIds = new HashSet<Long>(Arrays.asList(1L, 2L, 3L));
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).build();
		Assert.assertEquals("Node ID set", nodeIds, policy.getNodeIds());
		try {
			policy.getNodeIds().add(-1L);
			Assert.fail("Node ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildSourceIdsPolicy() {
		Set<String> sourceIds = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withSourceIds(sourceIds).build();
		Assert.assertEquals("Source ID set", sourceIds, policy.getSourceIds());
		try {
			policy.getSourceIds().add("no");
			Assert.fail("Source ID set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinAggregationPolicy() {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		Assert.assertEquals("Minimum aggregation set",
				EnumSet.of(Aggregation.Month, Aggregation.RunningTotal), policy.getAggregations());
		try {
			policy.getAggregations().add(Aggregation.Minute);
			Assert.fail("Aggregation set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinAggregationPolicyWithCache() {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		BasicSecurityPolicy policy2 = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		Assert.assertSame("Cached minimum aggregation set", policy.getAggregations(),
				policy2.getAggregations());
	}

	@Test
	public void buildMinLocationPrecisionPolicy() {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		Assert.assertEquals("Minimum location precision set",
				EnumSet.of(LocationPrecision.PostalCode, LocationPrecision.Locality,
						LocationPrecision.Region, LocationPrecision.StateOrProvince,
						LocationPrecision.TimeZone, LocationPrecision.Country),
				policy.getLocationPrecisions());
		try {
			policy.getLocationPrecisions().add(LocationPrecision.LatLong);
			Assert.fail("LocationPrecision set should be immutable");
		} catch ( UnsupportedOperationException e ) {
			// expected
		}
	}

	@Test
	public void buildMinLocationPrecisionPolicyWithCache() {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		BasicSecurityPolicy policy2 = new BasicSecurityPolicy.Builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		Assert.assertSame("Cached minimum location precision set", policy.getLocationPrecisions(),
				policy2.getLocationPrecisions());
	}

	@Test
	public void parseJsonNodeIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"nodeIds\":[1,2,3]}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Node ID set", new HashSet<Long>(Arrays.asList(1L, 2L, 3L)),
				policy.getNodeIds());
	}

	@Test
	public void parseJsonEmptyNodeIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"nodeIds\":[]}", BasicSecurityPolicy.class);
		Assert.assertNull("Node ID set", policy.getNodeIds());
	}

	@Test
	public void parseJsonSourceIdPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"sourceIds\":[\"one\",\"two\",\"three\"]}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Source ID set", new HashSet<String>(Arrays.asList("one", "two", "three")),
				policy.getSourceIds());
	}

	@Test
	public void parseJsonMinAggregationPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"minAggregation\":\"Month\"}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Minimum aggregation set",
				EnumSet.of(Aggregation.Month, Aggregation.RunningTotal), policy.getAggregations());
	}

	@Test
	public void parseJsonAggregationPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"aggregations\":[\"Month\"]}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Exact aggregation set", EnumSet.of(Aggregation.Month),
				policy.getAggregations());
	}

	@Test
	public void parseJsonMinLocationPrecisionPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"minLocationPrecision\":\"TimeZone\"}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Minimum location precision set",
				EnumSet.of(LocationPrecision.TimeZone, LocationPrecision.Country),
				policy.getLocationPrecisions());
	}

	@Test
	public void parseJsonLocationPrecisionPolicy() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		BasicSecurityPolicy policy = mapper.readValue("{\"locationPrecisions\":[\"TimeZone\"]}",
				BasicSecurityPolicy.class);
		Assert.assertEquals("Exact location precision set", EnumSet.of(LocationPrecision.TimeZone),
				policy.getLocationPrecisions());
	}

}
