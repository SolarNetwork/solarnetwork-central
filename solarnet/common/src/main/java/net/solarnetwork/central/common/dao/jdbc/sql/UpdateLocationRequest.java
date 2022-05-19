/* ==================================================================
 * UpdateLocationRequest.java - 19/05/2022 2:49:51 pm
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
import java.sql.Statement;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.LocationRequest;

/**
 * Update an existing location request.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class UpdateLocationRequest implements PreparedStatementCreator, SqlProvider {

	private final LocationRequest request;
	private final Long id;

	/**
	 * Constructor.
	 * 
	 * @param request
	 *        the request to update
	 * @param throws
	 *        IllegalArgumentException if {@code request} or {@code request.id}
	 *        is {@literal null}
	 */
	public UpdateLocationRequest(LocationRequest request) {
		super();
		this.request = requireNonNullArgument(request, "request");
		this.id = requireNonNullArgument(request.getId(), "request.id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("UPDATE solarnet.sn_loc_req SET modified = CURRENT_TIMESTAMP");
		if ( request.getLocationId() != null ) {
			buf.append("\n\t, loc_id = ?");
		}
		if ( request.getStatus() != null ) {
			buf.append("\n\t, status = ?");
		}
		if ( request.getMessage() != null ) {
			buf.append("\n\t, message = ?");
		}
		if ( request.getJsonData() != null ) {
			buf.append("\n\t, jdata = ?::jsonb");
		}
		buf.append("\nWHERE id = ?");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		int p = 0;
		if ( request.getLocationId() != null ) {
			stmt.setObject(++p, request.getLocationId());
		}
		if ( request.getStatus() != null ) {
			stmt.setString(++p, String.valueOf((char) request.getStatus().getCode()));
		}
		if ( request.getMessage() != null ) {
			stmt.setString(++p, request.getMessage());
		}
		if ( request.getJsonData() != null ) {
			stmt.setString(++p, request.getJsonData());
		}
		stmt.setObject(++p, id);
		return stmt;
	}

}
