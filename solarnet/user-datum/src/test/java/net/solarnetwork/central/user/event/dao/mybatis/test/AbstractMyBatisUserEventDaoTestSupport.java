/* ==================================================================
 * AbstractMyBatisUserEventDaoTestSupport.java - 5/06/2020 11:34:53 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dao.mybatis.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;

/**
 * Base class for user DAO tests.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisUserEventDaoTestSupport extends AbstractCentralTransactionalTest {

	private SqlSessionFactory sqlSessionFactory;

	protected MyBatisUserDao userDao;
	protected MyBatisUserNodeDao userNodeDao;

	@Before
	public void setupAbstractTestSupport() {
		userDao = new MyBatisUserDao();
		userDao.setSqlSessionFactory(sqlSessionFactory);

		userNodeDao = new MyBatisUserNodeDao();
		userNodeDao.setSqlSessionFactory(sqlSessionFactory);
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
		newUser.setCreated(Instant.now());
		newUser.setEmail(email);
		newUser.setName(email);
		newUser.setPassword(UUID.randomUUID().toString());
		newUser.setEnabled(Boolean.TRUE);
		Long id = userDao.store(newUser);
		assertThat("User ID returned", id, notNullValue());
		return id;
	}

	/**
	 * Setup a new {@link UserNode} entity.
	 * 
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 */
	protected void setupUserNode(Long userId, Long nodeId) {
		UserNode un = new UserNode(userDao.get(userId), new SolarNode(nodeId, TEST_LOC_ID));
		userNodeDao.store(un);
	}

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

}
