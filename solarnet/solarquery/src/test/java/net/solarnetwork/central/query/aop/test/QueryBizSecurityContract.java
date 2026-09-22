/* ==================================================================
 * QueryBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import java.util.UUID;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link QueryBiz}.
 *
 * <p>
 * The contract is enforced by {@code QuerySecurityAspect}, which applies only to
 * targets annotated with {@code Securable}. Datum queries require read access
 * to the nodes (or the nodes of the streams) and are subject to the security
 * policy's node and source restrictions. Queries for several nodes are
 * narrowed to the nodes the actor can read, rather than denied.
 * </p>
 *
 * <p>
 * The {@code SecurityActor} methods authorize the actor passed as an argument,
 * and location datum is public, so those methods are exempt. Locations are
 * public too, but the aspect applies to location searches.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class QueryBizSecurityContract {

	private QueryBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<QueryBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final String policySourceId = a.sourceIds().getFirst();
		final String otherSourceId = a.sourceIds().get(1);
		final Long[] bothNodes = new Long[] { a.privateNodeId(), b.privateNodeId() };
		final UUID streamId = a.stream(a.privateNodeId(), policySourceId).getStreamId();
		final TestActor[] everyone = tenants.actors().toArray(TestActor[]::new);

		// @formatter:off
		return SecurityContract.forApi(QueryBiz.class, tenants)
				.nodeRead(biz -> biz.getReportableInterval(a.privateNodeId(), policySourceId))
				.allowing(biz -> biz.getReportableInterval(a.otherPrivateNodeId(), policySourceId),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node and source")
				.allowing(biz -> biz.getReportableInterval(a.otherPrivateNodeId(), otherSourceId),
						a.userActor(), a.tokenActor(), a.dataTokenActor())
					.as("policy node, other source")
				.nodeRead(biz -> biz.findReportableInterval(nodeFilter(a.privateNodeId())))
				.allowing(biz -> biz.findReportableInterval(nodeFilter(a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node")
				.nodeRead(biz -> biz.getAvailableSources(nodeFilter(a.privateNodeId())))
				.allowing(biz -> biz.getAvailableSources(nodeFilter(bothNodes)))
					.as("other user node")
				.nodeRead(biz -> biz.findAvailableSources(nodeFilter(a.privateNodeId())))
				.allowing(biz -> biz.findAvailableSources(nodeFilter(bothNodes)))
					.as("other user node")
				.exempt("findAvailableNodes", "authorizes the actor argument")
				.exempt(biz -> biz.findAvailableSources(null, null), "authorizes the actor argument")
				.nodeRead(biz -> biz.findFilteredGeneralNodeDatum(nodeFilter(a.privateNodeId()), null,
						null, null))
				.allowing(biz -> biz.findFilteredGeneralNodeDatum(nodeFilter(a.otherPrivateNodeId()),
						null, null, null),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor(), a.dataTokenActor())
					.as("policy node")
				.allowing(biz -> biz.findFilteredGeneralNodeDatum(new DatumFilterCommand(), null, null,
						null))
					.as("no node")
					// the restricted token's policy sources are resolved with getAvailableSources()
					.dependsOnTarget()
				.nodeRead(biz -> biz.findFilteredAggregateGeneralNodeDatum(
						nodeFilter(a.privateNodeId()), null, null, null))
				.nodeRead(biz -> biz.findFilteredStreamDatum(streamFilter(streamId), null, null, null,
						null))
				.nodeRead(biz -> biz.findFilteredStreamDatum(nodeStreamFilter(a.privateNodeId()), null,
						null, null, null))
					.as("node")
				.allowing(biz -> biz.findFilteredStreamDatum(streamFilter(UUID.randomUUID()), null, null,
						null, null))
					.as("unknown stream")
				.allowing(biz -> biz.findFilteredStreamDatum(new StreamDatumFilterCommand(), null, null,
						null, null))
					.as("no node or stream")
				.nodeRead(biz -> biz.findFilteredStreamReadings(streamFilter(streamId),
						DatumReadingType.Difference, null, null, null, null, null))
				.nodeRead(biz -> biz.findFilteredReading(nodeFilter(a.privateNodeId()),
						DatumReadingType.Difference, null))
				.nodeRead(biz -> biz.findFilteredAggregateReading(nodeFilter(a.privateNodeId()),
						DatumReadingType.Difference, null, null, null, null))
				.exempt("findGeneralLocationDatum", "public location data")
				.exempt("findAggregateGeneralLocationDatum", "public location data")
				.exempt("getLocationAvailableSources", "public location data")
				.exempt("getLocationReportableInterval", "public location data")
				.allowing(biz -> biz.findFilteredLocations(new SolarLocation(), null, null, null),
						everyone)
				.build();
		// @formatter:on
	}

	private static DatumFilterCommand nodeFilter(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static StreamDatumFilterCommand streamFilter(UUID streamId) {
		final StreamDatumFilterCommand filter = new StreamDatumFilterCommand();
		filter.setStreamId(streamId);
		return filter;
	}

	private static StreamDatumFilterCommand nodeStreamFilter(Long nodeId) {
		final StreamDatumFilterCommand filter = new StreamDatumFilterCommand();
		filter.setKind(ObjectDatumKind.Node);
		filter.setObjectIds(new Long[] { nodeId });
		return filter;
	}

}
