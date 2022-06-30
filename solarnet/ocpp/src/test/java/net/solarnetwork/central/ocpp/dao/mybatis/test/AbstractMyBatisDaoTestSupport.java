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

import java.util.List;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ContextConfiguration;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;

/**
 * Base class for MyBatis DAO tests.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisDaoTestSupport extends AbstractCentralTransactionalTest {

	private SqlSessionFactory sqlSessionFactory;
	private SqlSessionTemplate sqlSessionTemplate;

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		if ( sqlSessionTemplate == null ) {
			sqlSessionTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
		}
		return sqlSessionTemplate;
	}

	@Autowired
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	/**
	 * Get the last update statement's update count result.
	 * 
	 * <p>
	 * This method can be used to automatically flush the MyBatis batch
	 * statements and return the last statement's update count.
	 * </p>
	 * 
	 * @param result
	 *        the result as returned by a {@code getSqlSession().update()} style
	 *        statement
	 * @return the update count, flushing batch statements as necessary
	 */
	protected int lastUpdateCount(int result) {
		if ( result < 0 ) {
			List<BatchResult> batchResults = getSqlSessionTemplate().flushStatements();
			if ( batchResults != null && !batchResults.isEmpty() ) {
				int[] counts = batchResults.get(batchResults.size() - 1).getUpdateCounts();
				result = counts[counts.length - 1];
			}
		}
		return result;
	}

}
