/* ==================================================================
 * AbstractMyBatisDaoTestSupport.java - Nov 8, 2014 1:30:18 PM
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

package net.solarnetwork.central.dao.mybatis.test;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Supporting class for MyBatis tests.
 * 
 * @author matt
 * @version 2.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisDaoTestSupport extends AbstractCentralTransactionalTest {

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

}
