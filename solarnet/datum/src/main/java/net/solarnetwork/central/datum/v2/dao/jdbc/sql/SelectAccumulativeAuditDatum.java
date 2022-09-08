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

import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.CountPreparedStatementCreatorProvider;
import net.solarnetwork.central.datum.v2.dao.AuditDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.AuditDatum;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Select for {@link AuditDatum} instances via a {@link AuditDatumCriteria}
 * filter.
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public class SelectAccumulativeAuditDatum extends SelectAuditDatum
		implements PreparedStatementCreator, SqlProvider, CountPreparedStatementCreatorProvider {

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectAccumulativeAuditDatum(AuditDatumCriteria filter) {
		super(filter, Aggregation.RunningTotal);
	}

	@Override
	protected void sqlSelectPk(StringBuilder buf) {
		if ( filter.isMostRecent() ) {
			buf.append("datum.ts_start AS aud_ts,\n");
			buf.append("r.node_id AS aud_node_id,\n");
			buf.append("r.source_id AS aud_source_id,\n");
		} else {
			super.sqlSelectPk(buf);
		}
	}

	@Override
	protected void sqlSelectDay(StringBuilder buf) {
		buf.append("'RunningTotal' AS aud_agg_kind,\n");
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(datum.datum_count) AS aud_datum_count,\n");
		} else {
			buf.append("datum.datum_count AS aud_datum_count,\n");
		}
		buf.append("NULL::bigint AS aud_datum_prop_count,\n");
		buf.append("NULL::bigint AS aud_datum_prop_update_count,\n");
		buf.append("NULL::bigint AS aud_datum_query_count,\n");
		if ( filter.hasDatumRollupCriteria() ) {
			buf.append("SUM(datum.datum_hourly_count) AS aud_datum_hourly_count,\n");
			buf.append("SUM(datum.datum_daily_count) AS aud_datum_daily_count,\n");
			buf.append("SUM(datum.datum_monthly_count) AS aud_datum_monthly_count\n");
		} else {
			buf.append("datum.datum_hourly_count AS aud_datum_hourly_count,\n");
			buf.append("datum.datum_daily_count AS aud_datum_daily_count,\n");
			buf.append("datum.datum_monthly_count AS aud_datum_monthly_count\n");
		}
	}

	@Override
	protected void sqlCte(StringBuilder buf) {
		super.sqlCte(buf);
		if ( filter.isMostRecent() ) {
			buf.append(", r AS (\n");
			buf.append("SELECT datum.stream_id, datum.ts_start, s.node_id, s.source_id\n");
			buf.append("FROM s\n");
			buf.append("INNER JOIN LATERAL (\n");
			buf.append("SELECT datum.stream_id, datum.ts_start\n");
			buf.append("FROM solardatm.aud_acc_datm_daily datum\n");
			buf.append("WHERE datum.stream_id = s.stream_id\n");
			buf.append("ORDER BY datum.stream_id, datum.ts_start DESC\n");
			buf.append("LIMIT 1\n");
			buf.append(") datum ON datum.stream_id = s.stream_id\n");
			buf.append(")\n");
		}
	}

	@Override
	protected String auditTableName() {
		return "solardatm.aud_acc_datm_daily";
	}

	@Override
	protected void sqlFrom(StringBuilder buf) {
		if ( filter.isMostRecent() ) {
			buf.append("FROM r\n");
			buf.append("INNER JOIN ").append(auditTableName())
					.append(" datum ON datum.stream_id = r.stream_id AND datum.ts_start = r.ts_start\n");
		} else {
			super.sqlFrom(buf);
		}
	}

}
