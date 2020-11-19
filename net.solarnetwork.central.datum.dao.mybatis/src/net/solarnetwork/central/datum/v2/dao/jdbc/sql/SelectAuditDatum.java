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

import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils.orderBySorts;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.domain.DatumRollupType;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumSqlUtils;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link AuditDatum} instances via a {@link AuditDatumCriteria}
 * filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectAuditDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	private final AuditDatumCriteria filter;
	private final Aggregation aggregation;

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
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument not be null.");
		}
		this.filter = filter;
		this.aggregation = aggregation(filter);
	}

	private void sqlSelectPk(StringBuilder buf) {
		if ( !filter.hasDatumMetadataCriteria() || filter.hasDatumRollupType(DatumRollupType.Time) ) {
			buf.append("aud.ts_start AS aud_ts,\n");
		} else {
			buf.append("NULL::timestamptz AS aud_ts,\n");
		}
		if ( !filter.hasDatumMetadataCriteria() || filter.hasDatumRollupType(DatumRollupType.Node) ) {
			buf.append("meta.node_id AS aud_node_id,\n");
		} else {
			buf.append("NULL::bigint AS aud_node_id,\n");
		}
		if ( !filter.hasDatumMetadataCriteria() || filter.hasDatumRollupType(DatumRollupType.Source) ) {
			buf.append("meta.source_id AS aud_source_id,\n");
		} else {
			buf.append("NULL::text AS aud_source_id,\n");
		}
	}

	private void sqlSelectHour(StringBuilder buf) {
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(aud.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(aud.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(aud.datum_q_count) AS aud_datum_query_count,\n");
		} else {
			buf.append("aud.datum_count AS aud_datum_count,\n");
			buf.append("aud.prop_count AS aud_datum_prop_count,\n");
			buf.append("aud.datum_q_count AS aud_datum_query_count,\n");
		}
		buf.append("'Hour' AS aud_agg_kind,\n");
		buf.append("NULL::bigint AS aud_datum_hourly_count,\n");
		buf.append("NULL::bigint AS aud_datum_daily_count,\n");
		buf.append("NULL::bigint AS aud_datum_monthly_count");
	}

	private void sqlSelectDay(StringBuilder buf) {
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(aud.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(aud.datum_hourly_count) AS aud_datum_hourly_count,\n");
			buf.append(
					"SUM(CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END) AS aud_datum_daily_count,\n");
			buf.append("SUM(aud.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(aud.datum_q_count) AS aud_datum_query_count,\n");
		} else {
			buf.append("aud.datum_count AS aud_datum_count,\n");
			buf.append("aud.datum_hourly_count AS aud_datum_hourly_count,\n");
			buf.append(
					"CASE aud.datum_daily_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_daily_count,\n");
			buf.append("aud.prop_count AS aud_datum_prop_count,\n");
			buf.append("aud.datum_q_count AS aud_datum_query_count,\n");
		}
		buf.append("'Day' AS aud_agg_kind,\n");
		buf.append("NULL::bigint AS aud_datum_monthly_count\n");
	}

	private void sqlSelectMonth(StringBuilder buf) {
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(aud.datum_count) AS aud_datum_count,\n");
			buf.append("SUM(aud.datum_hourly_count) AS aud_datum_hourly_count,\n");
			buf.append("SUM(aud.datum_daily_count) AS aud_datum_daily_count,\n");
			buf.append(
					"SUM(CASE aud.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END) AS aud_datum_monthly_count,\n");
			buf.append("SUM(aud.prop_count) AS aud_datum_prop_count,\n");
			buf.append("SUM(aud.datum_q_count) AS aud_datum_query_count,\n");
		} else {
			buf.append("aud.datum_count AS aud_datum_count,\n");
			buf.append("aud.datum_hourly_count AS aud_datum_hourly_count,\n");
			buf.append("aud.datum_daily_count AS aud_datum_daily_count,\n");
			buf.append(
					"CASE aud.datum_monthly_pres WHEN TRUE THEN 1 ELSE 0 END AS aud_datum_monthly_count,\n");
			buf.append("aud.prop_count AS aud_datum_prop_count,\n");
			buf.append("aud.datum_q_count AS aud_datum_query_count,\n");
		}
		buf.append("'Month' AS aud_agg_kind\n");
	}

	/*-
	<include refid="fragment-findall-AuditDatumEntity-rollup-group"/>
	<include refid="fragment-findall-AuditDatumEntity-order"/>
	*/
	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT ");
		sqlSelectPk(buf);
		if ( aggregation == Aggregation.Hour ) {
			sqlSelectHour(buf);
		} else if ( aggregation == Aggregation.Month ) {
			sqlSelectMonth(buf);
		} else {
			sqlSelectDay(buf);
		}
		buf.append("\nFROM ");
		if ( aggregation == Aggregation.Hour ) {
			buf.append("solardatm.aud_datm_hourly aud\n");
		} else if ( aggregation == Aggregation.Month ) {
			buf.append("solardatm.aud_datm_monthly aud\n");
		} else {
			buf.append("solardatm.aud_datm_daily aud\n");
		}
		buf.append("INNER JOIN solardatm.da_datm_meta meta ON meta.stream_id = aud.stream_id\n");
		if ( filter.hasUserCriteria() ) {
			buf.append("INNER JOIN solaruser.user_node un ON un.node_id = meta.node_id\n");
		}
		sqlWhere(buf);
		sqlRollupGroup(buf);
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = DatumSqlUtils.whereDatumMetadata(filter, where);
		idx |= DatumSqlUtils.whereDateRange(filter, aggregation, where);
		if ( idx > 0 ) {
			buf.append("WHERE ").append(where.substring(4));
		}
	}

	private void sqlRollupGroup(StringBuilder buf) {
		if ( filter.hasDatumRollupCriteria() && !filter.hasDatumRollupType(DatumRollupType.All) ) {
			StringBuilder group = new StringBuilder();
			for ( DatumRollupType t : filter.getDatumRollupTypes() ) {
				switch (t) {
					case Time:
						group.append(", aud.ts_start");
						break;

					case Node:
						group.append(", meta.node_id");
						break;

					case Source:
						group.append(", meta.source_id");
						break;

					default:
						// ignore
				}
			}
			if ( group.length() > 0 ) {
				buf.append("GROUP BY ").append(group.substring(2));
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

	private void sqlLimit(StringBuilder buf) {
		if ( filter != null && filter.getMax() != null ) {
			int max = filter.getMax();
			if ( max > 0 ) {
				buf.append("\nLIMIT ? OFFSET ?");
			}
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		sqlLimit(buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
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
