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

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.security.AuthenticatedToken;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;

/**
 * Base class for other unit tests.
 * 
 * @author matt
 * @version 2.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(net.solarnetwork.central.query.config.DataSourceConfig.class)
public abstract class AbstractQueryBizDaoTestSupport extends AbstractCentralTransactionalTest {

	public static final Long TEST_USER_ID = -2L;
	public static final String TEST_USER_EMAIL = "test@localhost";
	public static final String TEST_USER_NAME = "Foobar";
	public static final String TEST_USER_PASSWORD = "foobar";

	protected SolarNodeDao solarNodeDao;

	@Autowired
	private SqlSessionFactory sqlSessionFactory;

	@Autowired
	private SqlSessionTemplate sqlSessionTemplate;

	protected SqlSessionTemplate getSqlSessionTemplate() {
		return sqlSessionTemplate;
	}

	protected SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	@Before
	public void setupBaseSupport() {
		solarNodeDao = new MyBatisSolarNodeDao();
		((MyBatisSolarNodeDao) solarNodeDao).setSqlSessionFactory(sqlSessionFactory);
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	protected SolarNode getNode(Long nodeId) {
		return solarNodeDao.get(nodeId);
	}

	/**
	 * Persist a new User and return its primary key.
	 * 
	 * @param email
	 *        the email of the new user
	 * @return the primary key
	 */
	protected Long storeNewUser(String email) {
		return CommonDbTestUtils.insertUser(jdbcTemplate, email);
	}

	protected void storeNewUserNode(Long userId, SolarNode node) {
		CommonDbTestUtils.insertUserNode(jdbcTemplate, userId, node.getId());
	}

	protected void becomeActor(Authentication auth) {
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	protected SecurityToken becomeAuthenticatedReadNodeDataToken(final Long userId,
			final SecurityPolicy policy) {
		CommonDbTestUtils.insertSecurityToken(jdbcTemplate, "user", "pass", userId,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(),
				JsonUtils.getJSONString(policy, null));
		AuthenticatedToken token = new AuthenticatedToken(
				new org.springframework.security.core.userdetails.User("user", "pass", true, true, true,
						true, AuthorityUtils.NO_AUTHORITIES),
				SecurityTokenType.ReadNodeData, userId, policy);
		TestingAuthenticationToken auth = new TestingAuthenticationToken(token, userId.toString(),
				"ROLE_READNODEDATA");
		becomeActor(auth);
		return token;
	}

}
