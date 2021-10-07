/* ==================================================================
 * DatumMaintenanceSecurityAspectTests.java - 10/04/2019 11:34:37 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import static org.easymock.EasyMock.expect;
import java.util.UUID;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.config.DatumMaintenanceSecurityAspect;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthorizationException;

/**
 * Test cases for the {@link DatumMaintenanceSecurityAspect} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumMaintenanceSecurityAspectTests extends AbstractAspectTest {

	private SolarNodeOwnershipDao nodeOwnershipDao;
	private DatumMaintenanceSecurityAspect aspect;

	@Before
	public void setup() {
		nodeOwnershipDao = EasyMock.createMock(SolarNodeOwnershipDao.class);
		aspect = new DatumMaintenanceSecurityAspect(nodeOwnershipDao);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private void replayAll() {
		EasyMock.replay(nodeOwnershipDao);
	}

	private void verifyAll() {
		EasyMock.verify(nodeOwnershipDao);
	}

	private void expectUserNode(Long userId, Long nodeId) {
		SolarNodeOwnership ownership = BasicSolarNodeOwnership.ownershipFor(nodeId, userId);
		expect(nodeOwnershipDao.ownershipForNodeId(nodeId)).andReturn(ownership).anyTimes();
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckNoAuth() {
		// given
		expect(nodeOwnershipDao.ownershipForNodeId(TEST_NODE_ID)).andReturn(null).anyTimes();

		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		aspect.staleFilterCheck(filter);
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckDataToken() {
		// given
		setAuthenticatedReadNodeDataToken(1L, null);
		expectUserNode(1L, TEST_NODE_ID);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		aspect.staleFilterCheck(filter);
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckWrongUserId() {
		// given
		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		expectUserNode(1L, TEST_NODE_ID);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		aspect.staleFilterCheck(filter);
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckMultipleNodeIds() {
		// given
		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		expectUserNode(userId, -1L);
		expect(nodeOwnershipDao.ownershipForNodeId(-2L)).andReturn(null).anyTimes();

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { -1L, -2L });
		aspect.staleFilterCheck(filter);
	}

	@Test
	public void markStaleCheckPass() {
		// given
		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);
		expectUserNode(userId, TEST_NODE_ID);

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		aspect.staleFilterCheck(filter);
	}

}
