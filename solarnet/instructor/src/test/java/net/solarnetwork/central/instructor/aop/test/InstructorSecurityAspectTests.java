/* ==================================================================
 * InstructorSecurityAspectTests.java - 30/10/2017 7:24:56 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.thenExceptionOfType;
import static org.easymock.EasyMock.expect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.aop.InstructorSecurityAspect;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.instructor.support.SimpleInstructionFilter;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;

/**
 * Test cases for the {@link InstructorSecurityAspect} class.
 * 
 * @author matt
 * @version 1.1
 */
public class InstructorSecurityAspectTests {

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private NodeInstructionDao nodeInstructionDao;
	private InstructorSecurityAspect service;

	@BeforeEach
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);
		service = new InstructorSecurityAspect(nodeOwnershipDao, nodeInstructionDao);
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	@AfterEach
	public void teardown() {
		EasyMock.verify(nodeOwnershipDao, nodeInstructionDao);
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private void replayAll() {
		EasyMock.replay(nodeOwnershipDao, nodeInstructionDao);
	}

	private void setUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private SecurityToken setAuthenticatedUserToken(final Long userId, final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.User, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setUser(auth);
		return token;
	}

	@Test
	public void instructionsForNodePass() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		setAuthenticatedUserToken(userId, null);
		SolarNodeOwnership ownership = ownershipFor(nodeId, userId);

		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		service.instructionsForNodeCheck(nodeId);
	}

	@Test
	public void instructionsForNodeDeniedNotOwner() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		setAuthenticatedUserToken(userId, null);
		SolarNodeOwnership ownership = ownershipFor(nodeId, -200L);

		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			service.instructionsForNodeCheck(nodeId);
		});
	}

	@Test
	public void instructionsForNodesPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		long nodeIdCounter = (long) (Math.random() * 1000) + 1000L;

		Set<Long> nodeIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			long nodeId = nodeIdCounter++;
			nodeIds.add(nodeId);
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		}

		replayAll();

		service.instructionsForNodesCheck(nodeIds);
	}

	@Test
	public void instructionsForNodesDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		long nodeIdCounter = (long) (Math.random() * 1000) + 1000L;

		Set<Long> nodeIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			long nodeId = nodeIdCounter++;
			nodeIds.add(nodeId);
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		}

		// add one more node ID that is owned by another user
		final Long nodeId = nodeIdCounter++;
		nodeIds.add(nodeId);
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		replayAll();

		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			service.instructionsForNodesCheck(nodeIds);
		});
	}

	@Test
	public void viewInstructionsByIdPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		long nodeIdCounter = (long) (Math.random() * 1000) + 1000L;
		long instrIdCounter = (long) (Math.random() * 1000) + 2000;

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		List<NodeInstruction> instructions = new ArrayList<NodeInstruction>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = nodeIdCounter++;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = instrIdCounter++;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			instructionIds.add(instrId);
			instructions.add(instr);
		}

		replayAll();

		service.viewInstructionsAccessCheck(instructionIds, instructions);
	}

	@Test
	public void viewInstructionsByIdDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		long nodeIdCounter = (long) (Math.random() * 1000) + 1000L;
		long instrIdCounter = (long) (Math.random() * 1000) + 2000;

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		List<NodeInstruction> instructions = new ArrayList<NodeInstruction>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = nodeIdCounter++;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = instrIdCounter++;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			instructionIds.add(instrId);
			instructions.add(instr);
		}

		// add one more instruction that is owned by another user's ndoe
		Long nodeId = nodeIdCounter++;
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		Long instrId = instrIdCounter++;
		NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
		instr.setId(instrId);
		instructionIds.add(instrId);
		instructions.add(instr);

		replayAll();

		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			service.viewInstructionsAccessCheck(instructionIds, instructions);
		});
	}

	@Test
	public void updateInstructionsStateByIdPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		final long base = (long) (Math.random() * 1000L);

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = base + 1000 + i;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = base + 2000L + i;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			expect(nodeInstructionDao.get(instrId)).andReturn(instr);
			instructionIds.add(instrId);
		}

		replayAll();

		service.updateInstructionsAccessCheck(instructionIds);
	}

	@Test
	public void updateInstructionsStateByIdDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		long nodeIdCounter = (long) (Math.random() * 1000) + 1000L;
		long instrIdCounter = (long) (Math.random() * 1000) + 2000;
		Set<Long> instructionIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = nodeIdCounter++;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = instrIdCounter++;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			expect(nodeInstructionDao.get(instrId)).andReturn(instr);
			instructionIds.add(instrId);
		}

		// add one more instruction that is owned by another user's node
		Long nodeId = nodeIdCounter++;
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		Long instrId = instrIdCounter++;
		NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
		instr.setId(instrId);
		expect(nodeInstructionDao.get(instrId)).andReturn(instr);
		instructionIds.add(instrId);

		replayAll();

		thenExceptionOfType(AuthorizationException.class).isThrownBy(() -> {
			service.updateInstructionsAccessCheck(instructionIds);
		});
	}

	@Test
	public void findFilteredInstructionsCheck_noNodeOrInstructionIds() {
		// GIVEN
		final Long userId = randomLong();
		setAuthenticatedUserToken(userId, null);

		// WHEN
		replayAll();
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		thenExceptionOfType(AuthorizationException.class)
				.as("Missing node AND instruction IDs not allowed").isThrownBy(() -> {
					service.findFilteredInstructionsCheck(filter);
				});
	}

	@Test
	public void findFilteredInstructionsCheck_nodeIds_pass() {
		// GIVEN
		final Long nodeId = randomLong();
		final Long userId = randomLong();
		setAuthenticatedUserToken(userId, null);

		SolarNodeOwnership ownership = ownershipFor(nodeId, userId);

		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		// WHEN
		replayAll();
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(nodeId);
		service.findFilteredInstructionsCheck(filter);
	}

	@Test
	public void findFilteredInstructionsCheck_nodeIds_fail() {
		// GIVEN
		final Long nodeId = randomLong();
		final Long userId = randomLong();
		final Long otherUserId = randomLong();

		SolarNodeOwnership ownership = ownershipFor(nodeId, otherUserId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		// WHEN
		replayAll();
		setAuthenticatedUserToken(userId, null);
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setNodeId(nodeId);
		thenExceptionOfType(AuthorizationException.class).as("Node not owned by actor")
				.isThrownBy(() -> {
					service.findFilteredInstructionsCheck(filter);
				});
	}

	@Test
	public void findFilteredInstructionsCheck_instructionIds_pass() {
		// GIVEN
		final Long nodeId = randomLong();
		final Long userId = randomLong();
		setAuthenticatedUserToken(userId, null);

		NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());
		instruction.setNodeId(nodeId);
		expect(nodeInstructionDao.get(instruction.getId())).andReturn(instruction);

		SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		// WHEN
		replayAll();
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(new Long[] { instruction.getId() });
		service.findFilteredInstructionsCheck(filter);
	}

	@Test
	public void findFilteredInstructionsCheck_instructionIds_fail() {
		// GIVEN
		final Long nodeId = randomLong();
		final Long userId = randomLong();
		final Long otherUserId = randomLong();

		NodeInstruction instruction = new NodeInstruction();
		instruction.setId(randomLong());
		instruction.setNodeId(nodeId);
		expect(nodeInstructionDao.get(instruction.getId())).andReturn(instruction);

		SolarNodeOwnership ownership = ownershipFor(nodeId, otherUserId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		// WHEN
		replayAll();
		setAuthenticatedUserToken(userId, null);
		SimpleInstructionFilter filter = new SimpleInstructionFilter();
		filter.setInstructionIds(new Long[] { instruction.getId() });
		thenExceptionOfType(AuthorizationException.class).as("Node not owned by actor")
				.isThrownBy(() -> {
					service.findFilteredInstructionsCheck(filter);
				});
	}

}
