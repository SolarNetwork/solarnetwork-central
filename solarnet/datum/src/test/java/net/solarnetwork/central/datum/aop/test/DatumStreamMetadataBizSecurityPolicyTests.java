/* ==================================================================
 * DatumStreamMetadataBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.aop.DatumStreamMetadataSecurityAspect;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Verify {@link DatumStreamMetadataBiz} restricts stream metadata to the
 * actor, using the database.
 *
 * @author matt
 * @version 1.0
 */
public class DatumStreamMetadataBizSecurityPolicyTests extends BaseDatumSecurityPolicyTestSupport {

	private DatumStreamMetadataBiz biz;

	@BeforeEach
	public void setup() {
		biz = securedProxy((DatumStreamMetadataBiz) new DaoDatumStreamMetadataBiz(datumDao),
				new DatumStreamMetadataSecurityAspect(ownershipDao, datumDao)).proxy();
	}

	private static BasicDatumCriteria nodes(Long... nodeIds) {
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static Set<UUID> ids(Collection<ObjectDatumStreamMetadataId> results) {
		return results.stream().map(ObjectDatumStreamMetadataId::getStreamId).collect(toSet());
	}

	@Test
	public void user_otherUserNodeExcluded() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(SecurityUtils.getCurrentActor(),
				nodes(a.privateNodeId(), b.privateNodeId()));

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the user's node streams returned")
			.containsExactlyInAnyOrderElementsOf(streamIds(a, a.privateNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void user_allOwnStreams() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(SecurityUtils.getCurrentActor(),
				new BasicDatumCriteria());

		// THEN
		// @formatter:off
		then(results)
			.as("Only streams of the user's nodes returned")
			.isNotEmpty()
			.allMatch(id -> a.ownsNode(id.getObjectId()))
			;
		// @formatter:on
	}

	@Test
	public void otherUser_nothing() {
		// GIVEN
		b.userActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadata(SecurityUtils.getCurrentActor(),
				nodes(a.privateNodeId()));

		// THEN
		// @formatter:off
		then(results)
			.as("No streams of another user's node returned")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void restrictedToken_policyNodeAndSourceOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(SecurityUtils.getCurrentActor(),
				new BasicDatumCriteria());
		var metas = biz.findDatumStreamMetadata(SecurityUtils.getCurrentActor(),
				nodes(a.privateNodeId(), a.otherPrivateNodeId(), b.privateNodeId()));

		// THEN
		final UUID policyStreamId = a.stream(a.otherPrivateNodeId(), a.sourceIds().getFirst())
				.getStreamId();
		// @formatter:off
		then(ids(results))
			.as("Only the policy node source stream returned")
			.containsExactly(policyStreamId)
			;
		then(metas.stream().map(ObjectDatumStreamMetadata::getStreamId).collect(toSet()))
			.as("Only the policy node source stream metadata returned")
			.containsExactly(policyStreamId)
			;
		// @formatter:on
	}

	@Test
	public void dataToken_ownStreams() {
		// GIVEN
		b.dataTokenActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(SecurityUtils.getCurrentActor(),
				nodes(a.privateNodeId(), b.privateNodeId()));

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the token user's node streams returned")
			.containsExactlyInAnyOrderElementsOf(streamIds(b, b.privateNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void node_ownStreamsOnly() {
		// GIVEN
		a.nodeActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(SecurityUtils.getCurrentActor(),
				nodes(b.privateNodeId()));

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the node's own streams returned, whatever nodes are requested")
			.containsExactlyInAnyOrderElementsOf(streamIds(a, a.privateNodeId()))
			;
		// @formatter:on
	}

}
