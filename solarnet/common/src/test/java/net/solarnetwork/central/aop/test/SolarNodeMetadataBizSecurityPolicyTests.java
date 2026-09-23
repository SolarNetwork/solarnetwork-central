/* ==================================================================
 * SolarNodeMetadataBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.aop.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertNodeMetadata;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.aop.NodeMetadataSecurityAspect;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.dao.DaoSolarNodeMetadataBiz;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeMetadataDao;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Verify security policies are enforced on {@link SolarNodeMetadataBiz} with
 * the {@code NodeMetadataSecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 2.0
 */
@ExtendWith(SecurityContextExtension.class)
public class SolarNodeMetadataBizSecurityPolicyTests extends AbstractJUnit5JdbcDaoTestSupport {

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private SolarNodeMetadataBiz biz;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		for ( TestTenant t : List.of(a, b) ) {
			for ( Long nodeId : t.nodeIds() ) {
				insertNodeMetadata(jdbcTemplate, nodeId, metadata());
			}
		}
		final var dao = new JdbcSolarNodeMetadataDao(jdbcTemplate);
		biz = securedProxy((SolarNodeMetadataBiz) new DaoSolarNodeMetadataBiz(dao),
				new NodeMetadataSecurityAspect(new JdbcSolarNodeOwnershipDao(jdbcTemplate))).proxy();
	}

	private static GeneralDatumMetadata metadata() {
		final GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("a", 1);
		meta.putInfoValue("b", 2);
		return meta;
	}

	private static FilterSupport nodes(Long... nodeIds) {
		final FilterSupport filter = new FilterSupport();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static Set<Long> nodeIds(Iterable<SolarNodeMetadata> results) {
		return StreamSupport.stream(results.spliterator(), false).map(SolarNodeMetadata::getId)
				.collect(toSet());
	}

	@Test
	public void user_otherUserNodeRemoved() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findSolarNodeMetadata(nodes(a.privateNodeId(), b.privateNodeId()), null,
				null, null);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Other user's node removed from query")
			.containsExactly(a.privateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_policyNodeOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findSolarNodeMetadata(nodes(a.privateNodeId(), a.otherPrivateNodeId()),
				null, null, null);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only the policy node metadata returned")
			.containsExactly(a.otherPrivateNodeId())
			;
		// @formatter:on
	}

	@Test
	public void anonymous_publicNodeReadable() {
		// GIVEN
		tenants.anonymous().become();

		// WHEN
		var results = biz.findSolarNodeMetadata(nodes(a.publicNodeId(), a.privateNodeId()), null,
				null, null);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Only public node metadata returned")
			.containsExactly(a.publicNodeId())
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataPathsPolicy() {
		// GIVEN
		final TestToken token = a.newToken(SecurityTokenType.ReadNodeData,
				BasicSecurityPolicy.builder().withNodeMetadataPaths(Set.of("/m/a")).build());
		token.insert(jdbcTemplate);
		TestActor.token("A metadata token", token).become();

		// WHEN
		var results = biz.findSolarNodeMetadata(nodes(a.privateNodeId()), null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("Node metadata returned")
			.hasSize(1)
			.first()
			.extracting(m -> nonnull(m.getMetadata(), "Metadata").getInfo())
			.as("Only the policy metadata paths returned")
			.asInstanceOf(InstanceOfAssertFactories.MAP)
			.containsOnlyKeys("a")
			;
		// @formatter:on
	}

	@Test
	public void nodeMetadataPathsPolicy_noMatchingMetadataRemoved() {
		// GIVEN
		jdbcTemplate.update("""
				UPDATE solarnet.sn_node_meta SET jdata = '{"m":{"b":2}}'::jsonb
				WHERE node_id = ?""", a.otherPrivateNodeId());

		final TestToken token = a.newToken(SecurityTokenType.ReadNodeData,
				BasicSecurityPolicy.builder().withNodeMetadataPaths(Set.of("/m/a")).build());
		token.insert(jdbcTemplate);
		TestActor.token("A metadata token", token).become();

		// WHEN
		var results = biz.findSolarNodeMetadata(nodes(a.privateNodeId(), a.otherPrivateNodeId()), null,
				null, null);

		// THEN
		// @formatter:off
		then(nodeIds(results))
			.as("Node without any metadata allowed by the policy removed from results")
			.containsExactly(a.privateNodeId())
			;
		then(results.getReturnedResultCount())
			.as("Returned result count matches the restricted results")
			.isEqualTo(1)
			;
		// @formatter:on
	}
}
