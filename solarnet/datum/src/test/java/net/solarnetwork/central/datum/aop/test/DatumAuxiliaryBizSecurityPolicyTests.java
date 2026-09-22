/* ==================================================================
 * DatumAuxiliaryBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.aop.DatumAuxiliarySecurityAspect;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumAuxiliaryEntityDao;
import net.solarnetwork.central.test.tenant.TestTenant;

/**
 * Verify security policies are enforced on {@link DatumAuxiliaryBiz} with the
 * {@code DatumAuxiliarySecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 1.0
 */
public class DatumAuxiliaryBizSecurityPolicyTests extends BaseDatumSecurityPolicyTestSupport {

	private static final int AUX_COUNT = 2;

	private DatumAuxiliaryBiz biz;

	@BeforeEach
	public void setup() {
		for ( TestTenant t : List.of(a, b) ) {
			TenantDatumFixtures.insertDatumAuxiliary(jdbcTemplate, t, DEFAULT_START, AUX_COUNT);
		}
		biz = securedProxy(
				(DatumAuxiliaryBiz) new DaoDatumAuxiliaryBiz(
						new JdbcDatumAuxiliaryEntityDao(jdbcTemplate), datumDao),
				new DatumAuxiliarySecurityAspect(ownershipDao, datumDao)).proxy();
	}

	private static DatumFilterCommand nodes(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static Set<NodeSourcePK> nodeSources(
			Iterable<GeneralNodeDatumAuxiliaryFilterMatch> results) {
		return StreamSupport.stream(results.spliterator(), false)
				.map(m -> nonnull(m.getId(), "ID"))
				.map(id -> new NodeSourcePK(id.getNodeId(), id.getSourceId()))
				.collect(toSet());
	}

	@Test
	public void user_ownNode() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findGeneralNodeDatumAuxiliary(nodes(a.privateNodeId()), null, null, null);

		// THEN
		// @formatter:off
		then(results)
			.as("Auxiliary datum of every node source returned")
			.hasSize(a.sourceIds().size() * AUX_COUNT)
			;
		then(nodeSources(results))
			.as("Only the node's sources returned")
			.allMatch(pk -> a.privateNodeId().equals(pk.getNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_policySourcesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findGeneralNodeDatumAuxiliary(nodes(a.otherPrivateNodeId()), null, null,
				null);

		// THEN
		// @formatter:off
		then(nodeSources(results))
			.as("Only the policy source returned")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		then(results)
			.as("All auxiliary datum of the policy source returned")
			.hasSize(AUX_COUNT)
			;
		// @formatter:on
	}

}
