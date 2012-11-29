/* ==================================================================
 * DaoRegistrationBizTest.java - Nov 30, 2012 6:21:59 AM
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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.solarnetwork.central.in.biz.NetworkIdentityBiz;
import net.solarnetwork.central.user.biz.dao.DaoRegistrationBiz;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeConfirmationDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNodeConfirmation;
import net.solarnetwork.domain.BasicNetworkIdentity;
import net.solarnetwork.domain.NetworkAssociationDetails;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the {@link DaoRegistrationBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoRegistrationBizTest {

	private final Long TEST_USER_ID = -1L;
	private final Long TEST_CONF_ID = -2L;
	private final String TEST_EMAIL = "test@localhost";
	private final String TEST_SECURITY_PHRASE = "test phrase";

	private UserDao userDao;
	private User testUser;
	private UserNodeConfirmationDao userNodeConfirmationDao;
	private NetworkIdentityBiz networkIdentityBiz;
	private DaoRegistrationBiz registrationBiz;
	private BasicNetworkIdentity networkIdentity;

	@Before
	public void setup() {
		networkIdentity = new BasicNetworkIdentity("key", "tos", "host", 80, false);
		networkIdentityBiz = EasyMock.createMock(NetworkIdentityBiz.class);
		testUser = new User();
		testUser.setEmail(TEST_EMAIL);
		testUser.setId(TEST_USER_ID);
		userDao = EasyMock.createMock(UserDao.class);
		userNodeConfirmationDao = EasyMock.createMock(UserNodeConfirmationDao.class);
		registrationBiz = new DaoRegistrationBiz();
		registrationBiz.setNetworkIdentityBiz(networkIdentityBiz);
		registrationBiz.setUserDao(userDao);
		registrationBiz.setUserNodeConfirmationDao(userNodeConfirmationDao);
	}

	@Test
	public void createNodeAssociation() {
		expect(networkIdentityBiz.getNetworkIdentity()).andReturn(networkIdentity);
		expect(userDao.get(TEST_USER_ID)).andReturn(testUser);
		expect(userNodeConfirmationDao.store(EasyMock.anyObject(UserNodeConfirmation.class))).andReturn(
				TEST_CONF_ID);
		replay(networkIdentityBiz, userDao, userNodeConfirmationDao);

		final NetworkAssociationDetails result = registrationBiz.createNodeAssociation(TEST_USER_ID,
				TEST_SECURITY_PHRASE);

		verify(networkIdentityBiz, userDao, userNodeConfirmationDao);

		assertEquals(networkIdentity.getHost(), result.getHost());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(networkIdentity.getIdentityKey(), result.getIdentityKey());
		assertEquals(networkIdentity.getPort(), result.getPort());
		assertEquals(networkIdentity.getTermsOfService(), result.getTermsOfService());
		assertEquals(TEST_SECURITY_PHRASE, result.getSecurityPhrase());
		assertEquals(TEST_EMAIL, result.getUsername());
		assertNull(result.getNodeId());
	}
}
