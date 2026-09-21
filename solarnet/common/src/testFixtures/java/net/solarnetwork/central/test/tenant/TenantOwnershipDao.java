/* ==================================================================
 * TenantOwnershipDao.java - 22/09/2026 8:39:25 am
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

package net.solarnetwork.central.test.tenant;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.test.tenant.TestTenant.NodeStream;
import net.solarnetwork.domain.SecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * {@link SolarNodeOwnershipDao} backed by {@link TestTenant} data, for tests
 * that do not use a database.
 *
 * <p>
 * The results mirror {@code JdbcSolarNodeOwnershipDao} for the same data.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public class TenantOwnershipDao implements SolarNodeOwnershipDao {

	private final List<TestTenant> tenants;

	/**
	 * Constructor.
	 *
	 * @param tenants
	 *        the tenants
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public TenantOwnershipDao(List<TestTenant> tenants) {
		super();
		this.tenants = requireNonNullArgument(tenants, "tenants");
	}

	private static SolarNodeOwnership ownership(TestTenant tenant, Long nodeId) {
		return new BasicSolarNodeOwnership(nodeId, tenant.userId(), TestTenant.COUNTRY,
				ZoneId.of(TestTenant.TIME_ZONE), tenant.isPrivate(nodeId), tenant.isArchived(nodeId));
	}

	@Override
	public @Nullable SolarNodeOwnership ownershipForNodeId(Long nodeId) {
		for ( TestTenant tenant : tenants ) {
			if ( tenant.ownsNode(nodeId) ) {
				return ownership(tenant, nodeId);
			}
		}
		return null;
	}

	@Override
	public SolarNodeOwnership @Nullable [] ownershipsForUserId(Long userId) {
		for ( TestTenant tenant : tenants ) {
			if ( tenant.userId().equals(userId) ) {
				return tenant.nodeIds().stream().map(nodeId -> ownership(tenant, nodeId))
						.toArray(SolarNodeOwnership[]::new);
			}
		}
		return null;
	}

	@Override
	public Long[] nonArchivedNodeIdsForToken(String tokenId) {
		for ( TestTenant tenant : tenants ) {
			final TestToken token = tenant.token(tokenId);
			if ( token == null ) {
				continue;
			}
			final SecurityPolicy policy = token.policy();
			final Set<Long> policyNodeIds = (policy != null ? policy.getNodeIds() : null);
			return tenant.nodeIds().stream().filter(nodeId -> !tenant.isArchived(nodeId))
					.filter(nodeId -> policyNodeIds == null || policyNodeIds.contains(nodeId))
					.sorted().toArray(Long[]::new);
		}
		return new Long[0];
	}

	@Override
	public Map<UUID, ObjectDatumStreamMetadataId> getDatumStreamMetadataIds(UUID... streamIds) {
		final Map<UUID, ObjectDatumStreamMetadataId> result = new LinkedHashMap<>(
				streamIds.length);
		for ( UUID streamId : streamIds ) {
			for ( TestTenant tenant : tenants ) {
				for ( NodeStream s : tenant.streams() ) {
					if ( s.streamId().equals(streamId) ) {
						result.put(streamId, new ObjectDatumStreamMetadataId(streamId,
								ObjectDatumKind.Node, s.nodeId(), s.sourceId()));
					}
				}
			}
		}
		return result;
	}

}
