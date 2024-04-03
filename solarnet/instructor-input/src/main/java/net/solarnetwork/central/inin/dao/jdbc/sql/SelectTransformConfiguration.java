/* ==================================================================
 * SelectTransformConfiguration.java - 21/02/2024 1:41:43 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.inin.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.inin.dao.TransformFilter;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformPhase;

/**
 * Select {@link TransformConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class SelectTransformConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final TransformPhase phase;
	private final TransformFilter filter;
	private final int fetchSize;
	private final String alias;

	/**
	 * Constructor.
	 *
	 * @param phase
	 *        the phase
	 * @param filter
	 *        the filter
	 */
	public SelectTransformConfiguration(TransformPhase phase, TransformFilter filter) {
		this(phase, filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param phase
	 *        the phase
	 * @param filter
	 *        the filter
	 * @param fetchSize
	 *        the fetch size
	 */
	public SelectTransformConfiguration(TransformPhase phase, TransformFilter filter, int fetchSize) {
		super();
		this.phase = phase;
		this.filter = requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
		this.alias = (phase == TransformPhase.Request ? "iix" : "iox");

	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private void aliased(StringBuilder buf, String name) {
		buf.append(alias).append('.').append(name);
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT ");
		aliased(buf, "user_id");
		buf.append(", ");
		aliased(buf, "id");
		buf.append(", ");
		aliased(buf, "created");
		buf.append(", ");
		aliased(buf, "modified");
		buf.append(", ");
		aliased(buf, "cname");
		buf.append(", ");
		aliased(buf, "sident");
		buf.append(", ");
		aliased(buf, "sprops");
		buf.append("\nFROM solardin.inin_");
		buf.append(phase == TransformPhase.Request ? "req" : "res");
		buf.append("_xform ").append(alias).append('\n');

	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), alias + ".user_id", where);
		}
		if ( filter.hasTransformCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getTransformIds(), alias + ".id", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY ");
		aliased(buf, "user_id");
		buf.append(", ");
		aliased(buf, "id");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		CommonSqlUtils.prepareLimitOffset(filter, con, stmt, p);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasTransformCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getTransformIds());
		}
		return p;
	}

	@Override
	public PreparedStatementCreator countPreparedStatementCreator() {
		return new CountPreparedStatementCreator();
	}

	private final class CountPreparedStatementCreator implements PreparedStatementCreator, SqlProvider {

		@Override
		public String getSql() {
			StringBuilder buf = new StringBuilder();
			sqlCore(buf);
			return CommonSqlUtils.wrappedCountQuery(buf.toString());
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement stmt = con.prepareStatement(getSql());
			prepareCore(con, stmt, 0);
			return stmt;
		}

	}

}
