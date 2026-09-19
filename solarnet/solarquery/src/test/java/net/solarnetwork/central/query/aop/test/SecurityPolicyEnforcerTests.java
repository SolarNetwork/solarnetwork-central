/* ==================================================================
 * SecurityPolicyEnforcerTests.java - 11/10/2016 9:04:26 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilter;
import net.solarnetwork.central.datum.domain.StreamDatumFilterCommand;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityPolicyEnforcer;
import net.solarnetwork.domain.BasicSecurityPolicy;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link SecurityPolicyEnforcer} class.
 * 
 * @author matt
 * @version 1.2
 */
public class SecurityPolicyEnforcerTests {

	private static final Long TEST_NODE_ID = 1L;
	private static final Long TEST_NODE_ID2 = 2L;

	private void rejectNegativeNodeId(Long nodeId) {
		if ( nodeId != null && nodeId < 0 ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, nodeId);
		}
	}

	@Test
	public void restrictNodeIdsWithValidator() {
		// GIVEN
		BasicSecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Set.of(TEST_NODE_ID, TEST_NODE_ID2, -1L)).build();

		// WHEN
		DatumFilterCommand cmd = new DatumFilterCommand();
		cmd.setNodeIds(new Long[] { TEST_NODE_ID, -1L });
		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd, null, null,
				this::rejectNegativeNodeId, null);

		GeneralNodeDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);

		// THEN
		// @formatter:off
		then(filter.getNodeIds())
			.as("Restricted node IDs to intersection of policy and validation")
			.containsExactlyInAnyOrder(TEST_NODE_ID)
			;
		// @formatter:on
	}

	private Function<UUID[], Map<UUID, ObjectDatumStreamMetadataId>> filteredIds(
			Map<UUID, ObjectDatumStreamMetadataId> ids) {
		return (streamIds) -> {
			if ( streamIds == null || streamIds.length < 1 ) {
				return Map.of();
			}
			return ids.entrySet().stream().filter(e -> {
				for ( UUID streamId : streamIds ) {
					if ( streamId.equals(e.getKey()) ) {
						return true;
					}
				}
				return false;
			}).collect(toMap(Entry::getKey, Entry::getValue));
		};
	}

	@Test
	public void restrictStreamIdsWithProvider() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		final UUID streamId2 = UUID.randomUUID();
		final Long nodeId = randomLong();
		final Map<UUID, ObjectDatumStreamMetadataId> ids = Map.of(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, randomString()),
				streamId2,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, -1L, randomString()));

		final var policy = new BasicSecurityPolicy.Builder().withNodeIds(Set.of(nodeId, -1L)).build();

		// WHEN
		StreamDatumFilterCommand cmd = new StreamDatumFilterCommand();
		cmd.setStreamIds(new UUID[] { streamId, streamId2 });

		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd, null, null,
				this::rejectNegativeNodeId, filteredIds(ids));

		StreamDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);

		// THEN
		// @formatter:off
		then(filter.getStreamIds())
			.as("Restricted stream IDs to intersection of policy and validation")
			.containsExactlyInAnyOrder(streamId)
			;
		// @formatter:on
	}

	@Test
	public void allowStreamIdsWithProvider() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		final UUID streamId2 = UUID.randomUUID();
		final Long nodeId = randomLong();
		final Map<UUID, ObjectDatumStreamMetadataId> ids = Map.of(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, randomString()),
				streamId2,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, -1L, randomString()));

		final var policy = new BasicSecurityPolicy.Builder().withNodeIds(Set.of(nodeId, -1L)).build();

		// WHEN
		StreamDatumFilterCommand cmd = new StreamDatumFilterCommand();
		cmd.setStreamIds(new UUID[] { streamId });

		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd, null, null,
				this::rejectNegativeNodeId, filteredIds(ids));

		StreamDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);

		// THEN
		// @formatter:off
		then(filter.getStreamIds())
			.as("Allowed stream IDs within intersection of policy and validation")
			.containsExactlyInAnyOrder(streamId)
			;
		// @formatter:on
	}

	@Test
	public void denyStreamIdsWithProvider() {
		// GIVEN
		final UUID streamId = UUID.randomUUID();
		final UUID streamId2 = UUID.randomUUID();
		final Long nodeId = randomLong();
		final Map<UUID, ObjectDatumStreamMetadataId> ids = Map.of(streamId,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, randomString()),
				streamId2,
				new ObjectDatumStreamMetadataId(streamId, ObjectDatumKind.Node, nodeId, randomString()));

		final var policy = new BasicSecurityPolicy.Builder().withNodeIds(Set.of(nodeId + 1L)).build();

		// WHEN
		StreamDatumFilterCommand cmd = new StreamDatumFilterCommand();
		cmd.setStreamIds(new UUID[] { streamId });

		SecurityPolicyEnforcer enforcer = new SecurityPolicyEnforcer(policy, "Tester", cmd, null, null,
				this::rejectNegativeNodeId, filteredIds(ids));

		StreamDatumFilter filter = SecurityPolicyEnforcer.createSecurityPolicyProxy(enforcer);

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			filter.getStreamIds();
		})
			.as("Rejected node IDs is exception ID")
			.returns(new Long[] {nodeId}, from(AuthorizationException::getId));
		// @formatter:on
	}

}
