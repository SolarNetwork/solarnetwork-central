/* ==================================================================
 * QuerySecurityAspectTests.java - Mar 5, 2014 7:46:12 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import java.util.Arrays;
import java.util.HashSet;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.aop.QuerySecurityAspect;
import net.solarnetwork.central.security.AuthenticatedNode;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for the {@link QuerySecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class QuerySecurityAspectTests {

	private UserNodeDao userNodeDao;
	private QuerySecurityAspect service;

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		service = new QuerySecurityAspect(userNodeDao);
		service.setNodeIdNotRequiredSet(new HashSet<String>(Arrays.asList("price", "weather")));
	}

	@After
	public void teardown() {
		EasyMock.verify(userNodeDao);
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	private void setUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private AuthenticatedNode setAuthenticatedNode(final Long nodeId) {
		AuthenticatedNode node = new AuthenticatedNode(nodeId, null, false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(node, "foobar", "ROLE_NODE");
		setUser(auth);
		return node;
	}

	@Test
	public void datumQueryPublicNodeAsAuthenticatedNode() {
		AuthenticatedNode node = setAuthenticatedNode(-1L);
		UserNode userNode = new UserNode(new User(), new SolarNode(node.getNodeId(), null));

		EasyMock.expect(userNodeDao.get(node.getNodeId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(node.getNodeId());
		service.userNodeDatumAccessCheck(criteria);
	}

	@Test
	public void datumQueryPublicNodeAsAnonymous() {
		UserNode userNode = new UserNode(new User(), new SolarNode(-1L, null));

		EasyMock.expect(userNodeDao.get(userNode.getNode().getId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(userNode.getNode().getId());
		service.userNodeDatumAccessCheck(criteria);
	}

	@Test
	public void datumQueryPublicNodeAsSomeOtherNode() {
		setAuthenticatedNode(-2L);
		UserNode userNode = new UserNode(new User(), new SolarNode(-1L, null));

		EasyMock.expect(userNodeDao.get(userNode.getNode().getId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(userNode.getNode().getId());
		service.userNodeDatumAccessCheck(criteria);
	}

	@Test
	public void datumQueryPrivateNodeAsAuthenticatedNode() {
		AuthenticatedNode node = setAuthenticatedNode(-1L);
		UserNode userNode = new UserNode(new User(), new SolarNode(node.getNodeId(), null));
		userNode.setRequiresAuthorization(true);

		EasyMock.expect(userNodeDao.get(node.getNodeId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(node.getNodeId());
		service.userNodeDatumAccessCheck(criteria);
	}

	@Test
	public void datumQueryPrivateNodeAsAnonymous() {
		UserNode userNode = new UserNode(new User(), new SolarNode(-1L, null));
		userNode.setRequiresAuthorization(true);

		EasyMock.expect(userNodeDao.get(userNode.getNode().getId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(userNode.getNode().getId());
		try {
			service.userNodeDatumAccessCheck(criteria);
			Assert.fail("Should have thrown SecurityException for anonymous user");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

	@Test
	public void datumQueryPrivateNodeAsSomeOtherNode() {
		setAuthenticatedNode(-2L);
		UserNode userNode = new UserNode(new User(), new SolarNode(-1L, null));
		userNode.setRequiresAuthorization(true);

		EasyMock.expect(userNodeDao.get(userNode.getNode().getId())).andReturn(userNode);
		EasyMock.replay(userNodeDao);

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setDatumType("Consumption");
		criteria.setNodeId(userNode.getNode().getId());
		try {
			service.userNodeDatumAccessCheck(criteria);
			Assert.fail("Should have thrown SecurityException for anonymous user");
		} catch ( AuthorizationException e ) {
			Assert.assertEquals(Reason.ACCESS_DENIED, e.getReason());
		}
	}

}
