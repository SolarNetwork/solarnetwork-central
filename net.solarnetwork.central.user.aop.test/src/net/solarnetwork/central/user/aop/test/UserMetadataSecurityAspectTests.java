/* ==================================================================
 * UserMetadataSecurityAspectTests.java - 11/11/2016 5:16:20 PM
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

package net.solarnetwork.central.user.aop.test;

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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.AuthenticatedUser;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.aop.UserMetadataSecurityAspect;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserFilterCommand;
import net.solarnetwork.central.user.domain.UserMetadataFilterMatch;
import net.solarnetwork.central.user.domain.UserMetadataMatch;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link UserMetadataSecurityAspect} class.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class UserMetadataSecurityAspectTests extends AbstractCentralTransactionalTest {

	private static final Long TEST_USER_ID = -11L;

	private UserNodeDao userNodeDao;
	private ProceedingJoinPoint pjp;
	private UserMetadataSecurityAspect aspect;

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

	private SecurityToken setAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				UserAuthTokenType.ReadNodeData.toString(), userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, "123", "ROLE_USER");
		SecurityContextHolder.getContext().setAuthentication(auth);
		return token;
	}

	@Before
	public void setup() {
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		aspect = new UserMetadataSecurityAspect(userNodeDao);
	}

	@After
	public void teardown() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataNoAuth() {
		replayAll();
		aspect.updateMetadataCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void updateMetadataWrongUser() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(-2L);
		verifyAll();
	}

	@Test
	public void updateMetadataAllowed() {
		becomeUser("ROLE_USER");
		replayAll();
		aspect.updateMetadataCheck(TEST_USER_ID);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataWrongUser() throws Throwable {
		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		UserMetadataMatch match = new UserMetadataMatch();
		match.setUserId(TEST_USER_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<UserMetadataFilterMatch> matches = Collections
				.singletonList((UserMetadataFilterMatch) match);
		final BasicFilterResults<UserMetadataFilterMatch> filterResults = new BasicFilterResults<UserMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		UserFilterCommand filter = new UserFilterCommand();
		filter.setUserId(-2L);

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(pjp, filter);
		verifyAll();
	}

	@Test(expected = AuthorizationException.class)
	public void findMetadataNoUser() throws Throwable {
		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		UserMetadataMatch match = new UserMetadataMatch();
		match.setUserId(TEST_USER_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<UserMetadataFilterMatch> matches = Collections
				.singletonList((UserMetadataFilterMatch) match);
		final BasicFilterResults<UserMetadataFilterMatch> filterResults = new BasicFilterResults<UserMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		UserFilterCommand filter = new UserFilterCommand();

		becomeUser("ROLE_USER");
		replayAll();
		aspect.readMetadataCheck(pjp, filter);
		verifyAll();
	}

	@Test
	public void findMetadataAllowed() throws Throwable {
		pjp = EasyMock.createMock(ProceedingJoinPoint.class);
		UserMetadataMatch match = new UserMetadataMatch();
		match.setUserId(TEST_USER_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<UserMetadataFilterMatch> matches = Collections
				.singletonList((UserMetadataFilterMatch) match);
		final BasicFilterResults<UserMetadataFilterMatch> filterResults = new BasicFilterResults<UserMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		UserFilterCommand filter = new UserFilterCommand();
		filter.setUserId(TEST_USER_ID);

		becomeUser("ROLE_USER");
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();
		Assert.assertSame(filterResults, result);
	}

	@Test
	public void findMetadataPolicyEnforced() throws Throwable {
		final Set<String> metadataPaths = Collections.singleton("/pm/**/foo");
		final SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(Collections.singleton(TEST_NODE_ID)).withUserMetadataPaths(metadataPaths)
				.build();
		pjp = EasyMock.createMock(ProceedingJoinPoint.class);

		UserMetadataMatch match = new UserMetadataMatch();
		match.setUserId(TEST_USER_ID);
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("foo", "foo", "foo");
		meta.putInfoValue("a", "b", "c");
		match.setMeta(meta);
		List<UserMetadataFilterMatch> matches = Collections
				.singletonList((UserMetadataFilterMatch) match);
		final BasicFilterResults<UserMetadataFilterMatch> filterResults = new BasicFilterResults<UserMetadataFilterMatch>(
				matches);

		EasyMock.expect(pjp.proceed()).andReturn(filterResults);

		UserFilterCommand filter = new UserFilterCommand();
		filter.setUserId(TEST_USER_ID);

		setAuthenticatedReadNodeDataToken(TEST_USER_ID, policy);
		replayAll();
		Object result = aspect.readMetadataCheck(pjp, filter);
		verifyAll();

		Assert.assertTrue("FilterResults created", result instanceof FilterResults);
		@SuppressWarnings("unchecked")
		FilterResults<UserMetadataFilterMatch> results = (FilterResults<UserMetadataFilterMatch>) result;
		UserMetadataFilterMatch resultMatch = results.iterator().next();

		GeneralDatumMetadata expectedResultMeta = new GeneralDatumMetadata();
		expectedResultMeta.putInfoValue("foo", "foo", "foo");
		GeneralDatumMetadata resultMeta = resultMatch.getMetadata();
		Assert.assertEquals("Result metadata", expectedResultMeta, resultMeta);

	}
}
