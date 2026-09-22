/* ==================================================================
 * AuditDatumBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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

package net.solarnetwork.central.datum.aop.test;

import static java.util.stream.Collectors.toSet;
import static net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures.DEFAULT_START;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.aop.AuditDatumSecurityAspect;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.biz.dao.DaoAuditDatumBiz;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcAuditDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.AuditDatumRollup;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Verify security policies are enforced on {@link AuditDatumBiz} with the
 * {@code AuditDatumSecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 1.0
 */
public class AuditDatumBizSecurityPolicyTests extends BaseDatumSecurityPolicyTestSupport {

	private static final int DAY_COUNT = 2;

	private AuditDatumBiz biz;

	@BeforeEach
	public void setup() {
		for ( TestTenant t : List.of(a, b) ) {
			TenantDatumFixtures.insertDailyAuditDatum(jdbcTemplate, t, DEFAULT_START, DAY_COUNT);
		}
		biz = securedProxy(
				(AuditDatumBiz) new DaoAuditDatumBiz(new JdbcAuditDatumEntityDao(jdbcTemplate)),
				new AuditDatumSecurityAspect(ownershipDao)).proxy();
	}

	private BasicDatumCriteria filter(Long userId) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(userId);
		filter.setAggregation(Aggregation.Day);
		filter.setStartDate(DEFAULT_START.toInstant());
		filter.setEndDate(DEFAULT_START.plusDays(DAY_COUNT).toInstant());
		filter.setDatumRollupType(DatumRollupType.None);
		return filter;
	}

	private static Set<NodeSourcePK> nodeSources(Iterable<AuditDatumRollup> results) {
		return StreamSupport.stream(results.spliterator(), false)
				.map(r -> new NodeSourcePK(nonnull(r.getNodeId(), "Node ID"),
						nonnull(r.getSourceId(), "Source ID")))
				.collect(toSet());
	}

	@Test
	public void user_ownAuditDatumOnly() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findAuditDatumFiltered(filter(a.userId()));

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Only audit datum of the user's node sources returned")
			.isNotEmpty()
			.allMatch(pk -> a.ownsNode(pk.getNodeId()))
			;
		// @formatter:on
	}

	@Disabled("Policy nodes and sources are not applied to audit datum")
	@Test
	public void restrictedToken_policyNodeSourcesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findAuditDatumFiltered(filter(a.userId()));

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Only audit datum of the policy node source returned")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		// @formatter:on
	}

}
