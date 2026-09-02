/* ==================================================================
 * SelectDatumAuxiliary.java - 28/11/2020 9:47:43 am
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.support.SearchFilterUtils;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.util.SearchFilter;

/**
 * Delete for {@link DatumAuxiliaryEntity} instances via a
 * {@link DatumAuxiliaryCriteria} filter.
 *
 * @author matt
 * @version 1.0
 */
public final class DeleteDatumAuxiliaryByFilter implements PreparedStatementCreator, SqlProvider {

	private final DatumAuxiliaryCriteria filter;
	private final @Nullable SearchFilter searchFilter;

	/**
	 * Constructor.
	 *
	 * @param filter
	 *        the search criteria
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@code null}
	 */
	public DeleteDatumAuxiliaryByFilter(DatumAuxiliaryCriteria filter) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.searchFilter = SearchFilter.forLDAPSearchFilterString(filter.getSearchFilter());
	}

	private void sqlCte(StringBuilder buf) {
		buf.append("WITH s AS (\n");
		DatumSqlUtils.nodeMetadataFilterSql(filter,
				filter.hasLocalDateRange() ? DatumSqlUtils.MetadataSelectStyle.WithZone
						: DatumSqlUtils.MetadataSelectStyle.Minimum,
				buf);
		buf.append(")\n");
	}

	private void sqlFrom(StringBuilder buf) {
		buf.append("DELETE FROM solardatm.da_datm_aux datum\n");
		buf.append("USING s\n");
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		where.append("WHERE datum.stream_id = s.stream_id\n");
		if ( filter.getDatumAuxiliaryType() != null ) {
			where.append("\tAND datum.atype = ?::solardatm.da_datm_aux_type\n");
		}
		if ( filter.hasLocalDateRange() ) {
			DatumSqlUtils.whereLocalDateRange(filter, Aggregation.None,
					DatumSqlUtils.SQL_AT_STREAM_METADATA_TIME_ZONE, where);
		} else {
			DatumSqlUtils.whereDateRange(filter, Aggregation.None, where);
		}
		if ( searchFilter != null ) {
			where.append("\tAND jsonb_path_exists(datum.jmeta, ?::jsonpath)\n");
		}
		buf.append(where);
	}

	private void sqlCore(StringBuilder buf) {
		sqlCte(buf);
		sqlFrom(buf);
		sqlWhere(buf);
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		return buf.toString();
	}

	private int prepareCore(Connection con, PreparedStatement stmt, int p) throws SQLException {
		p = DatumSqlUtils.prepareDatumMetadataFilter(filter, con, stmt, p);
		if ( filter.getDatumAuxiliaryType() != null ) {
			stmt.setString(++p, filter.getDatumAuxiliaryType().name());
		}
		if ( filter.hasLocalDateRange() ) {
			p = DatumSqlUtils.prepareLocalDateRangeFilter(filter, stmt, p);
		} else {
			p = DatumSqlUtils.prepareDateRangeFilter(filter, stmt, p);
		}
		if ( searchFilter != null ) {
			stmt.setString(++p, SearchFilterUtils.toSqlJsonPath(searchFilter));
		}
		return p;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = prepareCore(con, stmt, 0);
		DatumSqlUtils.preparePaginationFilter(filter, con, stmt, p);
		return stmt;
	}

}
