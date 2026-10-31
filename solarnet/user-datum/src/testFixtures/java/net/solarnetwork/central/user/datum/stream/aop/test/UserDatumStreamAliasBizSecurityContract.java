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

import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static org.mockito.BDDMockito.given;
import java.time.Instant;
import java.util.List;
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
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Security contract for {@link UserDatumStreamAliasBiz}.
 *
 * <p>
 * The contract is enforced by {@code UserDatumStreamAliasSecurityAspect}, which
 * applies only to targets annotated with {@code Securable}. Saving an alias
 * requires write access to its alias node, and to its original node unless the
 * original stream has been shared with the alias node, which also requires an
 * unrestricted actor. Replacing an existing alias also requires write access to
 * the existing alias node, which determines the alias owner. Alias grants
 * require an unrestricted actor, and write access to any nodes they name. The
 * proxy must include an {@link ObjectDatumStreamAliasEntityDao} mock.
 * </p>
 *
 * @author matt
 * @version 1.1
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
				.allowing(biz -> biz.saveAlias(userId, UNASSIGNED_UUID_ID,
						input(b.privateNodeId(), b.sourceIds().getFirst(), a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor())
					.given(aliasAllowed(b.privateNodeId(), b.sourceIds().getFirst(),
							a.otherPrivateNodeId()))
					.as("other user original node granted")
				.allowing(biz -> biz.saveAlias(userId, ownAlias.getStreamId(),
						input(a.privateNodeId(), sourceId, a.privateNodeId())),
						a.userActor(), a.tokenActor())
					.given(aliasExists(ownAlias))
					.as("existing alias")
				.allowing(biz -> biz.saveAlias(userId, otherAlias.getStreamId(),
						input(a.privateNodeId(), sourceId, a.privateNodeId())))
					.given(aliasExists(otherAlias))
					.as("existing other user alias")
				.allowing(biz -> biz.saveAliases(userId,
						List.of(input(a.privateNodeId(), sourceId, a.privateNodeId()))),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.saveAliases(userId,
						List.of(input(a.otherPrivateNodeId(), sourceId, a.otherPrivateNodeId()))),
						a.userActor(), a.tokenActor(), a.restrictedTokenActor())
					.as("policy node")
				.allowing(biz -> biz.saveAliases(userId,
						List.of(input(a.privateNodeId(), sourceId, a.privateNodeId()),
								input(b.privateNodeId(), b.sourceIds().getFirst(), a.privateNodeId()))))
					.as("one other user original node")
				.userWrite(biz -> biz.deleteAliases(userId, null))
				.allowing(biz -> biz.aliasGrantForUser(userId, 1L),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.listAliasGrants(userId, null),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.listReceivedAliasGrants(userId, null),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.listAvailableAliasStreams(userId, a.privateNodeId(), null),
						a.userActor(), a.tokenActor(), a.nodeActor())
				.allowing(biz -> biz.listAvailableAliasStreams(userId, b.privateNodeId(), null))
					.as("other user node")
				.allowing(biz -> biz.saveAliasGrant(userId, UNASSIGNED_LONG_ID,
						grantInput(b.userId(), a.privateNodeId())),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.saveAliasGrant(userId, UNASSIGNED_LONG_ID,
						grantInput(b.userId(), a.otherPrivateNodeId())),
						a.userActor(), a.tokenActor())
					.as("policy node")
				.allowing(biz -> biz.saveAliasGrant(userId, UNASSIGNED_LONG_ID,
						grantInput(b.userId(), b.privateNodeId())))
					.as("other user node")
				.allowing(biz -> biz.updateAliasGrantEnabled(userId, 1L, true),
						a.userActor(), a.tokenActor())
				.allowing(biz -> biz.deleteAliasGrant(userId, 1L),
						a.userActor(), a.tokenActor())
				.build();
		// @formatter:on
	}

	private static ObjectDatumStreamAliasEntity alias(Long nodeId, String sourceId) {
		final Instant now = Instant.now();
		return new ObjectDatumStreamAliasEntity(UUID.randomUUID(), now, now, ObjectDatumKind.Node,
				nodeId, sourceId + "/alias", nodeId, sourceId);
	}

	private static ObjectDatumStreamAliasEntityInput input(Long originalNodeId, String originalSourceId,
			Long aliasNodeId) {
		final ObjectDatumStreamAliasEntityInput input = new ObjectDatumStreamAliasEntityInput();
		input.setOriginalObjectId(originalNodeId);
		input.setOriginalSourceId(originalSourceId);
		input.setObjectId(aliasNodeId);
		input.setSourceId(originalSourceId + "/alias");
		return input;
	}

	private static ObjectDatumStreamAliasGrantEntityInput grantInput(Long granteeId, Long nodeId) {
		final ObjectDatumStreamAliasGrantEntityInput input = new ObjectDatumStreamAliasGrantEntityInput();
		input.setGranteeId(granteeId);
		input.setNodeIds(new Long[] { nodeId });
		return input;
	}

	private static Consumer<SecuredProxy<UserDatumStreamAliasBiz>> aliasAllowed(Long originalNodeId,
			String originalSourceId, Long aliasNodeId) {
		return p -> given(p.mock(ObjectDatumStreamAliasEntityDao.class).aliasAllowed(originalNodeId,
				originalSourceId, aliasNodeId)).willReturn(true);
	}

	private static Consumer<SecuredProxy<UserDatumStreamAliasBiz>> aliasExists(
			ObjectDatumStreamAliasEntity alias) {
		return p -> given(p.mock(ObjectDatumStreamAliasEntityDao.class).get(alias.getStreamId()))
				.willReturn(alias);
	}

}
