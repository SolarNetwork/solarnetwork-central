/* ==================================================================
 * AbstractMyBatisDaoTestSupport.java - 25/02/2020 7:50:22 am
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

package net.solarnetwork.central.ocpp.dao.mybatis.test;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

/**
 * Base class for MyBatis DAO tests.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public abstract class AbstractMyBatisDaoTestSupport extends AbstractCentralTransactionalTest {

	private SqlSessionFactory sqlSessionFactory;

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	protected void setupTestUser(Long userId) {
		jdbcTemplate.update(
				"insert into solaruser.user_user (id, disp_name, email, password) values (?,?,?,?)",
				userId, "Test User " + userId, "test" + userId + "@localhost", "password-" + userId);
	}

	protected void setupTestUserNode(Long userId, Long nodeId) {
		jdbcTemplate.update("insert into solaruser.user_node (user_id, node_id) values (?,?)", userId,
				nodeId);
	}

}
