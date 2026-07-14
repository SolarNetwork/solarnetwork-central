/* ==================================================================
 * SelectCloudDatumStreamConfiguration.java - 3/10/2024 1:23:36 pm
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Support for SELECT for {@link CloudDatumStreamConfiguration} entities.
 *
 * @author matt
 * @version 1.2
 */
public final class SelectCloudDatumStreamConfiguration
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final CloudDatumStreamFilter filter;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudDatumStreamConfiguration(CloudDatumStreamFilter filter) {
		this(filter, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 */
	public SelectCloudDatumStreamConfiguration(CloudDatumStreamFilter filter, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.fetchSize = fetchSize;
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

	private void sqlCore(StringBuilder buf) {
		if ( filter.hasSourceCriteria() ) {
			buf.append("""
					WITH sources AS (
						SELECT ?::text[] AS source_ids
					)
					""");
		}
		buf.append("""
				SELECT cds.user_id, cds.id, cds.created, cds.modified, cds.enabled
					, cds.cname, cds.sident
					, cds.map_id, cds.schedule, cds.kind, cds.obj_id, cds.source_id
					, cds.sprops
				FROM solardin.cin_datum_stream cds""");
		if ( filter.hasSourceCriteria() ) {
			buf.append(", sources");
		}
		buf.append('\n');
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "cds.user_id", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "cds.id", where);
		}
		if ( filter.hasDatumStreamMappingCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamMappingIds(), "cds.map_id", where);
		}
		if ( filter.hasNodeCriteria() ) {
			where.append("\tAND cds.kind = '").append(ObjectDatumKind.Node.getKey()).append("'\n");
			idx += whereOptimizedArrayContains(filter.getNodeIds(), "cds.obj_id", where);
		}
		if ( filter.hasSourceCriteria() ) {
			boolean pattern = false;
			for ( String sourceId : filter.sourceIds() ) {
				if ( DatumUtils.WILDCARD_PATTERN_MATCHER.isPattern(sourceId) ) {
					pattern = true;
					break;
				}
			}
			where.append("\tAND (\n");
			if ( pattern ) {
				where.append(
						"""
									cds.source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
									OR (
										jsonb_typeof(sprops->'sourceIdMap') = 'object'
										AND EXISTS (
											SELECT TRUE
											FROM jsonb_array_elements_text(jsonb_path_query_array(sprops->'sourceIdMap', '$.*')) AS m_source_id
											WHERE m_source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
										)
									)
									OR 	(
										jsonb_typeof(sprops->'virtualSourceIds') = 'array'
										AND jsonb_array_length(sprops->'virtualSourceIds') > 0
										AND EXISTS (
											SELECT TRUE
											FROM jsonb_array_elements_text(cds.sprops->'virtualSourceIds') AS v_source_id
											WHERE v_source_id ~ ANY(ARRAY(SELECT solarcommon.ant_pattern_to_regexp(unnest(sources.source_ids))))
										)
									)
								""");
			} else {
				where.append(
						"""
									cds.source_id = ANY(sources.source_ids)
									OR (
										jsonb_typeof(sprops->'sourceIdMap') = 'object'
										AND EXISTS (
											SELECT TRUE
											FROM jsonb_array_elements_text(jsonb_path_query_array(sprops->'sourceIdMap', '$.*')) AS m_source_id
											WHERE m_source_id = ANY(sources.source_ids)
										)
									)
									OR 	(
										jsonb_typeof(sprops->'virtualSourceIds') = 'array'
										AND jsonb_array_length(sprops->'virtualSourceIds') > 0
										AND EXISTS (
											SELECT TRUE
											FROM jsonb_array_elements_text(cds.sprops->'virtualSourceIds') AS v_source_id
											WHERE v_source_id = ANY(sources.source_ids)
										)
									)
								""");
			}
			where.append(")\n");
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY cds.user_id, cds.id");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		CommonSqlUtils.prepareLimitOffset(filter, stmt, p);
		if ( fetchSize > 0 ) {
			stmt.setFetchSize(fetchSize);
		}
		return stmt;
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.hasSourceCriteria() ) {
			p = prepareArrayParameter(con, stmt, p, filter.getSourceIds());
		}
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
		}
		if ( filter.hasDatumStreamMappingCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamMappingIds());
		}
		if ( filter.hasNodeCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getNodeIds());
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
			sqlWhere(buf);
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
