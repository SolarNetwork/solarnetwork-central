/* ==================================================================
 * DaoUserBizTest.java - Dec 12, 2012 3:46:49 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.biz.dao.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.util.HashSet;
import java.util.Set;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.PasswordEncoder;
import net.solarnetwork.central.user.biz.dao.DaoUserBiz;
import net.solarnetwork.central.user.biz.dao.UserBizConstants;
import net.solarnetwork.central.user.dao.UserAuthTokenDao;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthToken;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link DaoUserBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserBizTest {

	private static final Long TEST_USER_ID = -1L;
	private static final String TEST_EMAIL = "test@localhost";
	private static final String TEST_PASSWORD = "changeit";
	private static final String TEST_ENC_PASSWORD = "encrypted.password";
	private static final String TEST_NAME = "Test User";
	private static final String TEST_ROLE = "ROLE_TEST";
	private static final String TEST_AUTH_TOKEN = "12345678901234567890";
	private static final String TEST_AUTH_SECRET = "123";

	private User testUser;
	private Set<String> testUserRoles;

	private UserDao userDao;
	private UserAuthTokenDao userAuthTokenDao;
	private PasswordEncoder passwordEncoder;

	private DaoUserBiz userBiz;

	@Before
	public void setup() {
		passwordEncoder = EasyMock.createMock(PasswordEncoder.class);

		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		testUser.setName(TEST_NAME);
		testUser.setPassword(TEST_ENC_PASSWORD);

		testUserRoles = new HashSet<String>();
		testUserRoles.add(TEST_ROLE);

		userDao = EasyMock.createMock(UserDao.class);
		userAuthTokenDao = EasyMock.createMock(UserAuthTokenDao.class);

		userBiz = new DaoUserBiz();
		userBiz.setUserDao(userDao);
		userBiz.setUserAuthTokenDao(userAuthTokenDao);
		userBiz.setPasswordEncoder(passwordEncoder);
	}

	/**
	 * Test able to logon a user successfully.
	 */
	@Test
	public void logonUser() {
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		expect(userDao.getUserRoles(testUser)).andReturn(testUserRoles);
		expect(passwordEncoder.matches(TEST_PASSWORD, TEST_ENC_PASSWORD)).andReturn(Boolean.TRUE);
		replay(userDao, passwordEncoder);

		final User user = userBiz.logonUser(TEST_EMAIL, TEST_PASSWORD);

		verify(userDao, passwordEncoder);

		assertNotNull(user);
		assertNotNull(user.getId());
		assertEquals(TEST_EMAIL, user.getEmail());
		assertEquals(TEST_NAME, user.getName());
	}

	/**
	 * Test attempting to logon an unconfirmed user fails.
	 */
	@Test
	public void attemptLogonUnconfirmedUser() {
		// make user's email "unconfirmed"
		testUser.setEmail(UserBizConstants.getUnconfirmedEmail(TEST_EMAIL));

		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(null);
		expect(userDao.getUserByEmail(testUser.getEmail())).andReturn(testUser);
		replay(userDao, passwordEncoder);

		try {
			userBiz.logonUser(TEST_EMAIL, TEST_PASSWORD);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.REGISTRATION_NOT_CONFIRMED, e.getReason());
			assertEquals(TEST_EMAIL, e.getEmail());
		}

		verify(userDao, passwordEncoder);
	}

	/**
	 * Test attempting to logon a non-existing email fails.
	 */
	@Test
	public void attemptLogonNonExistingEmail() {
		final String badEmail = "does@not.exist";
		expect(userDao.getUserByEmail(badEmail)).andReturn(null);
		expect(userDao.getUserByEmail(UserBizConstants.getUnconfirmedEmail(badEmail))).andReturn(null);
		replay(userDao);

		try {
			userBiz.logonUser(badEmail, TEST_PASSWORD);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.UNKNOWN_EMAIL, e.getReason());
			assertEquals(badEmail, e.getEmail());
		}
	}

	/**
	 * Test attempting to logon a null email fails.
	 */
	@Test
	public void attemptLogonNullEmail() {
		replay(userDao);

		try {
			userBiz.logonUser(null, TEST_PASSWORD);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.UNKNOWN_EMAIL, e.getReason());
			assertEquals(null, e.getEmail());
		}
		verify(userDao);
	}

	/**
	 * Test attempting to logon a bad password fails.
	 */
	@Test
	public void attemptLogonBadPassword() {
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		replay(userDao);

		try {
			userBiz.logonUser(TEST_EMAIL, "bad password");
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.BAD_PASSWORD, e.getReason());
			assertEquals(TEST_EMAIL, e.getEmail());
		}

		verify(userDao);
	}

	/**
	 * Test attempting to logon an empty password fails.
	 */
	@Test
	public void attemptLogonEmptyPassword() {
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		replay(userDao);

		try {
			userBiz.logonUser(TEST_EMAIL, "");
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.BAD_PASSWORD, e.getReason());
			assertEquals(TEST_EMAIL, e.getEmail());
		}

		verify(userDao);
	}

	/**
	 * Test attempting to logon an empty password fails.
	 */
	@Test
	public void attemptLogonNullPassword() {
		expect(userDao.getUserByEmail(TEST_EMAIL)).andReturn(testUser);
		replay(userDao);

		try {
			userBiz.logonUser(TEST_EMAIL, null);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.BAD_PASSWORD, e.getReason());
			assertEquals(TEST_EMAIL, e.getEmail());
		}

		verify(userDao);
	}

	private void replayProperties() {
		replay(userAuthTokenDao, userDao);
	}

	private void verifyProperties() {
		verify(userAuthTokenDao, userDao);
	}

	@Test
	public void generateUserAuthToken() {
		expect(userAuthTokenDao.get(anyObject(String.class))).andReturn(null);
		expect(userAuthTokenDao.store(anyObject(UserAuthToken.class))).andReturn(TEST_AUTH_TOKEN);
		replayProperties();
		UserAuthToken generated = userBiz.generateUserAuthToken(TEST_USER_ID, UserAuthTokenType.User,
				null);
		assertNotNull(generated);
		assertNotNull(generated.getAuthToken());
		assertEquals("Auth token should be exactly 20 characters", 20, generated.getAuthToken().length());
		assertNotNull(generated.getAuthSecret());
		assertEquals(TEST_USER_ID, generated.getUserId());
		assertEquals(UserAuthTokenStatus.Active, generated.getStatus());
		verifyProperties();
	}

	@Test
	public void deleteUserAuthToken() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				UserAuthTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);
		userAuthTokenDao.delete(same(token));
		replayProperties();
		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);
		verifyProperties();
	}

	@Test
	public void deleteUserAuthTokenNotFound() {
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(null);
		replayProperties();
		userBiz.deleteUserAuthToken(TEST_USER_ID, TEST_AUTH_TOKEN);
		verifyProperties();
	}

	@Test
	public void deleteUserAuthTokenWrongUser() {
		final UserAuthToken token = new UserAuthToken(TEST_AUTH_TOKEN, TEST_USER_ID, TEST_AUTH_SECRET,
				UserAuthTokenType.User);
		expect(userAuthTokenDao.get(TEST_AUTH_TOKEN)).andReturn(token);
		replayProperties();
		try {
			userBiz.deleteUserAuthToken(TEST_USER_ID - 1L, TEST_AUTH_TOKEN);
			fail("Should have thrown AuthorizationException");
		} catch ( AuthorizationException e ) {
			assertEquals(AuthorizationException.Reason.ACCESS_DENIED, e.getReason());
			assertEquals(TEST_AUTH_TOKEN, e.getId());
		}
		verifyProperties();
	}

}
