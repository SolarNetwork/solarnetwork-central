/* ==================================================================
 * DatumMetadataBizSecurityPolicyTests.java - 22/09/2026 5:23:14 pm
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
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.jdbc.JdbcLocationRequestDao;
import net.solarnetwork.central.datum.aop.DatumMetadataSecurityAspect;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Verify security policies are enforced on {@link DatumMetadataBiz} with the
 * {@code DatumMetadataSecurityAspect} aspect applied, using the database.
 *
 * @author matt
 * @version 1.0
 */
public class DatumMetadataBizSecurityPolicyTests extends BaseDatumSecurityPolicyTestSupport {

	private DatumMetadataBiz biz;

	@BeforeEach
	public void setup() {
		biz = securedProxy(
				(DatumMetadataBiz) new DaoDatumMetadataBiz(datumDao,
						new JdbcLocationRequestDao(jdbcTemplate), JsonUtils.JSON_OBJECT_MAPPER),
				new DatumMetadataSecurityAspect(ownershipDao)).proxy();
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
	public void user_streamIds_ownNode() {
		// GIVEN
		a.userActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(nodes(a.privateNodeId()));

		// THEN
		// @formatter:off
		then(ids(results))
			.as("All streams of the node returned")
			.containsExactlyInAnyOrderElementsOf(streamIds(a, a.privateNodeId()))
			;
		// @formatter:on
	}

	@Test
	public void user_streamIds_userCriteria() {
		// GIVEN
		a.userActor().become();
		final BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setUserId(a.userId());

		// WHEN
		var results = biz.findDatumStreamMetadataIds(filter);

		// THEN
		// @formatter:off
		then(results)
			.as("Only streams of the user's nodes returned")
			.isNotEmpty()
			.allMatch(id -> a.ownsNode(id.getObjectId()))
			;
		// @formatter:on
	}

	@Disabled("Policy sources are not applied to datum metadata reads")
	@Test
	public void restrictedToken_streamIds_policySourcesOnly() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		var results = biz.findDatumStreamMetadataIds(nodes(a.otherPrivateNodeId()));

		// THEN
		// @formatter:off
		then(ids(results))
			.as("Only the policy source stream returned")
			.containsExactlyElementsOf(streamIds(a, a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		// @formatter:on
	}

	@Disabled("Policy sources are not applied to datum metadata reads")
	@Test
	public void restrictedToken_nodeDatumMetadata_policySourcesOnly() {
		// GIVEN
		jdbcTemplate.update("""
				UPDATE solardatm.da_datm_meta SET jdata = '{"m":{"a":1}}'::jsonb
				WHERE node_id = ?""", a.otherPrivateNodeId());
		a.restrictedTokenActor().become();
		final DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(a.otherPrivateNodeId());

		// WHEN
		var results = biz.findGeneralNodeDatumMetadata(filter, null, null, null);

		// THEN
		// @formatter:off
		then(StreamSupport.stream(results.spliterator(), false).map(m -> m.getId()).toList())
			.as("Only the policy source metadata returned")
			.containsExactly(new NodeSourcePK(a.otherPrivateNodeId(), a.sourceIds().getFirst()))
			;
		// @formatter:on
	}

}
