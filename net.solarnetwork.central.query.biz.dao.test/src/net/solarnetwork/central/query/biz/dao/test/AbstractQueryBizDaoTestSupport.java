/* ==================================================================
 * AbstractQueryBizDaoTestSupport.java - Nov 25, 2014 7:35:24 AM
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

package net.solarnetwork.central.query.biz.dao.test;

import static org.junit.Assert.assertNotNull;
import org.apache.ibatis.session.SqlSessionFactory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.JsonUtils;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.dao.UserDao;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserAuthTokenStatus;
import net.solarnetwork.central.user.domain.UserAuthTokenType;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Base class for other unit tests.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration
public abstract class AbstractQueryBizDaoTestSupport extends AbstractCentralTransactionalTest {

	public static final Long TEST_USER_ID = -2L;
	public static final String TEST_USER_EMAIL = "test@localhost";
	public static final String TEST_USER_NAME = "Foobar";
	public static final String TEST_USER_PASSWORD = "foobar";

	private SqlSessionFactory sqlSessionFactory;
	protected UserDao userDao;

	@Autowired
	protected SolarNodeDao solarNodeDao;

	@Autowired
	protected UserNodeDao userNodeDao;

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	@Before
	public void setupBaseSupport() {
		userDao = new MyBatisUserDao();
		((MyBatisUserDao) userDao).setSqlSessionFactory(sqlSessionFactory);
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	protected SolarNode getNode(Long nodeId) {
		return solarNodeDao.get(nodeId);
	}

	/**
	 * Persist a new User and return it.
	 * 
	 * @param email
	 *        the email of the new user
	 * @return the User
	 */
	protected User createNewUser(String email) {
		return userDao.get(storeNewUser(email));
	}

	/**
	 * Persist a new User and return its primary key.
	 * 
	 * @param email
	 *        the email of the new user
	 * @return the primary key
	 */
	protected Long storeNewUser(String email) {
		User newUser = new User();
		newUser.setCreated(new DateTime());
		newUser.setEmail(email);
		newUser.setName(TEST_USER_NAME);
		newUser.setPassword(TEST_USER_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		Long id = userDao.store(newUser);
		logger.debug("Got new user PK: " + id);
		assertNotNull(id);
		return id;
	}

	protected void storeNewUserNode(User user, SolarNode node) {
		UserNode newUserNode = new UserNode();
		newUserNode.setCreated(new DateTime());
		newUserNode.setDescription("Test description");
		newUserNode.setName("Test name");
		newUserNode.setNode(node);
		newUserNode.setUser(user);
		userNodeDao.store(newUserNode);
	}

	protected void becomeActor(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	protected void storeNewToken(String tokenId, String tokenSecret, Long userId,
			UserAuthTokenStatus status, UserAuthTokenType type, String policy) {
		jdbcTemplate.update(
				"INSERT INTO solaruser.user_auth_token(auth_token,auth_secret,user_id,status,token_type,jpolicy)"
						+ " VALUES (?,?,?,?::solaruser.user_auth_token_status,?::solaruser.user_auth_token_type,?::json)",
				tokenId, tokenSecret, userId, status.name(), type.name(), policy);
	}

	protected SecurityToken becomeAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		storeNewToken("user", "pass", userId, UserAuthTokenStatus.Active, UserAuthTokenType.ReadNodeData,
				JsonUtils.getJSONString(policy, null));
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				UserAuthTokenType.ReadNodeData.toString(), userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, userId.toString(),
				"ROLE_READNODEDATA");
		becomeActor(auth);
		return token;
	}

}
