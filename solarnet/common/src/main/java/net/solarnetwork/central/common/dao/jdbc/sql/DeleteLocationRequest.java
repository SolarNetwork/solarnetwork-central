/* ==================================================================
 * DeleteLocationRequest.java - 20/05/2022 7:00:12 am
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;

/**
 * Delete a location request entity.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class DeleteLocationRequest implements PreparedStatementCreator, SqlProvider {

	private final Long id;
	private final LocationRequestCriteria filter;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID to delete
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteLocationRequest(Long id) {
		this(requireNonNullArgument(id, "id"), null);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        filter to limit the query to
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteLocationRequest(LocationRequestCriteria filter) {
		this(null, requireNonNullArgument(filter, "filter"));
	}

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        an optional ID to delete
	 * @param filter
	 *        an optional filter to limit the query to
	 */
	public DeleteLocationRequest(Long id, LocationRequestCriteria filter) {
		super();
		this.id = id;
		this.filter = filter;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder(64);
		buf.append("DELETE FROM solarnet.sn_loc_req");
		StringBuilder where = new StringBuilder(64);
		LocationRequestSqlUtils.appendLocationRequestCriteria(id, filter, where);
		if ( where.length() > 0 ) {
			buf.append(" WHERE").append(where.substring(CommonSqlUtils.WHERE_COMPONENT_PREFIX_LENGTH));
		}
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		LocationRequestSqlUtils.prepareLocationRequestCriteria(id, filter, con, stmt, 0);
		return stmt;
	}

}
