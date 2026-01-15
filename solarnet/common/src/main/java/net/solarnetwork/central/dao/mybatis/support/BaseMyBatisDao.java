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

import java.io.Serializable;
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
import net.solarnetwork.dao.*;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.Unique;

/**
 * Base DAO support for MyBatis implementations
 *
 * @author matt
 * @version 2.0
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
	 * @return the first result, or {@code null} if none matched the query
	 */
	@SuppressWarnings("TypeParameterUnusedInFormals")
	protected final <E> E selectFirst(String statement, Object parameters) {
		List<E> results = getSqlSession().selectList(statement, parameters, FIRST_ROW);
		if ( !results.isEmpty() ) {
			return results.getFirst();
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
	 *        a result offset, or {@code null} for no offset
	 * @param max
	 *        the maximum number of results, or {@code null} for no maximum
	 * @param <E>
	 *        the result type
	 * @return the first result, or {@code null} if none matched the query
	 */
	protected final <E> List<E> selectList(final String statement, Object parameters, Long offset,
			Integer max) {
		List<E> rows;
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
	 * @see #selectFiltered(String, Object, List, Long, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 * @since 1.3
	 */
	protected <M extends Unique<K>, K extends Comparable<K> & Serializable, F> FilterResults<M, K> selectFiltered(
			String query, F filter) {
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
	 * @see #selectFiltered(String, Object, List, Long, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 * @since 1.3
	 */
	protected <M extends Unique<K>, K extends Comparable<K> & Serializable, F> FilterResults<M, K> selectFiltered(
			String query, F filter, List<SortDescriptor> sorts, Long offset, Integer max) {
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
	 * @see #selectFiltered(String, Object, List, Long, Integer, BiConsumer,
	 *      FilterResultsFactory)
	 * @since 1.3
	 */
	protected <M extends Unique<K>, K extends Comparable<K> & Serializable, F> FilterResults<M, K> selectFiltered(
			String query, F filter, List<SortDescriptor> sorts, Long offset, Integer max,
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
	 * {@link BaseMyBatisGenericDaoSupport#SORT_DESCRIPTORS_PROPERTY}.
	 * Otherwise, the {@code sorts} argument will be set.</li>
	 * <li>If {@code propertyProcessor} is not {@literal null} invoke that.</li>
	 * <li>If {@code filter} implements {@link PaginationCriteria} and provides
	 * pagination values, those will be used in preference to the {@code offset}
	 * and {@code max} method arguments.</li>
	 * <li>If {@code max} is not {@literal null}, and if filter implements
	 * {@link OptimizedQueryCriteria} and does not disable a total results
	 * count, then call {@link #executeCountQuery(String, Map)}</li>
	 * <li>Call {@link #selectList(String, Object, Long, Integer)}</li>
	 * <li>Create a result object, or if {@code resultsFactory} provided then
	 * call
	 * {@link FilterResultsFactory#createFilterResults(Object, Map, Iterable, Long, Long, Integer)}
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
	protected <M extends Unique<K>, K extends Comparable<K> & Serializable, F> FilterResults<M, K> selectFiltered(
			final String query, F filter, List<SortDescriptor> sorts, Long offset, Integer max,
			BiConsumer<F, Map<String, Object>> propertyProcessor,
			FilterResultsFactory<M, K, F> resultsFactory) {
		Map<String, Object> sqlProps = new HashMap<>(1);
		sqlProps.put(FILTER_PROPERTY, filter);

		// if filter is SortCriteria and provides sort values, use those over method args
		if ( filter instanceof SortCriteria s && s.getSorts() != null && !s.getSorts().isEmpty() ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, s.getSorts());
		} else if ( sorts != null && !sorts.isEmpty() ) {
			sqlProps.put(SORT_DESCRIPTORS_PROPERTY, sorts);
		}

		if ( propertyProcessor != null ) {
			propertyProcessor.accept(filter, sqlProps);
		}

		// if filter provides pagination values, don't use MyBatis pagination
		Integer m = max;
		Long o = offset;
		if ( filter instanceof PaginationCriteria pagination ) {
			if ( pagination.getMax() != null && pagination.getMax() > 0 && pagination.getOffset() != null
					&& pagination.getOffset() >= 0 ) {
				max = pagination.getMax();
				m = null;
				offset = pagination.getOffset();
				o = null;
			}
		}

		// attempt count first, if max NOT null or specified as -1; if filter is instance of OptimizedQueryCriteria
		// check the withoutTotalResultsCount flag
		Long totalCount = null;
		if ( max != null && max != -1 && !((filter instanceof OptimizedQueryCriteria oqc)
				&& oqc.isWithoutTotalResultsCount()) ) {
			Long n = executeCountQuery(query + "-count", sqlProps);
			if ( n != null ) {
				totalCount = n;
			}
		}

		List<M> rows = selectList(query, sqlProps, o, m);

		if ( resultsFactory != null ) {
			return resultsFactory.createFilterResults(filter, sqlProps, rows, totalCount, offset,
					rows.size());
		}
		return new BasicFilterResults<>(rows, totalCount, offset != null ? offset : 0, rows.size());
	}

}
