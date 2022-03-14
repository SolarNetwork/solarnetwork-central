/* ==================================================================
 * SelectStaleFluxDatum.java - 24/11/2020 7:16:07 am
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
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.datum.v2.dao.AggregationCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.StaleFluxDatum;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Select for {@link StaleFluxDatum} instances via a {@link DatumStreamCriteria}
 * filter.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class SelectStaleFluxDatum implements PreparedStatementCreator, SqlProvider {

	/** A specialized instance for selecting any one row for update. */
	public static final SelectStaleFluxDatum ANY_ONE_FOR_UPDATE;
	static {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setMax(1);
		ANY_ONE_FOR_UPDATE = new SelectStaleFluxDatum(filter, true);
	}

	private final DatumStreamCriteria filter;
	private final Aggregation aggregation;
	private final boolean forUpdate;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStaleFluxDatum(DatumStreamCriteria filter) {
		this(filter, false);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the search criteria
	 * @param forUpdate
	 *        {@literal true} to acquire an exclusive lock on the results
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public SelectStaleFluxDatum(DatumStreamCriteria filter, boolean forUpdate) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.forUpdate = forUpdate;
		this.aggregation = aggregation(filter);
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

	private void sqlSelect(StringBuilder buf) {
		buf.append("SELECT datum.stream_id,\n");
		buf.append("	datum.agg_kind,\n");
		buf.append("	datum.created\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("FROM solardatm.agg_stale_flux datum\n");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( aggregation != Aggregation.None ) {
			where.append("\tAND datum.agg_kind = ?\n");
			idx++;
		}
		idx |= DatumSqlUtils.whereStreamMetadata(filter, buf);
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	private void sqlOrderBy(StringBuilder buf) {
		if ( filter == null ) {
			return;
		}

		StringBuilder order = new StringBuilder();
		int idx = 0;
		if ( filter.hasSorts() ) {
			idx = orderBySorts(filter.getSorts(), DatumSqlUtils.STALE_AGGREGATE_SORT_KEY_MAPPING, order);
		}
		if ( order.length() > 0 ) {
			buf.append("ORDER BY ").append(order.substring(idx));
		}
	}

	private void sqlCore(StringBuilder buf) {
		sqlSelect(buf);
		sqlFrom(buf);
		sqlWhere(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlOrderBy(buf);
		CommonSqlUtils.limitOffsetLiteral(filter, buf);
		if ( forUpdate ) {
			CommonSqlUtils.forUpdate(true, buf);
		}
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		if ( filter.getAggregation() != null ) {
			stmt.setString(++p, filter.getAggregation().getKey());
		}
		p = DatumSqlUtils.prepareStreamFilter(filter, con, stmt, p);
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = CommonSqlUtils.createPreparedStatement(con, getSql(), forUpdate);
		prepareCore(con, stmt, 0);
		return stmt;
	}

}
