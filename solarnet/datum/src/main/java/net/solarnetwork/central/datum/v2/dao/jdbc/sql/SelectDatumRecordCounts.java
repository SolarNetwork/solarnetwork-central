/* ==================================================================
 * SelectDatumRecordCounts.java - 5/12/2020 4:07:24 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.DatumRecordCounts;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link DatumRecordCounts} instances via a
 * {@link ObjectStreamCriteria} filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatumRecordCounts implements PreparedStatementCreator, SqlProvider {

	private final ObjectStreamCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null} or invalid
	 */
	public SelectDatumRecordCounts(ObjectStreamCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
		sqlCte(Aggregation.None, buf);
		sqlCte(Aggregation.Hour, buf);
		sqlCte(Aggregation.Day, buf);
		sqlCte(Aggregation.Month, buf);
	}

	private static char alias(Aggregation agg) {
		switch (agg) {
			case Hour:
				return 'h';

			case Day:
				return 'd';

			case Month:
				return 'm';

			default:
				return 'r';
		}
	}

	private static String colAlias(Aggregation agg) {
		switch (agg) {
			case Hour:
				return "datum_hourly_count";

			case Day:
				return "datum_daily_count";

			case Month:
				return "datum_monthly_count";

			default:
				return "datum_count";
		}
	}

	private static String table(Aggregation agg) {
		switch (agg) {
			case Hour:
				return "agg_datm_hourly";

			case Day:
				return "agg_datm_daily";

			case Month:
				return "agg_datm_monthly";

			default:
				return "da_datm";
		}
	}

	private void sqlCte(Aggregation agg, StringBuilder buf) {
		buf.append(", ").append(alias(agg)).append(" AS (\n");
		buf.append("\tSELECT COUNT(*) AS ").append(colAlias(agg)).append("\n");
		buf.append("\tFROM s\n");
		buf.append("\tINNER JOIN solardatm.").append(table(agg))
				.append(" datum ON datum.stream_id = s.stream_id\n");

		StringBuilder where = new StringBuilder();
		int idx = filter.hasLocalDate()
				? DatumSqlUtils.whereLocalDateRange(filter, agg,
						DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, null, RoundingMode.FLOOR, where)
				: DatumSqlUtils.whereDateRange(filter, agg, where);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
		buf.append(")\n");
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT NULL::UUID AS stream_id,\n");
		buf.append("	CURRENT_TIMESTAMP AS ts_start,\n");
		buf.append("	r.datum_count,\n");
		buf.append("	h.datum_hourly_count,\n");
		buf.append("	d.datum_daily_count,\n");
		buf.append("	m.datum_monthly_count\n");
		buf.append("FROM r, h, d, m");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCte(buf);
		sqlSelect(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		for ( int i = 0; i < 4; i++ ) {
			if ( filter.hasLocalDate() ) {
				p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, con, stmt, p);
			} else {
				p = DatumSqlUtils.prepareDateRangeFilter(filter, con, stmt, p);
			}
		}
		return stmt;
	}

}
