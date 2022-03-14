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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.SortCriteria;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Base DAO support for MyBatis implementations
 * 
 * @author matt
 * @version 1.3
 */
public abstract class BaseMyBatisDao extends SqlSessionDaoSupport {

	/** A RowBounds instance that returns at most the first row. */
	public static final RowBounds FIRST_ROW = new RowBounds(0, 1);

	/** The query property for a filter (search criteria) object. */
	public static final String FILTER_PROPERTY = "filter";

	/** The query property for any custom sort descriptors that are provided. */
	public static final String SORT_DESCRIPTORS_PROPERTY = "SortDescriptors";

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

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

	/**
	 * Execute a count query for a filter.
	 * 
	 * <p>
	 * If the query throws an {@link IllegalArgumentException} this method
	 * assumes that means the query name was not found, and will simply return
	 * {@literal null}.
	 * </p>
	 * 
	 * @param countQueryName
	 *        the query name
	 * @param sqlProps
	 *        the SQL properties
	 * @return the count
	 * @since 1.3
	 */
	protected Long executeCountQuery(final String countQueryName, final Map<String, ?> sqlProps) {
		try {
			return selectLong(countQueryName, sqlProps);
		} catch ( RuntimeException e ) {
			Throwable cause = e;
			while ( cause.getCause() != null ) {
				cause = cause.getCause();
			}
			if ( cause instanceof IllegalArgumentException ) {
				log.warn("Count query not supported: {}", countQueryName, e);
			} else {
				throw e;
			}
		}
		return null;
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * @param <M>
	 *        the match type
	 * @param <K>
	 *        the match key type
	 * @param <F>
	 *        the filter type
	 * @param query
	 *        the query name
	 * @param filter
	 *        the filter
	 * @return the results
	 * @since 1.3
	 * @see #selectFiltered(String, Object, List, Integer, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 */
	protected <M extends Identity<K>, K, F> FilterResults<M, K> selectFiltered(String query, F filter) {
		return selectFiltered(query, filter, null, null, null, null, null);
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * @param <M>
	 *        the match type
	 * @param <K>
	 *        the match key type
	 * @param <F>
	 *        the filter type
	 * @param query
	 *        the query name
	 * @param filter
	 *        the filter
	 * @param sorts
	 *        the sort descriptors
	 * @param offset
	 *        the starting result offset
	 * @param max
	 *        the maximum number of results
	 * @return the results
	 * @since 1.3
	 * @see #selectFiltered(String, Object, List, Integer, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 */
	protected <M extends Identity<K>, K, F> FilterResults<M, K> selectFiltered(String query, F filter,
			List<SortDescriptor> sorts, Integer offset, Integer max) {
		return selectFiltered(query, filter, sorts, offset, max, null, null);
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * @param <M>
	 *        the match type
	 * @param <K>
	 *        the match key type
	 * @param <F>
	 *        the filter type
	 * @param query
	 *        the query name
	 * @param filter
	 *        the filter
	 * @param sorts
	 *        the sort descriptors
	 * @param offset
	 *        the starting result offset
	 * @param max
	 *        the maximum number of results
	 * @param propertyProcessor
	 *        an optional property process to adjust the SQL properties
	 * @return the results
	 * @since 1.3
	 * @see #selectFiltered(String, Object, List, Integer, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 */
	protected <M extends Identity<K>, K, F> FilterResults<M, K> selectFiltered(String query, F filter,
			List<SortDescriptor> sorts, Integer offset, Integer max,
			BiConsumer<F, Map<String, Object>> propertyProcessor) {
		return selectFiltered(query, filter, sorts, offset, max, propertyProcessor, null);
	}

	/**
	 * Perform a filter search using standardized semantics.
	 * 
	 * <p>
	 * The following steps are taken:
	 * </p>
	 * 
	 * <ol>
	 * <li>Create a SQL parameters map with a
	 * {@link BaseMyBatisGenericDaoSupport#FILTER_PROPERTY} key and {@code f}
	 * value.</li>
	 * <li>If {@code filter} implements {@link SortCriteria} and provides sort
	 * descriptors, those will be set as the SQL parameter
	 * {@link BaseMyBatisGenericDaoSupport#SORT_DESCRIPTORS_PROPERTY}. Otherwise
	 * the {@code sorts} argument will be set.</li>
	 * <li>If {@code propertyProcessor} is not {@literal null} invoke that.</li>
	 * <li>If {@code filter} implements {@link PaginationCriteria} and provides
	 * pagination values, those will be used in preference to the {@code offset}
	 * and {@code max} method arguments.</li>
	 * <li>If {@code max} is not {@literal null}, and if filter implements
	 * {@link OptimizedQueryCriteria} and does not disable a total results
	 * count, then call {@link #executeCountQuery(String, Map)}</li>
	 * <li>Call {@link #selectList(String, Object, Integer, Integer)}</li>
	 * <li>Create a result object, or if {@code resultsFactory} provided then
	 * call
	 * {@link FilterResultsFactory#createFilterResults(Object, Map, Iterable, Long, Integer, Integer)}
	 * and return the result.</li>
	 * </ol>
	 * 
	 * @param <M>
	 *        the match type
	 * @param <K>
	 *        the match key type
	 * @param <F>
	 *        the filter type
	 * @param query
	 *        the query name
	 * @param filter
	 *        the filter
	 * @param sorts
	 *        the sort descriptors
	 * @param offset
	 *        the starting result offset
	 * @param max
	 *        the maximum number of results
	 * @param propertyProcessor
	 *        an optional property process to adjust the SQL properties
	 * @param resultsFactory
	 *        an optional factory for creating filter results objects
	 * @return the results
	 * @since 1.3
	 */
	protected <M extends Identity<K>, K, F> FilterResults<M, K> selectFiltered(final String query,
			F filter, List<SortDescriptor> sorts, Integer offset, Integer max,
			BiConsumer<F, Map<String, Object>> propertyProcessor,
			FilterResultsFactory<M, K, F> resultsFactory) {
		Map<String, Object> sqlProps = new HashMap<String, Object>(1);
		sqlProps.put(FILTER_PROPERTY, filter);

		// if filter is SortCriteria and provides sort values, use those over method args
		if ( filter instanceof SortCriteria && ((SortCriteria) filter).getSorts() != null
				&& !((SortCriteria) filter).getSorts().isEmpty() ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, ((SortCriteria) filter).getSorts());
		} else if ( sorts != null && sorts.size() > 0 ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sorts);
		}

		if ( propertyProcessor != null ) {
			propertyProcessor.accept(filter, sqlProps);
		}

		// if filter provides pagination values, don't use MyBatis pagination
		Integer m = max;
		Integer o = offset;
		if ( filter instanceof PaginationCriteria ) {
			final PaginationCriteria pagination = (PaginationCriteria) filter;
			if ( pagination.getMax() != null && pagination.getMax().intValue() > 0
					&& pagination.getOffset() != null && pagination.getOffset().intValue() >= 0 ) {
				max = pagination.getMax();
				m = null;
				offset = pagination.getOffset();
				o = null;
			}
		}

		// attempt count first, if max NOT null or specified as -1; if filter is instance of OptimizedQueryCriteria
		// check the withoutTotalResultsCount flag
		Long totalCount = null;
		if ( max != null && max.intValue() != -1 && !((filter instanceof OptimizedQueryCriteria)
				&& ((OptimizedQueryCriteria) filter).isWithoutTotalResultsCount()) ) {
			Long n = executeCountQuery(query + "-count", sqlProps);
			if ( n != null ) {
				totalCount = n.longValue();
			}
		}

		List<M> rows = selectList(query, sqlProps, o, m);

		if ( resultsFactory != null ) {
			return resultsFactory.createFilterResults(filter, sqlProps, rows, totalCount, offset,
					rows.size());
		}
		return new BasicFilterResults<M, K>(rows, totalCount, offset != null ? offset.intValue() : 0,
				rows.size());
	}

}
