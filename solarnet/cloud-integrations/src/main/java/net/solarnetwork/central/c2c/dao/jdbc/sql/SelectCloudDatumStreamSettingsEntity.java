/* ==================================================================
 * SelectCloudIntegrationConfiguration.java - 2/10/2024 8:46:14 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;

/**
 * Support for SELECT for {@link CloudDatumStreamSettingsEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class SelectCloudDatumStreamSettingsEntity
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/** The {@code fetchSize} property default value. */
	public static final int DEFAULT_FETCH_SIZE = 1000;

	private final CloudDatumStreamSettingsFilter filter;
	private final boolean resolveUserSettings;
	private final int fetchSize;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_FETCH_SIZE} will be used and
	 * {@code resolveUserSettings} will be {@code false}.
	 * </p>
	 *
	 * @param filter
	 *        the filter
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectCloudDatumStreamSettingsEntity(CloudDatumStreamSettingsFilter filter) {
		this(filter, false, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_FETCH_SIZE} will be used.
	 * </p>
	 *
	 * @param filter
	 *        the filter
	 * @param resolveUserSettings
	 *        {@code true} to resolve user settings as default values; only
	 *        honored if the {@code filter} also provides a user ID and datum
	 *        stream ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectCloudDatumStreamSettingsEntity(CloudDatumStreamSettingsFilter filter,
			boolean resolveUserSettings) {
		this(filter, resolveUserSettings, DEFAULT_FETCH_SIZE);
	}

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the filter
	 * @param resolveUserSettings
	 *        {@code true} to resolve user settings as default values; only
	 *        honored if the {@code filter} also provides a user ID and datum
	 *        stream ID
	 * @param fetchSize
	 *        the fetch size
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public SelectCloudDatumStreamSettingsEntity(CloudDatumStreamSettingsFilter filter,
			boolean resolveUserSettings, int fetchSize) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.resolveUserSettings = resolveUserSettings && filter.hasUserCriteria()
				&& filter.hasDatumStreamCriteria();
		this.fetchSize = fetchSize;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		if ( !resolveUserSettings ) {
			sqlOrderBy(buf);
		}
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		if ( resolveUserSettings ) {
			buf.append("""
					WITH cdss AS (
						SELECT user_id
							, ds_id
							, created
							, modified
							, pub_in
							, pub_flux
						FROM solardin.cin_datum_stream_settings
						WHERE user_id = ?
							AND ds_id = ?

						UNION ALL

						SELECT user_id
							, x'8000000000000000'::BIGINT AS ds_id
							, created
							, modified
							, pub_in
							, pub_flux
						FROM solardin.cin_user_settings
						WHERE user_id = ?
					)
					SELECT user_id
						, ds_id
						, created
						, modified
						, pub_in
						, pub_flux
					FROM cdss
					LIMIT 1
					""");
		} else {
			buf.append("""
					SELECT cdss.user_id
						, cdss.ds_id
						, cdss.created
						, cdss.modified
						, cdss.pub_in
						, cdss.pub_flux
					FROM solardin.cin_datum_stream_settings cdss
					""");
		}
	}

	private void sqlWhere(StringBuilder buf) {
		if ( resolveUserSettings ) {
			return;
		}
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "cdss.user_id", where);
		}
		if ( filter.hasDatumStreamCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getDatumStreamIds(), "cdss.ds_id", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY cdss.user_id, cdss.ds_id");
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
		if ( filter.hasDatumStreamCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getDatumStreamIds());
		}
		if ( resolveUserSettings ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
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
