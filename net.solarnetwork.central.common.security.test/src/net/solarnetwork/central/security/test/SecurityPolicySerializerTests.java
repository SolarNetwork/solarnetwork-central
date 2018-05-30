/* ==================================================================
 * SecurityPolicySerializerTests.java - 9/10/2016 1:41:02 PM
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.LocationPrecision;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicySerializer;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Test cases for the {@link SecurityPolicySerializer} class.
 * 
 * @author matt
 * @version 1.2
 */
public class SecurityPolicySerializerTests {

	private static final DateTime TEST_DATE = new DateTime(2018, 5, 30, 10, 30, DateTimeZone.UTC);

	private ObjectMapper objectMapper;

	@Before
	public void setup() throws Exception {
		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		List<JsonSerializer<?>> list = new ArrayList<JsonSerializer<?>>(1);
		list.add(new SecurityPolicySerializer());
		factory.setSerializers(list);
		objectMapper = factory.getObject();
	}

	@Test
	public void serializeNodeIdsPolicy() throws JsonProcessingException {
		Set<Long> nodeIds = new HashSet<Long>(Arrays.asList(1L, 2L, 3L));
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"nodeIds\":[1,2,3]}", json);
	}

	@Test
	public void serializeNodeIdsPolicyOrdered() throws JsonProcessingException {
		Set<Long> nodeIds = new HashSet<Long>(Arrays.asList(3L, 2L, 1L));
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withNodeIds(nodeIds).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"nodeIds\":[1,2,3]}", json);
	}

	@Test
	public void serializeSourceIdsPolicy() throws JsonProcessingException {
		Set<String> sourceIds = new HashSet<String>(Arrays.asList("one", "two", "three"));
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withSourceIds(sourceIds).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"sourceIds\":[\"one\",\"two\",\"three\"]}", json);
	}

	@Test
	public void serializeMinAggregationPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinAggregation(Aggregation.Month).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"minAggregation\":\"Month\"}", json);
	}

	@Test
	public void serializeAggregationPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withAggregations(EnumSet.of(Aggregation.Month, Aggregation.Day)).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"aggregations\":[\"Day\",\"Month\"]}", json);
	}

	@Test
	public void serializeMinLocationPrecisionPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"minLocationPrecision\":\"PostalCode\"}", json);
	}

	@Test
	public void serializeLocationPrecisionPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withLocationPrecisions(
				EnumSet.of(LocationPrecision.PostalCode, LocationPrecision.Block)).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"locationPrecisions\":[\"Block\",\"PostalCode\"]}", json);
	}

	@Test
	public void serializeNodeMetadataPathsPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList("1", "2", "3"))).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"nodeMetadataPaths\":[\"1\",\"2\",\"3\"]}", json);
	}

	@Test
	public void serializeUserMetadataPathsPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withUserMetadataPaths(new LinkedHashSet<String>(Arrays.asList("1", "2", "3"))).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON", "{\"userMetadataPaths\":[\"1\",\"2\",\"3\"]}", json);
	}

	@Test
	public void serializeComplex() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withMinLocationPrecision(LocationPrecision.PostalCode)
				.withMinAggregation(Aggregation.Day)
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList("three", "two", "one")))
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(1L, 2L, 3L)))
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList("1")))
				.withUserMetadataPaths(new LinkedHashSet<String>(Arrays.asList("2", "3"))).build();
		String json = objectMapper.writeValueAsString(policy);
		Assert.assertEquals("JSON",
				"{\"nodeIds\":[1,2,3],\"sourceIds\":[\"three\",\"two\",\"one\"],\"minAggregation\":\"Day\",\"minLocationPrecision\":\"PostalCode\""
						+ ",\"nodeMetadataPaths\":[\"1\"],\"userMetadataPaths\":[\"2\",\"3\"]}",
				json);
	}

	@Test
	public void serializeExpiringPolicy() throws JsonProcessingException {
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withNotAfter(TEST_DATE)
				.withRefreshAllowed(true).build();
		String json = objectMapper.writeValueAsString(policy);
		assertThat("JSON", json,
				equalTo("{\"notAfter\":" + TEST_DATE.getMillis() + ",\"refreshAllowed\":true}"));
	}

}
