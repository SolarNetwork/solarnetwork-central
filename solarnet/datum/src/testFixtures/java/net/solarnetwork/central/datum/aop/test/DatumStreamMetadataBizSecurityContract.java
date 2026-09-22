/* ==================================================================
 * DatumStreamMetadataBizSecurityContract.java - 22/09/2026 2:19:25 pm
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import java.util.UUID;
import java.util.function.Consumer;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamIdentity;

/**
 * Security contract for {@link DatumStreamMetadataBiz}.
 *
 * <p>
 * The contract is enforced by {@code DatumStreamMetadataSecurityAspect}.
 * Updating node stream attributes requires write access to the stream's node,
 * and to the new node when the node changes. Updating location stream
 * attributes requires the operations role. The proxy must include a
 * {@link DatumStreamMetadataDao} mock.
 * </p>
 *
 * <p>
 * The find methods authorize the actor passed as an argument, so they are
 * exempt here.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class DatumStreamMetadataBizSecurityContract {

	private DatumStreamMetadataBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<DatumStreamMetadataBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final ObjectDatumStreamIdentity stream = a.stream(a.privateNodeId(), a.sourceIds().getFirst());
		final UUID streamId = stream.getStreamId();
		final Consumer<SecuredProxy<DatumStreamMetadataBiz>> streamExists = streamExists(stream);

		// @formatter:off
		return SecurityContract.forApi(DatumStreamMetadataBiz.class, tenants)
				.nodeWrite(biz -> biz.updateIdAttributes(ObjectDatumKind.Node, streamId, null, "/new"))
					.given(streamExists)
				.allowing(biz -> biz.updateIdAttributes(ObjectDatumKind.Node, streamId,
						b.privateNodeId(), null))
					.given(streamExists)
					.as("to other user node")
				.allowing(biz -> biz.updateIdAttributes(ObjectDatumKind.Node, UUID.randomUUID(), null,
						"/new"))
					.as("unknown stream")
				.allowing(biz -> biz.updateIdAttributes(ObjectDatumKind.Location, streamId, null,
						"/new"))
					.as("location stream")
				.nodeWrite(biz -> biz.updateAttributes(ObjectDatumKind.Node, streamId, null, null,
						new String[] { "a" }, null, null))
					.given(streamExists)
				.allowing(biz -> biz.updateAttributes(ObjectDatumKind.Node, streamId,
						b.privateNodeId(), null, null, null, null))
					.given(streamExists)
					.as("to other user node")
				.exempt("findDatumStreamMetadata", "authorizes the actor argument")
				.exempt("findDatumStreamMetadataIds", "authorizes the actor argument")
				.build();
		// @formatter:on
	}

	private static Consumer<SecuredProxy<DatumStreamMetadataBiz>> streamExists(
			ObjectDatumStreamIdentity stream) {
		return p -> given(p.mock(DatumStreamMetadataDao.class).findStreamMetadata(any()))
				.willReturn(new BasicObjectDatumStreamMetadata(stream.getStreamId(), "UTC",
						stream.getKind(), stream.getObjectId(), stream.getSourceId(), null, null,
						null));
	}

}
