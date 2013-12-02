/* ==================================================================
 * SqlExecutor.java - Dec 3, 2013 10:55:31 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.ibatis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.ibatis.sqlmap.engine.impl.SqlMapClientImpl;
import com.ibatis.sqlmap.engine.impl.SqlMapExecutorDelegate;
import com.ibatis.sqlmap.engine.mapping.result.ResultMap;
import com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactoryUtil;
import com.ibatis.sqlmap.engine.mapping.statement.DefaultRowHandler;
import com.ibatis.sqlmap.engine.mapping.statement.MappedStatement;
import com.ibatis.sqlmap.engine.mapping.statement.RowHandlerCallback;
import com.ibatis.sqlmap.engine.scope.ErrorContext;
import com.ibatis.sqlmap.engine.scope.SessionScope;
import com.ibatis.sqlmap.engine.scope.StatementScope;

/**
 * Custom SqlExecutor that supports more efficient result set processing.
 * 
 * <p>
 * For SolarNetwork's use case of supporting filtered results, we must create
 * the PreparedStatement with both {@link ResultSet#TYPE_FORWARD_ONLY} (which
 * was configurable in stock iBATIS) and {@link ResultSet#CONCUR_READ_ONLY}
 * (which was not configurable in stock iBATIS) even if no maximum limit is set
 * on the results. In addition, we call
 * {@link PreparedStatement#setMaxRows(int)} if a limit is set, rather than rely
 * on filtering the results in the client.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SqlExecutor extends com.ibatis.sqlmap.engine.execution.SqlExecutor {

	@Override
	public void executeQuery(StatementScope statementScope, Connection conn, String sql,
			Object[] parameters, int skipResults, int maxResults, RowHandlerCallback callback)
			throws SQLException {
		ErrorContext errorContext = statementScope.getErrorContext();
		errorContext.setActivity("executing query");
		errorContext.setObjectId(sql);
		PreparedStatement ps = null;
		ResultSet rs = null;
		setupResultObjectFactory(statementScope);
		try {
			errorContext.setMoreInfo("Check the SQL Statement (preparation failed).");
			Integer rsType = statementScope.getStatement().getResultSetType();
			if ( rsType != null ) {
				ps = prepareStatement(statementScope.getSession(), conn, sql, rsType);
			} else {
				ps = prepareStatement(statementScope.getSession(), conn, sql);
			}
			setStatementTimeout(statementScope.getStatement(), ps);
			Integer fetchSize = statementScope.getStatement().getFetchSize();
			if ( fetchSize != null ) {
				ps.setFetchSize(fetchSize.intValue());
			}
			if ( maxResults > 0 ) {
				ps.setMaxRows(skipResults + maxResults);
			}
			errorContext.setMoreInfo("Check the parameters (set parameters failed).");
			statementScope.getParameterMap().setParameters(statementScope, ps, parameters);
			errorContext.setMoreInfo("Check the statement (query failed).");
			ps.execute();
			errorContext.setMoreInfo("Check the results (failed to retrieve results).");

			// Begin ResultSet Handling
			rs = handleMultipleResults(ps, statementScope, skipResults, maxResults, callback);
			// End ResultSet Handling
		} finally {
			try {
				closeResultSet(rs);
			} finally {
				closeStatement(statementScope.getSession(), ps);
			}
		}
	}

	private void setupResultObjectFactory(StatementScope statementScope) {
		SqlMapClientImpl client = (SqlMapClientImpl) statementScope.getSession().getSqlMapClient();
		ResultObjectFactoryUtil.setResultObjectFactory(client.getResultObjectFactory());
		ResultObjectFactoryUtil.setStatementId(statementScope.getStatement().getId());
	}

	private PreparedStatement prepareStatement(SessionScope sessionScope, Connection conn, String sql,
			Integer rsType) throws SQLException {
		SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor())
				.getDelegate();
		if ( sessionScope.hasPreparedStatementFor(sql) ) {
			return sessionScope.getPreparedStatement((sql));
		} else {
			PreparedStatement ps = conn.prepareStatement(sql, rsType.intValue(),
					ResultSet.CONCUR_READ_ONLY);
			sessionScope.putPreparedStatement(delegate, sql, ps);
			return ps;
		}
	}

	private static PreparedStatement prepareStatement(SessionScope sessionScope, Connection conn,
			String sql) throws SQLException {
		SqlMapExecutorDelegate delegate = ((SqlMapClientImpl) sessionScope.getSqlMapExecutor())
				.getDelegate();
		if ( sessionScope.hasPreparedStatementFor(sql) ) {
			return sessionScope.getPreparedStatement((sql));
		} else {
			PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_READ_ONLY);
			sessionScope.putPreparedStatement(delegate, sql, ps);
			return ps;
		}
	}

	private static void closeStatement(SessionScope sessionScope, PreparedStatement ps) {
		if ( ps != null ) {
			if ( !sessionScope.hasPreparedStatement(ps) ) {
				try {
					ps.close();
				} catch ( SQLException e ) {
					// ignore
				}
			}
		}
	}

	private static void closeResultSet(ResultSet rs) {
		if ( rs != null ) {
			try {
				rs.close();
			} catch ( SQLException e ) {
				// ignore
			}
		}
	}

	private static void setStatementTimeout(MappedStatement mappedStatement, Statement statement)
			throws SQLException {
		if ( mappedStatement.getTimeout() != null ) {
			statement.setQueryTimeout(mappedStatement.getTimeout().intValue());
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ResultSet handleMultipleResults(PreparedStatement ps, StatementScope statementScope,
			int skipResults, int maxResults, RowHandlerCallback callback) throws SQLException {
		ResultSet rs;
		rs = getFirstResultSet(statementScope, ps);
		if ( rs != null ) {
			handleResults(statementScope, rs, skipResults, maxResults, callback);
		}

		// Multiple ResultSet handling
		if ( callback.getRowHandler() instanceof DefaultRowHandler ) {
			MappedStatement statement = statementScope.getStatement();
			DefaultRowHandler defaultRowHandler = ((DefaultRowHandler) callback.getRowHandler());
			if ( statement.hasMultipleResultMaps() ) {
				List multipleResults = new ArrayList();
				multipleResults.add(defaultRowHandler.getList());
				ResultMap[] resultMaps = statement.getAdditionalResultMaps();
				int i = 0;
				while ( moveToNextResultsSafely(statementScope, ps) ) {
					if ( i >= resultMaps.length )
						break;
					ResultMap rm = resultMaps[i];
					statementScope.setResultMap(rm);
					rs = ps.getResultSet();
					DefaultRowHandler rh = new DefaultRowHandler();
					handleResults(statementScope, rs, skipResults, maxResults, new RowHandlerCallback(
							rm, null, rh));
					multipleResults.add(rh.getList());
					i++;
				}
				defaultRowHandler.setList(multipleResults);
				statementScope.setResultMap(statement.getResultMap());
			} else {
				while ( moveToNextResultsSafely(statementScope, ps) )
					;
			}
		}
		// End additional ResultSet handling
		return rs;
	}

	private ResultSet getFirstResultSet(StatementScope scope, Statement stmt) throws SQLException {
		ResultSet rs = null;
		boolean hasMoreResults = true;
		while ( hasMoreResults ) {
			rs = stmt.getResultSet();
			if ( rs != null ) {
				break;
			}
			hasMoreResults = moveToNextResultsIfPresent(scope, stmt);
		}
		return rs;
	}

	private boolean moveToNextResultsIfPresent(StatementScope scope, Statement stmt) throws SQLException {
		boolean moreResults;
		// This is the messed up JDBC approach for determining if there are more results
		moreResults = !(((moveToNextResultsSafely(scope, stmt) == false) && (stmt.getUpdateCount() == -1)));
		return moreResults;
	}

	private boolean moveToNextResultsSafely(StatementScope scope, Statement stmt) throws SQLException {
		if ( forceMultipleResultSetSupport(scope)
				|| stmt.getConnection().getMetaData().supportsMultipleResultSets() ) {
			return stmt.getMoreResults();
		}
		return false;
	}

	private boolean forceMultipleResultSetSupport(StatementScope scope) {
		return ((SqlMapClientImpl) scope.getSession().getSqlMapClient()).getDelegate()
				.isForceMultipleResultSetSupport();
	}

	private void handleResults(StatementScope statementScope, ResultSet rs, int skipResults,
			int maxResults, RowHandlerCallback callback) throws SQLException {
		try {
			statementScope.setResultSet(rs);
			ResultMap resultMap = statementScope.getResultMap();
			if ( resultMap != null ) {
				// Skip Results
				if ( rs.getType() != ResultSet.TYPE_FORWARD_ONLY ) {
					if ( skipResults > 0 ) {
						rs.absolute(skipResults);
					}
				} else {
					for ( int i = 0; i < skipResults; i++ ) {
						if ( !rs.next() ) {
							return;
						}
					}
				}

				// Get Results
				int resultsFetched = 0;
				while ( (maxResults == SqlExecutor.NO_MAXIMUM_RESULTS || resultsFetched < maxResults)
						&& rs.next() ) {
					Object[] columnValues = resultMap.resolveSubMap(statementScope, rs).getResults(
							statementScope, rs);
					callback.handleResultObject(statementScope, columnValues, rs);
					resultsFetched++;
				}
			}
		} finally {
			statementScope.setResultSet(null);
		}
	}
}
