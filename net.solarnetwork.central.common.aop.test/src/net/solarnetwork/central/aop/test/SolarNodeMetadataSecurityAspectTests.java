/* ==================================================================
 * SolarNodeMetadataSecurityAspectTests.java - 11/11/2016 4:41:23 PM
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

package net.solarnetwork.central.aop.test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.aop.SolarNodeMetadataSecurityAspect;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.domain.SolarNodeMetadataMatch;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link SolarNodeMetadataSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SolarNodeMetadataSecurityAspectTests extends AbstractCentralTransactionalTest {

	private static final Long TEST_USER_ID = -11L;

	private UserNodeDao userNodeDao;
	private ProceedingJoinPoint pjp;
	private SolarNodeMetadataSecurityAspect aspect;

	private void replayAll() {
		EasyMock.replay(userNodeDao);
		if ( pjp != null ) {
			EasyMock.replay(pjp);
		}
	}

	private void verifyAll() {
		EasyMock.verify(userNodeDao);
		if ( pjp != null ) {
			EasyMock.verify(pjp);
		}
	}

	private void becomeUser(String... roles) {
		org.springframework.security.core.userdetails.User userDetails = new org.springframework.security.core.userdetails.User(
				"test@localhost", "foobar", AuthorityUtils.NO_AUTHORITIES);
		AuthenticatedUser user = new AuthenticatedUser(userDetails, TEST_USER_ID, "Test User", false);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(user, "foobar", roles);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		aspect = new SolarNodeMetadataSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataNoAuth() {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataWrongNode() {
		UserNode userNode = new UserNode(new User(-2L, "someoneelse@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test
	public void updateMetadataAllowed() {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_NODE_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataWrongNode() throws Throwable {
		UserNode userNode = new UserNode(new User(-2L, "someoneelse@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		userNode.setRequiresAuthorization(true);
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		SolarNodeMetadataMatch match = new SolarNodeMetadataMatch();
		match.setNodeId(TEST_NODE_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<SolarNodeMetadataFilterMatch> matches = Collections
				.singletonList((SolarNodeMetadataFilterMatch) match);
		final BasicFilterResults<SolarNodeMetadataFilterMatch> filterResults = new BasicFilterResults<SolarNodeMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		FilterSupport filter = new FilterSupport();
		filter.setNodeId(TEST_NODE_ID);

		becomeUser("ROLE_USER");
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();
		Assert.assertSame(filterResults, result);
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataNoNode() throws Throwable {
		FilterSupport filter = new FilterSupport();

		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		SolarNodeMetadataMatch match = new SolarNodeMetadataMatch();
		match.setNodeId(TEST_NODE_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<SolarNodeMetadataFilterMatch> matches = Collections
				.singletonList((SolarNodeMetadataFilterMatch) match);
		final BasicFilterResults<SolarNodeMetadataFilterMatch> filterResults = new BasicFilterResults<SolarNodeMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		becomeUser("ROLE_USER");
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();
		Assert.assertSame(filterResults, result);
	}

	@Test
	public void findMetadataAllowed() throws Throwable {
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		userNode.setRequiresAuthorization(true);
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		SolarNodeMetadataMatch match = new SolarNodeMetadataMatch();
		match.setNodeId(TEST_NODE_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<SolarNodeMetadataFilterMatch> matches = Collections
				.singletonList((SolarNodeMetadataFilterMatch) match);
		final BasicFilterResults<SolarNodeMetadataFilterMatch> filterResults = new BasicFilterResults<SolarNodeMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		FilterSupport filter = new FilterSupport();
		filter.setNodeId(TEST_NODE_ID);

		becomeUser("ROLE_USER");
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();
		Assert.assertSame(filterResults, result);
	}

	private void setUser(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private SecurityToken setAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				UserAuthTokenType.ReadNodeData.toString(), userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		setUser(auth);
		return token;
	}

	@Test
	public void findMetadataPolicyEnforced() throws Throwable {
		final UserNode userNode = new UserNode(new User(TEST_USER_ID, "test@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		userNode.setRequiresAuthorization(true);
		EasyMock.expect(userNodeDao.get(TEST_NODE_ID)).andReturn(userNode);

		final Set<String> metadataPaths = Collections.singleton("/pm/**/foo");
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(TEST_NODE_ID)).withNodeMetadataPaths(metadataPaths)
				.build();
		pjp = EasyMock.createMock(ProceedingJoinPoint.class);

		SolarNodeMetadataMatch match = new SolarNodeMetadataMatch();
		match.setNodeId(TEST_NODE_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<SolarNodeMetadataFilterMatch> matches = Collections
				.singletonList((SolarNodeMetadataFilterMatch) match);
		final BasicFilterResults<SolarNodeMetadataFilterMatch> filterResults = new BasicFilterResults<SolarNodeMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		FilterSupport filter = new FilterSupport();
		filter.setNodeId(TEST_NODE_ID);

		setAuthenticatedReadNodeDataToken(TEST_USER_ID, policy);
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();

		Assert.assertTrue("FilterResults created", result instanceof FilterResults);
		@SuppressWarnings("unchecked")
		FilterResults<SolarNodeMetadataFilterMatch> results = (FilterResults<SolarNodeMetadataFilterMatch>) result;
		SolarNodeMetadataFilterMatch resultMatch = results.iterator().next();

		GeneralDatumMetadata expectedResultMeta = new GeneralDatumMetadata();
		expectedResultMeta.putInfoValue("foo", "foo", "foo");
		GeneralDatumMetadata resultMeta = resultMatch.getMetadata();
		Assert.assertEquals("Result metadata", expectedResultMeta, resultMeta);

	}

}
