/* ==================================================================
 * SelectAuditDatum.java - 20/11/2020 10:15:11 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.central.datum.v2.dao.jdbc.sql.DatumSqlUtils.orderBySorts;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link AuditDatum} instances via a {@link AuditDatumCriteria}
 * filter.
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class SelectAuditDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	protected final AuditDatumCriteria filter;
	protected final Aggregation aggregation;

	private static Aggregation aggregation(AuditDatumCriteria filter) {
		// limit aggregation to specific supported ones
		Aggregation aggregation = Aggregation.Day;
		if ( filter != null && filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Hour:
				case Day:
				case Month:
					aggregation = filter.getAggregation();
					break;

				default:
					// ignore all others
			}
		}
		return aggregation;
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectAuditDatum(AuditDatumCriteria filter) {
		this(filter, aggregation(filter));
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param aggregation
	 *        the aggregation
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	protected SelectAuditDatum(AuditDatumCriteria filter, Aggregation aggregation) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.aggregation = aggregation;
	}

	/**
	 * Generate SQL SELECT fields for primary key fields (time, node, source).
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlSelectPk(StringBuilder buf) {
		if ( !filter.hasDatumRollupCriteria() || filter.hasDatumRollupType(DatumRollupType.Time) ) {
			buf.append("datum.ts_start AS aud_ts,\n");
		} else {
			buf.append("NULL::timestamptz AS aud_ts,\n");
		}
		if ( !filter.hasDatumRollupCriteria() || filter.hasDatumRollupType(DatumRollupType.Node) ) {
			buf.append("s.node_id AS aud_node_id,\n");
		} else {
			buf.append("NULL::bigint AS aud_node_id,\n");
		}
		if ( !filter.hasDatumRollupCriteria() || filter.hasDatumRollupType(DatumRollupType.Source) ) {
			buf.append("s.source_id AS aud_source_id,\n");
		} else {
			buf.append("NULL::text AS aud_source_id,\n");
		}
	}

	private void sqlSelectHour(StringBuilder buf) {
		buf.append("'Hour' AS aud_agg_kind,\n");
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(datum.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(datum.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(datum.prop_u_count) AS aud_datum_prop_update_count,\n");
			buf.append("SUM(datum.datum_q_count) AS aud_datum_query_count,\n");
		} else {
			buf.append("datum.datum_count AS aud_datum_count,\n");
			buf.append("datum.prop_count AS aud_datum_prop_count,\n");
			buf.append("datum.prop_u_count AS aud_datum_prop_update_count,\n");
			buf.append("datum.datum_q_count AS aud_datum_query_count,\n");
		}
		buf.append("NULL::bigint AS aud_datum_hourly_count,\n");
		buf.append("NULL::bigint AS aud_datum_daily_count,\n");
		buf.append("NULL::bigint AS aud_datum_monthly_count\n");
	}

	/**
	 * Generate SQL SELECT fields for day aggregation.
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlSelectDay(StringBuilder buf) {
		buf.append("'Day' AS aud_agg_kind,\n");
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(datum.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(datum.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(datum.prop_u_count) AS aud_datum_prop_update_count,\n");
			buf.append("SUM(datum.datum_q_count) AS aud_datum_query_count,\n");
			buf.append("SUM(datum.datum_hourly_count) AS aud_datum_hourly_count,\n");
			buf.append(
					"SUM(CASE datum.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END) AS aud_datum_daily_count,\n");
		} else {
			buf.append("datum.datum_count AS aud_datum_count,\n");
			buf.append("datum.prop_count AS aud_datum_prop_count,\n");
			buf.append("datum.prop_u_count AS aud_datum_prop_update_count,\n");
			buf.append("datum.datum_q_count AS aud_datum_query_count,\n");
			buf.append("datum.datum_hourly_count AS aud_datum_hourly_count,\n");
			buf.append(
					"CASE datum.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_daily_count,\n");
		}
		buf.append("NULL::bigint AS aud_datum_monthly_count\n");
	}

	private void sqlSelectMonth(StringBuilder buf) {
		buf.append("'Month' AS aud_agg_kind,\n");
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(datum.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(datum.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(datum.prop_u_count) AS aud_datum_prop_update_count,\n");
			buf.append("SUM(datum.datum_q_count) AS aud_datum_query_count,\n");
			buf.append("SUM(datum.datum_hourly_count) AS aud_datum_hourly_count,\n");
			buf.append("SUM(datum.datum_daily_count) AS aud_datum_daily_count,\n");
			buf.append(
					"SUM(CASE datum.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END) AS aud_datum_monthly_count\n");
		} else {
			buf.append("datum.datum_count AS aud_datum_count,\n");
			buf.append("datum.prop_count AS aud_datum_prop_count,\n");
			buf.append("datum.prop_u_count AS aud_datum_prop_update_count,\n");
			buf.append("datum.datum_q_count AS aud_datum_query_count,\n");
			buf.append("datum.datum_hourly_count AS aud_datum_hourly_count,\n");
			buf.append("datum.datum_daily_count AS aud_datum_daily_count,\n");
			buf.append(
					"CASE datum.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_monthly_count\n");
		}
	}

	/**
	 * Generate the full SQL statement, without any specific output ordering.
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlCore(StringBuilder buf) {
		sqlCte(buf);
		buf.append("SELECT ");
		sqlSelectPk(buf);
		if ( aggregation == Aggregation.Hour ) {
			sqlSelectHour(buf);
		} else if ( aggregation == Aggregation.Month ) {
			sqlSelectMonth(buf);
		} else {
			sqlSelectDay(buf);
		}
		sqlFrom(buf);
		sqlWhere(buf);
		sqlRollupGroup(buf);
	}

	/**
	 * Generate the SQL common table expression.
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
	}

	/**
	 * Get the SQL table name to query for audit data.
	 * 
	 * @return the table name
	 */
	protected String auditTableName() {
		if ( aggregation == Aggregation.Hour ) {
			return "solardatm.aud_datm_io";
		} else if ( aggregation == Aggregation.Month ) {
			return "solardatm.aud_datm_monthly";
		} else {
			return "solardatm.aud_datm_daily";
		}
	}

	/**
	 * Generate the SQL {@literal FROM} clause.
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlFrom(StringBuilder buf) {
		buf.append("FROM s\n");
		buf.append("INNER JOIN ").append(auditTableName())
				.append(" datum ON datum.stream_id = s.stream_id\n");
	}

	/**
	 * Generate the SQL {@literal WHERE} clause.
	 * 
	 * @param buf
	 *        the buffer to append the SQL to
	 */
	protected void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = filter.hasLocalDateRange()
				? DatumSqlUtils.whereLocalDateRange(filter, aggregation,
						DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where)
				: DatumSqlUtils.whereDateRange(filter, aggregation, where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlRollupGroup(StringBuilder buf) {
		if ( filter.hasDatumRollupCriteria() && !filter.hasDatumRollupType(DatumRollupType.All) ) {
			StringBuilder group = new StringBuilder();
			for ( DatumRollupType t : filter.getDatumRollupTypes() ) {
				switch (t) {
					case Time:
						group.append(", datum.ts_start");
						break;

					case Node:
						group.append(", s.node_id");
						break;

					case Source:
						group.append(", s.source_id");
						break;

					default:
						// ignore
				}
			}
			if ( group.length() > 0 ) {
				buf.append("GROUP BY ").append(group.substring(2)).append("\n");
			}
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( !filter.hasDatumRollupCriteria() || !filter.hasDatumRollupType(DatumRollupType.All) ) {
			StringBuilder order = new StringBuilder();
			int idx = 2;
			if ( filter.isMostRecent() ) {
				order.append(", aud_node_id, aud_source_id, aud_ts DESC");
			} else if ( filter.hasSorts() ) {
				idx = orderBySorts(filter.getSorts(), DatumSqlUtils.AUDIT_DATUM_SORT_KEY_MAPPING, order);
			} else {
				if ( !filter.hasDatumRollupCriteria()
						|| filter.hasDatumRollupType(DatumRollupType.Time) ) {
					order.append(", aud_ts");
				}
				if ( !filter.hasDatumRollupCriteria()
						|| filter.hasDatumRollupType(DatumRollupType.Node) ) {
					order.append(", aud_node_id");
				}
				if ( !filter.hasDatumRollupCriteria()
						|| filter.hasDatumRollupType(DatumRollupType.Source) ) {
					order.append(", aud_source_id");
				}
			}
			if ( order.length() > 0 ) {
				buf.append("ORDER BY ").append(order.substring(idx));
			}
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		}
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		int p = prepareCore(con, stmt, 0);
		DatumSqlUtils.preparePaginationFilter(filter, con, stmt, p);
		return stmt;
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
			return DatumSqlUtils.wrappedCountQuery(buf.toString());
		}

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement stmt = con.prepareStatement(getSql());
			prepareCore(con, stmt, 0);
			return stmt;
		}

	}

}
