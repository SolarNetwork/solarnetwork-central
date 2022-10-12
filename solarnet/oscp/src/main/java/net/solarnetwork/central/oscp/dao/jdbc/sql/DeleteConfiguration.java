/* ==================================================================
 * BaseDeleteConfiguration.java - 14/08/2022 7:38:18 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareOptimizedArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.whereOptimizedArrayContains;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.oscp.dao.ConfigurationFilter;

/**
 * Support for DELETE configuration entities.
 * 
 * @author matt
 * @version 1.0
 */
public class DeleteConfiguration implements PreparedStatementCreator, SqlProvider {

	private final ConfigurationFilter filter;
	private final String tableName;

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the delete criteria
	 * @param tableName
	 *        the table name
	 */
	public DeleteConfiguration(ConfigurationFilter filter, String tableName) {
		super();
		this.filter = requireNonNullArgument(filter, "filter");
		this.tableName = requireNonNullArgument(tableName, "tableName");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		sqlWhere(buf);
		return buf.toString();
	}

	private void sqlCore(StringBuilder buf) {
		buf.append(String.format("DELETE FROM %s\n", tableName));
	}

	private void sqlWhere(StringBuilder buf) {
		StringBuilder where = new StringBuilder();
		int idx = 0;
		if ( filter.hasUserCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getUserIds(), "user_id", where);
		}
		if ( filter.hasConfigurationCriteria() ) {
			idx += whereOptimizedArrayContains(filter.getConfigurationIds(), "id", where);
		}
		if ( idx > 0 ) {
			buf.append("WHERE").append(where.substring(4));
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		if ( filter.hasUserCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getUserIds());
		}
		if ( filter.hasConfigurationCriteria() ) {
			p = prepareOptimizedArrayParameter(con, stmt, p, filter.getConfigurationIds());
		}
		return stmt;
	}

}
