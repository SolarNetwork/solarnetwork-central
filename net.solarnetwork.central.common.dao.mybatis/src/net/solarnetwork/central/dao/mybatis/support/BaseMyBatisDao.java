/* ==================================================================
 * BaseMyBatisDao.java - Nov 10, 2014 1:02:32 PM
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

package net.solarnetwork.central.dao.mybatis.support;

import java.util.List;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;

/**
 * Base DAO support for MyBatis implementations
 * 
 * @author matt
 * @version 1.2
 */
public abstract class BaseMyBatisDao extends SqlSessionDaoSupport {

	/** A RowBounds instance that returns at most the first row. */
	public static final RowBounds FIRST_ROW = new RowBounds(0, 1);

	@Override
	public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
		if ( getSqlSession() == null ) {
			// create own SqlSession with own exception translator implementation
			setSqlSessionTemplate(new SqlSessionTemplate(sqlSessionFactory,
					sqlSessionFactory.getConfiguration().getDefaultExecutorType(),
					new MyBatisExceptionTranslator(
							sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(),
							true)));
		}
		super.setSqlSessionFactory(sqlSessionFactory);
	}

	/**
	 * Select the first available result from a query. This is similar to
	 * {@link SqlSession#selectOne(String, Object)} except that the
	 * {@link BaseMyBatisGenericDao#FIRST_ROW} bounds is passed to the database.
	 * 
	 * @param statement
	 *        the name of the SQL statement to execute
	 * @param parameters
	 *        any parameters to pass to the statement
	 * @param <E>
	 *        the result type
	 * @return the first result, or <em>null</em> if none matched the query
	 */
	protected final <E> E selectFirst(String statement, Object parameters) {
		List<E> results = getSqlSession().selectList(statement, parameters, FIRST_ROW);
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	/**
	 * Select a list with optional support for row bounds.
	 * 
	 * @param statement
	 *        the name of the SQL statement to execute
	 * @param parameters
	 *        any parameters to pass to the statement
	 * @param offset
	 *        a result offset, or <em>null</em> for no offset
	 * @param max
	 *        the maximum number of results, or <em>null</em> for no maximum
	 * @param <E>
	 *        the result type
	 * @return the first result, or <em>null</em> if none matched the query
	 */
	protected final <E> List<E> selectList(final String statement, Object parameters, Integer offset,
			Integer max) {
		List<E> rows = null;
		if ( max != null && max > 0 ) {
			rows = getSqlSession().selectList(statement, parameters, new RowBounds(
					(offset == null || offset.intValue() < 0 ? 0 : offset.intValue()), max));
		} else {
			rows = getSqlSession().selectList(statement, parameters);
		}
		return rows;
	}

	/**
	 * Execute a {@code SELECT} query that returns a single long value.
	 * 
	 * @param statement
	 *        the name of the SQL statement to execute
	 * @param parameters
	 *        any parameters to pass to the statement
	 * @return the result as a long, or {@literal null}
	 * @since 1.1
	 */
	protected Long selectLong(final String statement, final Object parameters) {
		Number n = getSqlSession().selectOne(statement, parameters);
		if ( n != null ) {
			return n.longValue();
		}
		return null;
	}

}
