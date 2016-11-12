/* ==================================================================
 * SecurityPolicyEnforcerTests.java - 11/10/2016 9:04:26 AM
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SolarNodeMetadataMatch;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.central.security.SecurityPolicyMetadataType;
import net.solarnetwork.central.support.NodeMetadataSerializer;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.util.ObjectMapperFactoryBean;

/**
 * Test cases for the {@link SecurityPolicyEnforcer} class.
 * 
 * @author matt
 * @version 1.2
 */
public class SecurityPolicyEnforcerTests {

	private static final String TEST_SOURCE_ID = "Main";
	private static final String TEST_SOURCE_ID2 = "Main2";
	private static final Long TEST_NODE_ID = 1L;
	private static final Long TEST_NODE_ID2 = 2L;

	@Test
	public void fillInPolicySourceId() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new HashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in source ID", TEST_SOURCE_ID, filter.getSourceId());
	}

	@Test
	public void fillInPolicySourceIdFromMulti() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in source ID", TEST_SOURCE_ID, filter.getSourceId());
	}

	@Test
	public void fillInPolicySourceIdsSingle() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new HashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Filled in source IDs", policySourceIds, filter.getSourceIds());
	}

	@Test
	public void fillInPolicySourceIdsMulti() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Filled in source IDs", policySourceIds, filter.getSourceIds());
	}

	@Test
	public void restrictToPolicySourceIdsSingle() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new HashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIds(new String[] { TEST_SOURCE_ID, "Other" });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Restricted source IDs", policySourceIds, filter.getSourceIds());
	}

	@Test
	public void restrictToPolicySourceIdsMulti() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIds(new String[] { TEST_SOURCE_ID, TEST_SOURCE_ID2, "Other", "Other2" });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Restricted source IDs", policySourceIds, filter.getSourceIds());
	}

	@Test(expected = AuthorizationException.class)
	public void denyFromPolicySourceIds() {
		String[] policySourceIds = new String[] { TEST_SOURCE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new HashSet<String>(Arrays.asList(policySourceIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setSourceIds(new String[] { "Other" });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		filter.getSourceIds();
	}

	private static final String TEST_SOURCE_PAT_ONELEVEL = "/Main/*";
	private static final String TEST_SOURCE_PAT_MULTILEVEL = "/Main2/**";
	private static final String TEST_SOURCE_PAT_MULTILEVEL_MIDDLE = "/Main2/**/Meter";
	private static final String TEST_SOURCE_ID_ONELEVEL = "/Main/1";
	private static final String TEST_SOURCE_ID_MULTILEVEL = "/Main2/One/Two";

	@Test
	public void verifySourceIdsWithPathMatcher() {
		String[] policySourceIds = new String[] { TEST_SOURCE_PAT_ONELEVEL, TEST_SOURCE_PAT_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher());
		String[] result = enforcer.verifySourceIds(new String[] { TEST_SOURCE_ID_ONELEVEL });
		Assert.assertArrayEquals("Verify source IDs", new String[] { TEST_SOURCE_ID_ONELEVEL }, result);
	}

	@Test
	public void verifySourceIdsWithPathMatcherRestricted() {
		String[] policySourceIds = new String[] { TEST_SOURCE_PAT_ONELEVEL, TEST_SOURCE_PAT_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher());
		String[] result = enforcer.verifySourceIds(
				new String[] { TEST_SOURCE_ID_ONELEVEL, TEST_SOURCE_ID_MULTILEVEL, "/some/other" });
		Assert.assertArrayEquals("Restricted source IDs",
				new String[] { TEST_SOURCE_ID_ONELEVEL, TEST_SOURCE_ID_MULTILEVEL }, result);
	}

	@Test(expected = AuthorizationException.class)
	public void verifySourceIdsWithPathMatcherDenied() {
		String[] policySourceIds = new String[] { TEST_SOURCE_PAT_ONELEVEL, TEST_SOURCE_PAT_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher());
		enforcer.verifySourceIds(new String[] { "/not/accepted", "/some/other" });
	}

	@Test
	public void verifySourceIdsWithPathMatcherMiddle() {
		String[] policySourceIds = new String[] { TEST_SOURCE_PAT_MULTILEVEL_MIDDLE };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher());
		String[] inputSourceIds = new String[] { "/Main2/foo/bar/bam/Meter", "/Main2/Meter" };
		String[] result = enforcer.verifySourceIds(inputSourceIds);
		Assert.assertArrayEquals("Restricted source IDs", inputSourceIds, result);
	}

	@Test
	public void verifySourceIdsWithPathMatcherMixedPatterns() {
		String[] policySourceIds = new String[] { "/A/BC/1", "/A/bc/*", "/A/bc/1/*" };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withSourceIds(new LinkedHashSet<String>(Arrays.asList(policySourceIds))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher());
		String[] inputSourceIds = new String[] { "/A/BC/1" };
		String[] result = enforcer.verifySourceIds(inputSourceIds);
		Assert.assertArrayEquals("Restricted source IDs", inputSourceIds, result);
	}

	@Test
	public void fillInPolicyNodeId() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in source ID", TEST_NODE_ID, filter.getNodeId());
	}

	@Test
	public void fillInPolicyNodeIdFromMulti() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID, TEST_NODE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in source ID", TEST_NODE_ID, filter.getNodeId());
	}

	@Test
	public void fillInPolicyNodeIdsSingle() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Filled in source IDs", policyNodeIds, filter.getNodeIds());
	}

	@Test
	public void fillInPolicyNodeIdsMulti() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID, TEST_NODE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Filled in source IDs", policyNodeIds, filter.getNodeIds());
	}

	@Test
	public void restrictToPolicyNodeIdsSingle() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, -1L });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Restricted source IDs", policyNodeIds, filter.getNodeIds());
	}

	@Test
	public void restrictToPolicyNodeIdsMulti() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID, TEST_NODE_ID2 };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new LinkedHashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID2, -1L, -2L });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		Assert.assertArrayEquals("Restricted source IDs", policyNodeIds, filter.getNodeIds());
	}

	@Test(expected = AuthorizationException.class)
	public void denyFromPolicyNodeIds() {
		Long[] policyNodeIds = new Long[] { TEST_NODE_ID };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(policyNodeIds))).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { -1L });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		filter.getNodeIds();
	}

	@Test
	public void fillInMinAggregation() {
		Aggregation min = Aggregation.Month;
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withMinAggregation(min).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		AggregateGeneralNodeDatumFilter filter = SecurityPolicyEnforcer
				.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in aggregation", min, filter.getAggregation());
	}

	@Test
	public void restrictToMinAggregation() {
		Aggregation min = Aggregation.Month;
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder().withMinAggregation(min).build();
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setAggregation(Aggregation.Day);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd);
		AggregateGeneralNodeDatumFilter filter = SecurityPolicyEnforcer
				.createSecurityPolicyProxy(enforcer);
		Assert.assertEquals("Filled in aggregation", min, filter.getAggregation());
	}

	private static final String TEST_META_PAT_ONELEVEL = "/m/*";
	private static final String TEST_META_PAT_MULTILEVEL = "/pm/**";
	private static final String TEST_META_PAT_MULTILEVEL_MIDDLE = "/pm/**/bar";
	private static final String TEST_META_ONELEVEL = "/m/1";
	private static final String TEST_META_MULTILEVEL = "/pm/one/two";

	@Test
	public void verifyMetadataPaths() {
		String[] policyMetadataPaths = new String[] { TEST_META_PAT_ONELEVEL, TEST_META_PAT_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(policyMetadataPaths)))
				.build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("1", "one");
		GeneralDatumMetadata result = enforcer.verifyMetadata(meta);
		Assert.assertSame("Verify metadata paths", meta, result);
	}

	@Test(expected = AuthorizationException.class)
	public void verifyMetadataPathsDenied() {
		String[] policyMetadataPaths = new String[] { TEST_META_ONELEVEL, TEST_META_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(policyMetadataPaths)))
				.build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("2", "two");
		enforcer.verifyMetadata(meta);
	}

	@Test
	public void verifyMetadataPathsMiddle() {
		String[] metaPaths = new String[] { TEST_META_PAT_MULTILEVEL_MIDDLE };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(metaPaths))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar", "bam");
		meta.putInfoValue("meter", "yes");
		GeneralDatumMetadata result = enforcer.verifyMetadata(meta);

		GeneralDatumMetadata expected = new GeneralDatumMetadata(null, meta.getPropertyInfo());

		Assert.assertEquals("Restricted metadata", expected, result);
	}

	@Test
	public void verifyMetadataPathsMixedPatterns() {
		String[] metaPaths = new String[] { "/pm/BC/1", "/pm/bc/*" };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(metaPaths))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("BC", "1", "one");
		meta.putInfoValue("BC", "2", "two");
		meta.putInfoValue("bc", "a", "A");
		meta.putInfoValue("bc", "b", "B");

		GeneralDatumMetadata result = enforcer.verifyMetadata(meta);

		GeneralDatumMetadata expected = new GeneralDatumMetadata();
		expected.putInfoValue("BC", "1", "one");
		expected.putInfoValue("bc", "a", "A");
		expected.putInfoValue("bc", "b", "B");

		Assert.assertEquals("Restricted metadata", expected, result);
	}

	@Test
	public void verifyMetadataPathsDeepObject() {
		String[] metaPaths = new String[] { TEST_META_PAT_MULTILEVEL_MIDDLE };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(metaPaths))).build();
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", null,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);

		Map<String, Object> nestedMap = Collections.singletonMap("bar", (Object) "yes");

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", "1", nestedMap);
		meta.putInfoValue("b", "2", nestedMap);
		meta.putInfoValue("c", "bar", "yes");
		meta.putInfoValue("d", "foo", "no");

		GeneralDatumMetadata result = enforcer.verifyMetadata(meta);

		GeneralDatumMetadata expected = new GeneralDatumMetadata();
		expected.putInfoValue("a", "1", nestedMap);
		expected.putInfoValue("b", "2", nestedMap);
		expected.putInfoValue("c", "bar", "yes");

		Assert.assertEquals("Restricted metadata", expected, result);
	}

	@Test(expected = AuthorizationException.class)
	public void denyFromPolicyNodeMetadataPaths() {
		String[] policyMetadataPaths = new String[] { TEST_META_ONELEVEL, TEST_META_MULTILEVEL };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(policyMetadataPaths)))
				.build();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("2", "two");

		SolarNodeMetadataMatch src = new SolarNodeMetadataMatch();
		src.setMeta(meta);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", src,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		SolarNodeMetadataFilterMatch match = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		match.getMetadata();
	}

	@Test
	public void restrictToPolicyNodeMetadataPaths() {
		String[] policyMetadataPaths = new String[] { TEST_META_PAT_MULTILEVEL_MIDDLE };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(policyMetadataPaths)))
				.build();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar", "bam");
		meta.putInfoValue("meter", "yes");

		GeneralDatumMetadata expected = new GeneralDatumMetadata(null, meta.getPropertyInfo());

		SolarNodeMetadataMatch src = new SolarNodeMetadataMatch();
		src.setMeta(meta);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", src,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		SolarNodeMetadataFilterMatch match = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);
		GeneralDatumMetadata result = match.getMetadata();
		Assert.assertEquals("Restricted metadata", expected, result);
	}

	@Test
	public void restrictToPolicyNodeMetadataPathsJSON() throws Exception {
		String[] policyMetadataPaths = new String[] { TEST_META_PAT_MULTILEVEL_MIDDLE };
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeMetadataPaths(new LinkedHashSet<String>(Arrays.asList(policyMetadataPaths)))
				.build();

		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar", "bam");
		meta.putInfoValue("meter", "yes");

		SolarNodeMetadataMatch src = new SolarNodeMetadataMatch();
		src.setNodeId(1L);
		src.setMeta(meta);
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", src,
				new AntPathMatcher(), SecurityPolicyMetadataType.Node);
		SolarNodeMetadataFilterMatch match = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);

		ObjectMapperFactoryBean factory = new ObjectMapperFactoryBean();
		List<JsonSerializer<?>> list = new ArrayList<JsonSerializer<?>>(1);
		list.add(new NodeMetadataSerializer());
		factory.setSerializers(list);
		ObjectMapper mapper = factory.getObject();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		String json = mapper.writeValueAsString(match);
		Assert.assertEquals("Restricted metadata JSON",
				"{\"nodeId\":1,\"pm\":{\"foo\":{\"bar\":\"bam\"}}}", json);
	}

}
