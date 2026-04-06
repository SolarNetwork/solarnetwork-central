/* ==================================================================
 * DeleteObjectDatumStreamAliasEntity.java - 28/03/2026 9:42:17 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType.AliasOnly;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType.OriginalOrAlias;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;

/**
 * Delete datum stream data matching an {@link ObjectDatumStreamAliasFilter}
 * filter.
 *
 * @author matt
 * @version 1.0
 */
public class DeleteObjectDatumStreamAliasEntity implements PreparedStatementCreator, SqlProvider {

	private final ObjectDatumStreamAliasMatchType matchType;
	private final ObjectDatumStreamAliasFilter filter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DeleteObjectDatumStreamAliasEntity(ObjectDatumStreamAliasFilter filter) {
		this(filter, false);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @param matchAliasOnly
	 *        {@code true} for stream/node/source criteria to match only alias
	 *        values, not original (destination) values
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DeleteObjectDatumStreamAliasEntity(ObjectDatumStreamAliasFilter filter,
			boolean matchAliasOnly) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.matchType = (matchAliasOnly ? AliasOnly : filter.streamAliasMatchType());

	}

	@Override
	public String getSql() {
		var buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		if ( filter.hasUserCriteria() || (matchType != AliasOnly && filter.hasStreamCriteria()) ) {
			buf.append("""
						WITH m AS (
							SELECT node_id, source_id, orig_stream_id
							FROM %s.da_datm_meta_aliased
					""".formatted(filter.hasUserCriteria() ? "solaruser" : "solardatm"));
			var where = new StringBuilder();
			whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
			whereOptimizedArrayContains(filter.getNodeIds(), "node_id", where);
			if ( !where.isEmpty() ) {
				buf.append("\tWHERE ").append(where.substring(4));
			}
			buf.append(")\n");
		}
		buf.append("""
				DELETE FROM solardatm.da_datm_alias da
				""");
		if ( filter.hasUserCriteria() || (matchType != AliasOnly && filter.hasStreamCriteria()) ) {
			buf.append("USING m\n");
		}
	}

	private int sqlWhereOr(int idx, Object @Nullable [] array, String col1, String col2,
			StringBuilder where) {
		if ( array == null ) {
			return idx;
		}
		StringBuilder or = new StringBuilder();
		idx += whereOptimizedArrayContains(array, col1, or);
		or.insert(5, "(\n\t");
		or.append("\tOR ");
		StringBuilder or2 = new StringBuilder();
		idx += whereOptimizedArrayContains(array, col2, or2);
		or.append(or2.substring(4));
		or.append(")\n");
		where.append(or);
		return idx;
	}

	private void sqlWhere(StringBuilder buf) {
		var where = new StringBuilder();
		if ( filter.hasUserCriteria() || (matchType != AliasOnly && filter.hasStreamCriteria()) ) {
			where.append("""
						AND da.node_id = m.node_id
						AND da.source_id = m.source_id
					""");
		}
		int idx = 0;
		if ( filter.hasStreamCriteria() ) {
			if ( matchType == OriginalOrAlias ) {
				idx += sqlWhereOr(idx, filter.getStreamIds(), "da.stream_id", "m.orig_stream_id", where);
			} else {
				idx += whereOptimizedArrayContains(filter.getStreamIds(),
						(matchType == AliasOnly ? "da.stream_id" : "m.orig_stream_id"), where);
			}
		}
		if ( filter.hasNodeCriteria() ) {
			if ( matchType == OriginalOrAlias ) {
				idx += sqlWhereOr(idx, filter.getNodeIds(), "da.alias_node_id", "da.node_id", where);
			} else {
				idx += whereOptimizedArrayContains(filter.getNodeIds(),
						(matchType == AliasOnly ? "da.alias_node_id" : "da.node_id"), where);
			}
		}
		if ( filter.hasSourceCriteria() ) {
			if ( matchType == OriginalOrAlias ) {
				idx += sqlWhereOr(idx, filter.getSourceIds(), "da.alias_source_id", "da.source_id",
						where);
			} else {
				idx += whereOptimizedArrayContains(filter.getSourceIds(),
						(matchType == AliasOnly ? "da.alias_source_id" : "da.source_id"), where);
			}
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
		}

		if ( filter.hasStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getStreamIds());
			if ( matchType == OriginalOrAlias ) {
				p = prepareOptimizedArrayParameter(con, stmt, p, filter.getStreamIds());
			}
		}
		if ( filter.hasNodeCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
			if ( matchType == OriginalOrAlias ) {
				p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
			}
		}
		if ( filter.hasSourceCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getSourceIds());
			if ( matchType == OriginalOrAlias ) {
				p = prepareOptimizedArrayParameter(con, stmt, p, filter.getSourceIds());
			}
		}
		return stmt;
	}

}
