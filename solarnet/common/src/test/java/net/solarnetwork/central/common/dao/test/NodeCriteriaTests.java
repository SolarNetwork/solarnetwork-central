/* ==================================================================
 * NodeCriteriaTests.java - 23/09/2026 11:45:00 am
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

package net.solarnetwork.central.common.dao.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.NodeCriteria;

/**
 * Test cases for the {@link NodeCriteria} interface.
 *
 * @author matt
 * @version 1.0
 */
public class NodeCriteriaTests {

	private static NodeCriteria criteria(Long... nodeIds) {
		final BasicCoreCriteria criteria = new BasicCoreCriteria();
		criteria.setNodeIds(nodeIds);
		return criteria;
	}

	@Test
	public void nodeIdsUnique_none() {
		// GIVEN
		final NodeCriteria criteria = criteria((Long[]) null);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("No node IDs gives an empty set")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_empty() {
		// GIVEN
		final NodeCriteria criteria = criteria();

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Empty node IDs gives an empty set")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_single() {
		// GIVEN
		final Long nodeId = randomLong();
		final NodeCriteria criteria = criteria(nodeId);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Single node ID returned")
			.containsExactly(nodeId)
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_multi() {
		// GIVEN
		final Long nodeId1 = randomLong();
		final Long nodeId2 = randomLong();
		final NodeCriteria criteria = criteria(nodeId1, nodeId2);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("All node IDs returned, in order")
			.containsExactly(nodeId1, nodeId2)
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_repeated() {
		// GIVEN
		final Long nodeId = randomLong();
		final NodeCriteria criteria = criteria(nodeId, nodeId);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Repeated node ID collapsed into one")
			.containsExactly(nodeId)
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_repeatedAmongOthers() {
		// GIVEN
		final Long nodeId1 = randomLong();
		final Long nodeId2 = randomLong();
		final NodeCriteria criteria = criteria(nodeId1, nodeId2, nodeId1);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Repeated node ID collapsed, first-seen order preserved")
			.containsExactly(nodeId1, nodeId2)
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_singleNull() {
		// GIVEN
		final NodeCriteria criteria = criteria((Long) null);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Null node ID ignored")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void nodeIdsUnique_nullAmongOthers() {
		// GIVEN
		final Long nodeId = randomLong();
		final NodeCriteria criteria = criteria(nodeId, null);

		// THEN
		// @formatter:off
		then(criteria.nodeIdsUnique())
			.as("Null node ID ignored")
			.containsExactly(nodeId)
			;
		// @formatter:on
	}

}
