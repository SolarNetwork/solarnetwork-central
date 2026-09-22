/* ==================================================================
 * InstructorBizSecurityPolicyTests.java - 22/09/2026 6:32:40 pm
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

package net.solarnetwork.central.instructor.aop.test;

import static net.solarnetwork.central.test.CommonDbTestUtils.insertNodeInstruction;
import static net.solarnetwork.central.test.aop.SecuredProxies.securedProxy;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import net.solarnetwork.central.common.dao.jdbc.JdbcSolarNodeOwnershipDao;
import net.solarnetwork.central.instructor.aop.InstructorSecurityAspect;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.biz.dao.DaoInstructorBiz;
import net.solarnetwork.central.instructor.dao.mybatis.MyBatisNodeInstructionDao;
import net.solarnetwork.central.instructor.dao.mybatis.test.AbstractMyBatisDaoTestSupport;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.support.AbstractFilteredResultsProcessor;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenant;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Verify security policies are enforced on {@link InstructorBiz} with the
 * {@code InstructorSecurityAspect} aspect applied, using the database.
 *
 * <p>
 * The restricted token's policy allows only tenant A's other private node, so
 * its instructions for tenant A's private node are out of bounds.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(SecurityContextExtension.class)
public class InstructorBizSecurityPolicyTests extends AbstractMyBatisDaoTestSupport {

	private static final String TOPIC = "Test";

	private TestTenants tenants;
	private TestTenant a;
	private TestTenant b;
	private InstructorBiz biz;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		a = tenants.a();
		b = tenants.b();
		tenants.insert(jdbcTemplate);
		final MyBatisNodeInstructionDao dao = new MyBatisNodeInstructionDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		biz = securedProxy((InstructorBiz) new DaoInstructorBiz(dao),
				new InstructorSecurityAspect(new JdbcSolarNodeOwnershipDao(jdbcTemplate), dao))
				.proxy();
	}

	private Long queued(Long nodeId) {
		return insertNodeInstruction(jdbcTemplate, nodeId, TOPIC, InstructionState.Queued.name());
	}

	private long instructionCount(Long nodeId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM solarnet.sn_node_instruction WHERE node_id = ?", Long.class,
				nodeId), "Count");
	}

	private String state(Long instructionId) {
		return nonnull(jdbcTemplate.queryForObject(
				"SELECT deliver_state::text FROM solarnet.sn_node_instruction WHERE id = ?",
				String.class, instructionId), "State");
	}

	private static Instruction instruction() {
		return new Instruction(TOPIC, Instant.now());
	}

	private static SimpleInstructionFilter nodes(Long... nodeIds) {
		final SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeIds(nodeIds);
		return filter;
	}

	private static SimpleInstructionFilter ids(Long... instructionIds) {
		final SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(instructionIds);
		return filter;
	}

	private List<Long> find(SimpleInstructionFilter filter) throws IOException {
		final List<Long> results = new ArrayList<>();
		biz.findFilteredNodeInstructions(filter, new AbstractFilteredResultsProcessor<>() {

			@Override
			public void handleResultItem(NodeInstruction resultItem) {
				results.add(resultItem.getId());
			}

		});
		return results;
	}

	@Test
	public void queueInstruction_restrictedToken_policyNode() {
		// GIVEN
		a.restrictedTokenActor().become();

		// WHEN
		final NodeInstruction result = biz.queueInstruction(a.otherPrivateNodeId(), instruction());

		// THEN
		// @formatter:off
		then(result)
			.as("Instruction queued for policy node")
			.isNotNull()
			.returns(a.otherPrivateNodeId(), from(NodeInstruction::getNodeId))
			;
		then(instructionCount(a.otherPrivateNodeId()))
			.as("Instruction stored")
			.isEqualTo(1L)
			;
		// @formatter:on
	}

	@Test
	public void queueInstructions_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Queuing to a non-policy node denied")
			.isThrownBy(() -> biz.queueInstructions(Set.of(a.otherPrivateNodeId(), a.privateNodeId()),
					instruction()))
			;
		then(instructionCount(a.otherPrivateNodeId()) + instructionCount(a.privateNodeId()))
			.as("No instructions stored, including for the policy node")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void queueInstruction_dataToken_denied() {
		// GIVEN
		a.dataTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Data token cannot queue instructions")
			.isThrownBy(() -> biz.queueInstruction(a.privateNodeId(), instruction()))
			;
		then(instructionCount(a.privateNodeId()))
			.as("No instruction stored")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void findInstructions_restrictedToken_policyNode() throws IOException {
		// GIVEN
		final Long policyInstructionId = queued(a.otherPrivateNodeId());
		queued(a.privateNodeId());
		a.restrictedTokenActor().become();

		// WHEN
		final List<Long> results = find(nodes(a.otherPrivateNodeId()));

		// THEN
		// @formatter:off
		then(results)
			.as("Only the policy node instruction found")
			.containsExactly(policyInstructionId)
			;
		// @formatter:on
	}

	@Test
	public void findInstructions_restrictedToken_nonPolicyNode_denied() {
		// GIVEN
		queued(a.privateNodeId());
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Finding instructions of a non-policy node denied")
			.isThrownBy(() -> find(nodes(a.privateNodeId())))
			;
		// @formatter:on
	}

	@Test
	public void findInstructions_restrictedToken_nonPolicyInstructionId_denied() {
		// GIVEN
		final Long policyInstructionId = queued(a.otherPrivateNodeId());
		final Long otherInstructionId = queued(a.privateNodeId());
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Finding a non-policy node instruction by ID denied")
			.isThrownBy(() -> find(ids(policyInstructionId, otherInstructionId)))
			;
		// @formatter:on
	}

	@Test
	public void findInstructions_otherUserInstructionId_denied() {
		// GIVEN
		final Long instructionId = queued(b.privateNodeId());
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Finding another user's instruction by ID denied")
			.isThrownBy(() -> find(ids(instructionId)))
			;
		// @formatter:on
	}

	@Test
	public void getInstruction_otherUserInstruction_denied() {
		// GIVEN
		final Long instructionId = queued(b.privateNodeId());
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Viewing another user's instruction denied")
			.isThrownBy(() -> biz.getInstruction(instructionId))
			;
		// @formatter:on
	}

	@Test
	public void getInstruction_restrictedToken_nonPolicyInstruction_denied() {
		// GIVEN
		final Long instructionId = queued(a.privateNodeId());
		a.restrictedTokenActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Viewing a non-policy node instruction denied")
			.isThrownBy(() -> biz.getInstruction(instructionId))
			;
		// @formatter:on
	}

	@Test
	public void updateInstructionsState_otherUserInstruction_denied() {
		// GIVEN
		final Long instructionId = queued(a.privateNodeId());
		final Long otherUserInstructionId = queued(b.privateNodeId());
		a.userActor().become();

		// THEN
		// @formatter:off
		thenExceptionOfType(AuthorizationException.class)
			.as("Updating another user's instruction denied")
			.isThrownBy(() -> biz.updateInstructionsState(Set.of(instructionId, otherUserInstructionId),
					InstructionState.Declined))
			;
		then(state(otherUserInstructionId))
			.as("Other user's instruction unchanged")
			.isEqualTo(InstructionState.Queued.name())
			;
		then(state(instructionId))
			.as("User's instruction unchanged")
			.isEqualTo(InstructionState.Queued.name())
			;
		// @formatter:on
	}

	@Test
	public void updateInstructionsStateForUser_otherUserNode_notUpdated() {
		// GIVEN
		final Long instructionId = queued(a.privateNodeId());
		final Long otherUserInstructionId = queued(b.privateNodeId());
		a.userActor().become();

		// WHEN
		var result = biz.updateInstructionsStateForUser(a.userId(),
				nodes(a.privateNodeId(), b.privateNodeId()), InstructionState.Declined);

		// THEN
		// @formatter:off
		then(result)
			.as("Only the user's instruction updated")
			.containsExactly(instructionId)
			;
		then(state(otherUserInstructionId))
			.as("Other user's instruction unchanged")
			.isEqualTo(InstructionState.Queued.name())
			;
		// @formatter:on
	}

	@Disabled("Policy nodes are not applied to updating instructions state for a user")
	@Test
	public void updateInstructionsStateForUser_restrictedToken_policyNodeOnly() {
		// GIVEN
		final Long policyInstructionId = queued(a.otherPrivateNodeId());
		final Long otherInstructionId = queued(a.privateNodeId());
		a.restrictedTokenActor().become();

		// WHEN
		var result = biz.updateInstructionsStateForUser(a.userId(), new SimpleInstructionFilter(),
				InstructionState.Declined);

		// THEN
		// @formatter:off
		then(result)
			.as("Only the policy node instruction updated")
			.containsExactly(policyInstructionId)
			;
		then(state(otherInstructionId))
			.as("Non-policy node instruction unchanged")
			.isEqualTo(InstructionState.Queued.name())
			;
		// @formatter:on
	}

}
