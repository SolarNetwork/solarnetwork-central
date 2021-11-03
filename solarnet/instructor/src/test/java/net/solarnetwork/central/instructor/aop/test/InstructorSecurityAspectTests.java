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
import static org.easymock.EasyMock.expect;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.config.InstructorSecurityAspect;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenType;

/**
 * Test cases for the {@link InstructorSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InstructorSecurityAspectTests {

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private NodeInstructionDao nodeInstructionDao;
	private InstructorSecurityAspect service;

	@Before
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		nodeInstructionDao = EasyMock.createMock(NodeInstructionDao.class);
		service = new InstructorSecurityAspect(nodeOwnershipDao, nodeInstructionDao);
	}

	@After
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

	@Test(expected = AuthorizationException.class)
	public void instructionsForNodeDeniedNotOwner() {
		final Long nodeId = -1L;
		final Long userId = -100L;
		setAuthenticatedUserToken(userId, null);
		SolarNodeOwnership ownership = ownershipFor(nodeId, -200L);

		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		replayAll();

		service.instructionsForNodeCheck(nodeId);
	}

	@Test
	public void instructionsForNodesPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> nodeIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			long nodeId = (long) (Math.random() * 1000) + 1000L;
			nodeIds.add(nodeId);
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		}

		replayAll();

		service.instructionsForNodesCheck(nodeIds);
	}

	@Test(expected = AuthorizationException.class)
	public void instructionsForNodesDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> nodeIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			long nodeId = (long) (Math.random() * 1000) + 1000L;
			nodeIds.add(nodeId);
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);
		}

		// add one more node ID that is owned by another user
		final Long nodeId = (long) (Math.random() * 1000) + 2000L;
		nodeIds.add(nodeId);
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		replayAll();

		service.instructionsForNodesCheck(nodeIds);
	}

	@Test
	public void viewInstructionsByIdPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		List<NodeInstruction> instructions = new ArrayList<NodeInstruction>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = (long) (Math.random() * 1000) + 1000L;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = (long) (Math.random() * 1000) + 2000L;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			instructionIds.add(instrId);
			instructions.add(instr);
		}

		replayAll();

		service.viewInstructionsAccessCheck(instructionIds, instructions);
	}

	@Test(expected = AuthorizationException.class)
	public void viewInstructionsByIdDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		List<NodeInstruction> instructions = new ArrayList<NodeInstruction>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = (long) (Math.random() * 1000) + 1000L;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = (long) (Math.random() * 1000) + 2000L;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			instructionIds.add(instrId);
			instructions.add(instr);
		}

		// add one more instruction that is owned by another user's ndoe
		Long nodeId = (long) (Math.random() * 1000) - 1000L;
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		Long instrId = (long) (Math.random() * 1000) + 2000L;
		NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
		instr.setId(instrId);
		instructionIds.add(instrId);
		instructions.add(instr);

		replayAll();

		service.viewInstructionsAccessCheck(instructionIds, instructions);
	}

	@Test
	public void updateInstructionsStateByIdPass() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = (long) (Math.random() * 1000) + 1000L;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = (long) (Math.random() * 1000) + 2000L;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			expect(nodeInstructionDao.get(instrId)).andReturn(instr);
			instructionIds.add(instrId);
		}

		replayAll();

		service.updateInstructionsAccessCheck(instructionIds);
	}

	@Test(expected = AuthorizationException.class)
	public void updateInstructionsStateByIdDeniedNotOwner() {
		final Long userId = (long) (Math.random() * 1000);
		setAuthenticatedUserToken(userId, null);

		Set<Long> instructionIds = new LinkedHashSet<Long>();
		for ( int i = 0; i < 3; i++ ) {
			Long nodeId = (long) (Math.random() * 1000) + 1000L;
			SolarNodeOwnership ownership = ownershipFor(nodeId, userId);
			expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

			Long instrId = (long) (Math.random() * 1000) + 2000L;
			NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
			instr.setId(instrId);
			expect(nodeInstructionDao.get(instrId)).andReturn(instr);
			instructionIds.add(instrId);
		}

		// add one more instruction that is owned by another user's ndoe
		Long nodeId = (long) (Math.random() * 1000) - 1000L;
		SolarNodeOwnership ownership = ownershipFor(nodeId, -3L);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership);

		Long instrId = (long) (Math.random() * 1000) + 2000L;
		NodeInstruction instr = new NodeInstruction("foo", Instant.now(), nodeId);
		instr.setId(instrId);
		expect(nodeInstructionDao.get(instrId)).andReturn(instr);
		instructionIds.add(instrId);

		replayAll();

		service.updateInstructionsAccessCheck(instructionIds);
	}

}
