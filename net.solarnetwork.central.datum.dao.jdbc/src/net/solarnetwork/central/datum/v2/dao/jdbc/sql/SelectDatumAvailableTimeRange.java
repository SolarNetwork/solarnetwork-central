/* ==================================================================
 * SelectDatumAvailableTimeRange.java - 29/11/2020 8:31:23 pm
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.AggregationCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select time ranges for datum streams.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectDatumAvailableTimeRange implements PreparedStatementCreator, SqlProvider {

	private final ObjectStreamCriteria filter;
	private final Aggregation aggregation;
	private final ObjectDatumKind kind;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectDatumAvailableTimeRange(ObjectStreamCriteria filter) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		this.filter = filter;
		this.aggregation = aggregation(filter);
		this.kind = filter.getObjectKind() != null ? filter.getObjectKind() : ObjectDatumKind.Node;
	}

	private static Aggregation aggregation(AggregationCriteria filter) {
		Aggregation agg = Aggregation.None;
		if ( filter.getAggregation() != null ) {
			switch (filter.getAggregation()) {
				case Hour:
				case Day:
				case Month:
					agg = filter.getAggregation();
					break;

				default:
					// ignore
			}
		}
		return agg;
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		if ( kind == ObjectDatumKind.Location ) {
			DatumSqlUtils.locationMetadataFilterSql(filter, DatumSqlUtils.MetadataSelectStyle.WithZone,
					buf);
		} else {
			DatumSqlUtils.nodeMetadataFilterSql(filter, DatumSqlUtils.MetadataSelectStyle.WithZone, buf);
		}
		buf.append(")\n");
	}

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT ");
		sqlColumns(buf);
	}

	private void sqlColumns(StringBuilder buf) {
		buf.append("s.stream_id,\n");
		if ( aggregation == Aggregation.None ) {
			buf.append("early.ts AS ts_start,\n");
			buf.append("late.ts AS ts_end,\n");
		} else {
			buf.append("early.ts_start AS ts_start,\n");
			buf.append("late.ts_start AS ts_end,\n");
		}
		if ( kind == ObjectDatumKind.Location ) {
			buf.append("s.loc_id AS obj_id,\n");
		} else {
			buf.append("s.node_id AS obj_id,\n");
		}
		buf.append("s.source_id,\n");
		buf.append("s.time_zone,\n");
		buf.append("'").append(kind == ObjectDatumKind.Location ? 'l' : 'n')
				.append("'::CHARACTER AS kind\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM s\n");
		DatumSqlUtils.joinStreamMetadataExtremeDatumSql("solardatm.da_datm", "ts", false, buf);
		DatumSqlUtils.joinStreamMetadataExtremeDatumSql("solardatm.da_datm", "ts", true, buf);
	}

	private void sqlOrderBy(StringBuilder buf) {
		buf.append("ORDER BY obj_id, source_id");
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);
		sqlSelect(buf);
		sqlFrom(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		DatumSqlUtils.limitOffset(filter, buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
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

}
