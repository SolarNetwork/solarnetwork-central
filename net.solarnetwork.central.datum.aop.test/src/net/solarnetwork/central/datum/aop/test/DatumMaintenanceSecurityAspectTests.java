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
import net.solarnetwork.central.datum.aop.DatumMaintenanceSecurityAspect;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Test cases for the {@link
 * 
 * @author matt
 * @version 1.0
 */
public class DatumMaintenanceSecurityAspectTests extends AbstractAspectTest {

	private UserNodeDao userNodeDao;
	private DatumMaintenanceSecurityAspect aspect;

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		aspect = new DatumMaintenanceSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		verifyAll();
	}

	private void replayAll() {
		EasyMock.replay(userNodeDao);
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao);
	}

	private void expectUserNode(Long userId, Long nodeId) {
		User user = new User(userId, null);
		SolarNode node = new SolarNode(nodeId, null);
		UserNode un = new UserNode(user, node);
		expect(userNodeDao.get(nodeId)).andReturn(un).anyTimes();
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckNoAuth() {
		// given
		expect(userNodeDao.get(TEST_NODE_ID)).andReturn(null).anyTimes();

		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		aspect.markDatumAggregatesStaleCheck(filter);
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
		aspect.markDatumAggregatesStaleCheck(filter);
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
		aspect.markDatumAggregatesStaleCheck(filter);
	}

	@Test(expected = AuthorizationException.class)
	public void markStaleCheckMultipleNodeIds() {
		// given
		Long userId = UUID.randomUUID().getMostSignificantBits();
		becomeUser(userId);

		expectUserNode(userId, -1L);
		expect(userNodeDao.get(-2L)).andReturn(null).anyTimes();

		// when
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeIds(new Long[] { -1L, -2L });
		aspect.markDatumAggregatesStaleCheck(filter);
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
		aspect.markDatumAggregatesStaleCheck(filter);
	}

}
