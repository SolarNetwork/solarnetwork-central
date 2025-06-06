/* ==================================================================
 * AbstractMyBatisUserDaoTestSupport.java - 9/07/2018 11:33:06 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.expire.dao.mybatis.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.user.dao.mybatis.MyBatisUserDao;
import net.solarnetwork.central.user.domain.User;

/**
 * Base class for user DAO tests.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisUserDaoTestSupport extends AbstractJUnit5CentralTransactionalTest {

	public static final String TEST_EMAIL = "foo@localhost.localdomain";
	public static final String TEST_NAME = "Foo Bar";
	public static final String TEST_PASSWORD = "password";

	private SqlSessionFactory sqlSessionFactory;

	protected MyBatisUserDao userDao;

	@BeforeEach
	public void setup() {
		userDao = new MyBatisUserDao();
		userDao.setSqlSessionFactory(sqlSessionFactory);
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
		newUser.setName(TEST_NAME);
		newUser.setPassword(TEST_PASSWORD);
		newUser.setEnabled(Boolean.TRUE);
		Long id = userDao.save(newUser);
		log.debug("Got new user PK: " + id);
		then(id).isNotNull();
		return id;
	}

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

}
