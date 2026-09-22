/* ==================================================================
 * QueryBizSecurityPolicyTests.java - 22/09/2026 5:30:12 pm
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

package net.solarnetwork.central.query.aop.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures.DEFAULT_START;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.support.BasicStreamDatumFilteredResultsProcessor;
import net.solarnetwork.central.query.aop.QuerySecurityAspect;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.test.tenant.TestToken;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Verify security policies are enforced on {@link QueryBiz} with the
 * {@code QuerySecurityAspect} aspect applied, using the database.
 *
 * <p>
 * Every tenant has datum for all of its node streams, so these tests also
 * verify that tenant B's datum never appears in tenant A's results. The
 * application's data source settings configure the test data source.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
@TestPropertySource(properties = { "spring.datasource.url=${app.datasource.url}",
		"spring.datasource.username=${app.datasource.username}",
		"spring.datasource.password=${app.datasource.password}" })
public class QueryBizSecurityPolicyTests extends AbstractJUnit5JdbcDaoTestSupport {

	private static final int DATUM_COUNT = 3;
	private static final int HOUR_COUNT = 2;

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private QueryBiz biz;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
		for ( TestTenant t : List.of(a, b) ) {
			TenantDatumFixtures.insertDatum(jdbcTemplate, t, DEFAULT_START, Duration.ofMinutes(5),
					DATUM_COUNT);
			TenantDatumFixtures.insertAggregateDatum(jdbcTemplate, t, Aggregation.Hour, DEFAULT_START,
					HOUR_COUNT);
		}

		final JdbcSolarNodeOwnershipDao ownershipDao = new JdbcSolarNodeOwnershipDao(jdbcTemplate);
		final JdbcDatumEntityDao datumDao = new JdbcDatumEntityDao(jdbcTemplate);
		biz = securedProxy((QueryBiz) new DaoQueryBiz(datumDao, datumDao, datumDao, ownershipDao),
				new QuerySecurityAspect(ownershipDao)).proxy();
	}

	private static DatumFilterCommand nodeFilter(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static Set<NodeSourcePK> nodeSources(
			Iterable<? extends GeneralNodeDatumFilterMatch> results) {
		return StreamSupport.stream(results.spliterator(), false)
				.map(m -> new NodeSourcePK(m.getId().getNodeId(), m.getId().getSourceId()))
				.collect(toSet());
	}

	private static Set<NodeSourcePK> nodeSources(Long nodeId, String... sourceIds) {
		return Arrays.stream(sourceIds).map(s -> new NodeSourcePK(nodeId, s)).collect(toSet());
	}

	private TestActor tokenActor(TestTenant tenant, SecurityTokenType type, SecurityPolicy policy) {
		final TestToken token = tenant.newToken(type, policy);
		token.insert(jdbcTemplate);
		return TestActor.token(tenant.name() + " policy token", token);
	}

	private static SecurityPolicy policy(String json) {
		// parse like token policies are parsed when authenticating
		return nonnull(JsonUtils.getObjectFromJSON(json, SecurityPolicy.class), "Policy");
	}

	private String[] sources(TestTenant tenant) {
		return tenant.sourceIds().toArray(String[]::new);
	}

	@Test
	public void user_allNodeSources() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findFilteredGeneralNodeDatum(
				nodeFilter(a.privateNodeId(), a.otherPrivateNodeId()), null, null, null);

		// THEN
		final Set<NodeSourcePK> expected = nodeSources(a.privateNodeId(), sources(a));
		expected.addAll(nodeSources(a.otherPrivateNodeId(), sources(a)));
		// @formatter:off
		then(nodeSources(results))
			.as("All sources of all requested nodes returned")
			.containsExactlyInAnyOrderElementsOf(expected)
			;
		then(results)
			.as("Raw datum returned for every stream")
			.hasSize(expected.size() * DATUM_COUNT)
			;
		// @formatter:on
	}

	@Test
	public void user_otherUserNodeRemoved() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findFilteredGeneralNodeDatum(
				nodeFilter(a.privateNodeId(), b.privateNodeId()), null, null, null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Other user's node removed from query")
			.containsExactlyInAnyOrderElementsOf(nodeSources(a.privateNodeId(), sources(a)))
			;
		// @formatter:on
	}

	@Test
	public void user_otherUserNodeOnly_denied() {
		// GIVEN
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Query for only another user's private node denied")
			.isThrownBy(() -> biz.findFilteredGeneralNodeDatum(nodeFilter(b.privateNodeId()), null,
					null, null))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_nodesAndSourcesNarrowedToPolicy() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findFilteredGeneralNodeDatum(
				nodeFilter(a.privateNodeId(), a.otherPrivateNodeId(), b.privateNodeId()), null, null,
				null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Only the policy node and source returned")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_requestedSourcesNarrowedToPolicy() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		final DatumFilterCommand filter = nodeFilter(a.otherPrivateNodeId());
		filter.setSourceIds(sources(a));
		var results = biz.findFilteredGeneralNodeDatum(filter, null, null, null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Requested sources narrowed to the policy source")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_nonPolicyNodeOnly_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Query for only a node outside the policy denied")
			.isThrownBy(() -> biz.findFilteredGeneralNodeDatum(nodeFilter(a.privateNodeId()), null,
					null, null))
			;
		// @formatter:on
	}

	@Test
	public void sourcePatternPolicy() {
		// GIVEN
		final String pattern = "/%s/pwr/*".formatted(a.name());
		tokenActor(a, SecurityTokenType.ReadNodeData,
				BasicSecurityPolicy.builder().withSourceIds(Set.of(pattern)).build()).become();

		// WHEN
		var results = biz.findFilteredGeneralNodeDatum(nodeFilter(a.privateNodeId()), null, null,
				null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Only sources matching the policy pattern returned")
			.isNotEmpty()
			.allMatch(pk -> pk.getSourceId().startsWith("/%s/pwr/".formatted(a.name())))
			.containsExactlyInAnyOrderElementsOf(nodeSources(a.privateNodeId(),
					a.sourceIds().stream().filter(s -> s.contains("/pwr/")).toArray(String[]::new)))
			;
		// @formatter:on
	}

	@Test
	public void minAggregationPolicy_rawQueryAggregated() {
		// GIVEN
		tokenActor(a, SecurityTokenType.ReadNodeData,
				BasicSecurityPolicy.builder().withMinAggregation(Aggregation.Hour).build()).become();

		// WHEN
		final DatumFilterCommand filter = nodeFilter(a.privateNodeId());
		filter.setStartDate(DEFAULT_START.toInstant());
		filter.setEndDate(DEFAULT_START.plusHours(HOUR_COUNT).toInstant());
		var results = biz.findFilteredGeneralNodeDatum(filter, null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("Raw query returns hourly aggregate datum for every stream")
			.hasSize(a.sourceIds().size() * HOUR_COUNT)
			.allMatch(ReportingGeneralNodeDatumMatch.class::isInstance)
			;
		then(nodeSources(results))
			.as("All sources of the node returned")
			.containsExactlyInAnyOrderElementsOf(nodeSources(a.privateNodeId(), sources(a)))
			;
		// @formatter:on
	}

	@Test
	public void aggregationsPolicy_otherAggregationDenied() {
		// GIVEN
		tokenActor(a, SecurityTokenType.ReadNodeData, policy("{\"aggregations\":[\"Day\"]}"))
				.become();
		final DatumFilterCommand hourFilter = nodeFilter(a.privateNodeId());
		hourFilter.setAggregate(Aggregation.Hour);
		final DatumFilterCommand dayFilter = nodeFilter(a.privateNodeId());
		dayFilter.setAggregate(Aggregation.Day);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Aggregation not in policy denied")
			.isThrownBy(() -> biz.findFilteredAggregateGeneralNodeDatum(hourFilter, null, null,
					null))
			;
		thenExceptionOfType(AuthorizationException.class)
			.as("Raw datum not in policy denied")
			.isThrownBy(() -> biz.findFilteredGeneralNodeDatum(nodeFilter(a.privateNodeId()), null,
					null, null))
			;
		then(biz.findFilteredAggregateGeneralNodeDatum(dayFilter, null, null, null))
			.as("Aggregation in policy allowed")
			.isNotNull()
			;
		// @formatter:on
	}

	@Test
	public void streamQuery_otherUserStreamRemoved() throws Exception {
		// GIVEN
		a.userActor().become();
		final ObjectDatumStreamIdentity aStream = a.stream(a.privateNodeId(),
				a.sourceIds().getFirst());
		final ObjectDatumStreamIdentity bStream = b.stream(b.privateNodeId(),
				b.sourceIds().getFirst());
		final StreamDatumFilterCommand filter = new StreamDatumFilterCommand();
		filter.setStreamIds(new UUID[] { aStream.getStreamId(), bStream.getStreamId() });

		// WHEN
		final var processor = new BasicStreamDatumFilteredResultsProcessor();
		biz.findFilteredStreamDatum(filter, processor, null, null, null);

		// THEN
		// @formatter:off
		then(processor.getData())
			.as("Only the user's stream returned")
			.hasSize(DATUM_COUNT)
			.allMatch(d -> aStream.getStreamId().equals(d.getStreamId()))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_streamQuery_nonPolicySourceRemoved() throws Exception {
		// GIVEN
		a.restrictedTokenActor().become();
		final ObjectDatumStreamIdentity policyStream = a.stream(a.otherPrivateNodeId(),
				a.sourceIds().getFirst());
		final ObjectDatumStreamIdentity otherStream = a.stream(a.otherPrivateNodeId(),
				a.sourceIds().get(1));
		final StreamDatumFilterCommand filter = new StreamDatumFilterCommand();
		filter.setStreamIds(
				new UUID[] { policyStream.getStreamId(), otherStream.getStreamId() });

		// WHEN
		final var processor = new BasicStreamDatumFilteredResultsProcessor();
		biz.findFilteredStreamDatum(filter, processor, null, null, null);

		// THEN
		// @formatter:off
		then(processor.getData())
			.as("Only the policy source stream returned")
			.hasSize(DATUM_COUNT)
			.allMatch(d -> policyStream.getStreamId().equals(d.getStreamId()))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_availableSourcesNarrowedToPolicy() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		final Set<String> sources = biz.getAvailableSources(nodeFilter(a.otherPrivateNodeId()));

		// THEN
		// @formatter:off
		then(sources)
			.as("Only the policy source available")
			.containsExactly(a.sourceIds().getFirst())
			;
		// @formatter:on
	}

	@Test
	public void findAvailableNodes_tokens() {
		// @formatter:off
		a.tokenActor().become();
		then(biz.findAvailableNodes(SecurityUtils.getCurrentActor()))
			.as("User token has all non-archived nodes of its user")
			.containsExactlyInAnyOrder(a.privateNodeId(), a.otherPrivateNodeId(), a.publicNodeId())
			;

		a.dataTokenActor().become();
		then(biz.findAvailableNodes(SecurityUtils.getCurrentActor()))
			.as("Data token has all non-archived nodes of its user")
			.containsExactlyInAnyOrder(a.privateNodeId(), a.otherPrivateNodeId(), a.publicNodeId())
			;

		a.restrictedTokenActor().become();
		then(biz.findAvailableNodes(SecurityUtils.getCurrentActor()))
			.as("Restricted token has only its policy nodes")
			.containsExactly(a.otherPrivateNodeId())
			;

		b.tokenActor().become();
		then(biz.findAvailableNodes(SecurityUtils.getCurrentActor()))
			.as("Other user token has only the other user's nodes")
			.containsExactlyInAnyOrder(b.privateNodeId(), b.otherPrivateNodeId(), b.publicNodeId())
			;
		// @formatter:on
	}

	@Test
	public void findAvailableSources_actors() {
		// @formatter:off
		a.restrictedTokenActor().become();
		then(biz.findAvailableSources(SecurityUtils.getCurrentActor(), new DatumFilterCommand()))
			.as("Restricted token has only its policy node sources")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;

		a.nodeActor().become();
		then(biz.findAvailableSources(SecurityUtils.getCurrentActor(), new DatumFilterCommand()))
			.as("Node has only its own sources")
			.containsExactlyInAnyOrderElementsOf(nodeSources(a.privateNodeId(), sources(a)))
			;

		a.tokenActor().become();
		final Collection<NodeSourcePK> tokenSources = biz
				.findAvailableSources(SecurityUtils.getCurrentActor(), new DatumFilterCommand());
		then(tokenSources)
			.as("User token has sources of its user's nodes only")
			.isNotEmpty()
			.allMatch(pk -> a.ownsNode(pk.getNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void anonymous_publicNodeReadable() {
		// GIVEN
		tenants.anonymous().become();

		// WHEN
		var results = biz.findFilteredGeneralNodeDatum(nodeFilter(a.publicNodeId()), null, null,
				null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Public node datum readable by anyone")
			.containsExactlyInAnyOrderElementsOf(nodeSources(a.publicNodeId(), sources(a)))
			;
		thenExceptionOfType(AuthorizationException.class)
			.as("Private node datum not readable anonymously")
			.isThrownBy(() -> biz.findFilteredGeneralNodeDatum(nodeFilter(a.privateNodeId()), null,
					null, null))
			;
		// @formatter:on
	}

}
