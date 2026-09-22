/* ==================================================================
 * DatumMaintenanceBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.aop.DatumMaintenanceSecurityAspect;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.test.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.test.tenant.TenantDatumFixtures;
import net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum;
import net.solarnetwork.central.test.tenant.TestTenant;

/**
 * Verify {@link DatumMaintenanceBiz} only affects the requested nodes' datum
 * with the {@code DatumMaintenanceSecurityAspect} aspect applied, using the
 * database.
 *
 * @author matt
 * @version 1.0
 */
public class DatumMaintenanceBizSecurityPolicyTests extends BaseDatumSecurityPolicyTestSupport {

	private DatumMaintenanceBiz biz;

	@BeforeEach
	public void setup() {
		for ( TestTenant t : List.of(a, b) ) {
			TenantDatumFixtures.insertDatum(jdbcTemplate, t, DEFAULT_START, Duration.ofMinutes(5), 3);
		}
		biz = securedProxy((DatumMaintenanceBiz) new DaoDatumMaintenanceBiz(datumDao, datumDao),
				new DatumMaintenanceSecurityAspect(ownershipDao)).proxy();
	}

	private static DatumFilterCommand filter(Long nodeId) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		filter.setStartDate(DEFAULT_START.toInstant());
		filter.setEndDate(DEFAULT_START.plusHours(1).toInstant());
		return filter;
	}

	@Test
	public void user_markStale_ownNodeOnly() {
		// GIVEN
		a.userActor().become();

		// WHEN
		biz.markDatumAggregatesStale(filter(a.privateNodeId()));

		// THEN
		final List<StaleAggregateDatum> stale = DatumDbUtils.listStaleAggregateDatum(jdbcTemplate);
		// @formatter:off
		then(stale.stream().map(StaleAggregateDatum::getStreamId).collect(toSet()))
			.as("Only streams of the requested node marked stale")
			.containsExactlyInAnyOrderElementsOf(streamIds(a, a.privateNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void user_findStale_ownNodeOnly() {
		// GIVEN
		b.userActor().become();
		biz.markDatumAggregatesStale(filter(b.privateNodeId()));
		a.userActor().become();
		biz.markDatumAggregatesStale(filter(a.privateNodeId()));

		// WHEN
		var results = biz.findStaleAggregateDatum(filter(a.privateNodeId()), null, null, null);

		// THEN
		// @formatter:off
		then(StreamSupport.stream(results.spliterator(), false)
				.map(d -> new NodeSourcePK(d.getNodeId(), d.getSourceId())).collect(toSet()))
			.as("Only stale datum of the requested node returned")
			.isNotEmpty()
			.allMatch(pk -> a.privateNodeId().equals(pk.getNodeId()))
			;
		// @formatter:on
	}

}
