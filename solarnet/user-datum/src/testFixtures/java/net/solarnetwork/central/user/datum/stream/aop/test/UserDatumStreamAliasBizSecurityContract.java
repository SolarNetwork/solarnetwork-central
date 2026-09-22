/* ==================================================================
 * UserDatumStreamAliasBizSecurityContract.java - 22/09/2026 5:30:12 pm
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

package net.solarnetwork.central.user.datum.stream.aop.test;

import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.test.aop.SecuredProxy;
import net.solarnetwork.central.test.aop.SecurityContract;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link UserDatumStreamAliasBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserDatumStreamAliasSecurityAspect}, which
 * applies only to targets annotated with {@code Securable}. Saving an alias
 * requires write access to both of its nodes, and replacing an existing alias
 * also requires write access to the existing alias's original node, which
 * determines the alias owner. The proxy must include an
 * {@link ObjectDatumStreamAliasEntityDao} mock.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class UserDatumStreamAliasBizSecurityContract {

	private UserDatumStreamAliasBizSecurityContract() {
		// not available
	}

	/**
	 * Get the contract.
	 *
	 * @param tenants
	 *        the tenants
	 * @return the contract
	 */
	public static SecurityContract<UserDatumStreamAliasBiz> contract(TestTenants tenants) {
		final TestTenant a = tenants.a();
		final TestTenant b = tenants.b();
		final Long userId = a.userId();
		final String sourceId = a.sourceIds().getFirst();
		final ObjectDatumStreamAliasEntity ownAlias = alias(a.privateNodeId(), sourceId);
		final ObjectDatumStreamAliasEntity otherAlias = alias(b.privateNodeId(),
				b.sourceIds().getFirst());

		// @formatter:off
		return SecurityContract.forApi(UserDatumStreamAliasBiz.class, tenants)
				.userRead(biz -> biz.aliasForUser(userId, UUID.randomUUID()))
				.userRead(biz -> biz.listAliases(userId, null))
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(a.privateNodeId(), sourceId, a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(a.otherPrivateNodeId(), sourceId, a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(a.privateNodeId(), sourceId, b.privateNodeId())))
					.as("other user alias node")
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(a.privateNodeId(), sourceId, b.publicNodeId())))
					.as("other user public alias node")
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(b.publicNodeId(), b.sourceIds().getFirst(), a.privateNodeId())))
					.as("other user public original node")
				.allowing(biz -> biz.saveAlias(userId, ownAlias.getStreamId(),
						input(a.privateNodeId(), sourceId, a.privateNodeId())),
						a.userActor(), a.tokenActor())
					.given(aliasExists(ownAlias))
					.as("existing alias")
				.allowing(biz -> biz.saveAlias(userId, otherAlias.getStreamId(),
						input(a.privateNodeId(), sourceId, a.privateNodeId())))
					.given(aliasExists(otherAlias))
					.as("existing other user alias")
				.userWrite(biz -> biz.deleteAliases(userId, null))
				.build();
		// @formatter:on
	}

	private static ObjectDatumStreamAliasEntity alias(Long nodeId, String sourceId) {
		final Instant now = Instant.now();
		return new ObjectDatumStreamAliasEntity(UUID.randomUUID(), now, now, ObjectDatumKind.Node,
				nodeId, sourceId + "/alias", nodeId, sourceId);
	}

	private static ObjectDatumStreamAliasEntityInput input(Long originalNodeId,
			String originalSourceId, Long aliasNodeId) {
		final ObjectDatumStreamAliasEntityInput input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(originalNodeId);
		input.setOriginalSourceId(originalSourceId);
		input.setObjectId(aliasNodeId);
		input.setSourceId(originalSourceId + "/alias");
		return input;
	}

	private static Consumer<SecuredProxy<UserDatumStreamAliasBiz>> aliasExists(
			ObjectDatumStreamAliasEntity alias) {
		return p -> given(p.mock(ObjectDatumStreamAliasEntityDao.class).get(alias.getStreamId()))
				.willReturn(alias);
	}

}
