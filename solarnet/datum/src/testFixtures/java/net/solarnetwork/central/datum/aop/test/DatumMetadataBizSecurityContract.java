/* ==================================================================
 * DatumMetadataBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import net.solarnetwork.central.common.dao.BasicLocationRequestCriteria;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestActor;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link DatumMetadataBiz}.
 *
 * <p>
 * The contract is enforced by {@code DatumMetadataSecurityAspect}. Node
 * metadata requires access to every node in the request, location metadata is
 * public but can only be modified by location metadata administrators, and
 * location requests require access to the user.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class DatumMetadataBizSecurityContract {

	private DatumMetadataBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<DatumMetadataBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final String sourceId = a.sourceIds().getFirst();
		final Long[] bothNodes = new Long[] { a.privateNodeId(), b.privateNodeId() };
		final TestActor[] everyone = tenants.actors().toArray(TestActor[]::new);

		// @formatter:off
		return SecurityContract.forApi(DatumMetadataBiz.class, tenants)
				.nodeWrite(biz -> biz.addGeneralNodeDatumMetadata(a.privateNodeId(), sourceId,
						new GeneralDatumMetadata()))
				.nodeWrite(biz -> biz.storeGeneralNodeDatumMetadata(a.privateNodeId(), sourceId,
						new GeneralDatumMetadata()))
				.nodeWrite(biz -> biz.removeGeneralNodeDatumMetadata(a.privateNodeId(), sourceId))
				.nodeRead(biz -> biz.findGeneralNodeDatumMetadata(nodeFilter(a.privateNodeId()), null,
						null, null))
				.allowing(biz -> biz.findGeneralNodeDatumMetadata(nodeFilter(bothNodes), null, null,
						null))
					.as("other user node")
				.allowing(biz -> biz.findGeneralNodeDatumMetadata(new DatumFilterCommand(), null, null,
						null))
					.as("no node")
				.allowing(biz -> biz.getGeneralNodeDatumMetadataFilteredSources(bothNodes, "(a=b)"))
					.as("other user node")
				.nodeRead(biz -> biz.getGeneralNodeDatumMetadataFilteredSources(
						new Long[] { a.privateNodeId() }, "(a=b)"))
				.nodeRead(biz -> biz.findDatumStreamMetadata(streamFilter(a.privateNodeId())))
				.allowing(biz -> biz.findDatumStreamMetadata(streamFilter(bothNodes)))
					.as("other user node")
				.userRead(biz -> biz.findDatumStreamMetadata(userStreamFilter(a.userId())))
					.as("user")
				.allowing(biz -> biz.findDatumStreamMetadata(locationStreamFilter(a.locationId())),
						everyone)
					.as("location")
				.nodeRead(biz -> biz.findDatumStreamMetadata(
						nodeLocationStreamFilter(a.locationId(), a.privateNodeId())))
					.as("node in location")
				.allowing(biz -> biz.findDatumStreamMetadata(nodeLocationStreamFilter(a.locationId())))
					.as("any node in location")
				.allowing(biz -> biz.findDatumStreamMetadata(new BasicDatumCriteria()))
					.as("no criteria")
				.nodeRead(biz -> biz.findDatumStreamMetadataIds(streamFilter(a.privateNodeId())))
				.allowing(biz -> biz.findDatumStreamMetadataIds(streamFilter(bothNodes)))
					.as("other user node")
				.allowing(biz -> biz.findDatumStreamMetadataIds(nodeLocationStreamFilter(a.locationId())))
					.as("any node in location")
				.allowing(biz -> biz.addGeneralLocationDatumMetadata(a.locationId(), sourceId,
						new GeneralDatumMetadata()))
				.allowing(biz -> biz.storeGeneralLocationDatumMetadata(a.locationId(), sourceId,
						new GeneralDatumMetadata()))
				.allowing(biz -> biz.removeGeneralLocationDatumMetadata(a.locationId(), sourceId))
				.exempt("findGeneralLocationDatumMetadata", "public location data")
				.exempt("getGeneralLocationDatumMetadataFilteredSources", "public location data")
				.userRead(biz -> biz.findLocationRequests(a.userId(), new BasicLocationRequestCriteria(),
						null, null, null))
				.userRead(biz -> biz.getLocationRequest(a.userId(), randomLong()))
				.userWrite(biz -> biz.submitLocationRequest(a.userId(), new LocationRequestInfo()))
				.userWrite(biz -> biz.updateLocationRequest(a.userId(), randomLong(),
						new LocationRequestInfo()))
				.userWrite(biz -> biz.removeLocationRequest(a.userId(), randomLong()))
				.build();
		// @formatter:on
	}

	private static DatumFilterCommand nodeFilter(Long... nodeIds) {
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static BasicDatumCriteria streamFilter(Long... nodeIds) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static BasicDatumCriteria userStreamFilter(Long userId) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(userId);
		return filter;
	}

	private static BasicDatumCriteria locationStreamFilter(Long locationId) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(locationId);
		return filter;
	}

	private static BasicDatumCriteria nodeLocationStreamFilter(Long locationId, Long... nodeIds) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Node);
		filter.setLocationId(locationId);
		if ( nodeIds.length > 0 ) {
			filter.setNodeIds(nodeIds);
		}
		return filter;
	}

}
