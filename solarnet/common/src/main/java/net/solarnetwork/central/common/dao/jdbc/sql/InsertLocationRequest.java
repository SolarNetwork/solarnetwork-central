/* ==================================================================
 * InsertLocationRequest.java - 19/05/2022 2:12:52 pm
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
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.LocationRequest;

/**
 * Insert a {@link LocationRequest}.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertLocationRequest
		implements PreparedStatementCreator, SqlProvider, BatchPreparedStatementSetter {

	private final List<LocationRequest> requests;
	private final boolean batchMode;

	/**
	 * Constructor.
	 * 
	 * @param request
	 *        the request
	 */
	public InsertLocationRequest(LocationRequest request) {
		this(Collections.singletonList(requireNonNullArgument(request, "request")), false);
	}

	/**
	 * Constructor.
	 * 
	 * @param requests
	 *        the requests
	 */
	public InsertLocationRequest(List<LocationRequest> requests) {
		this(requests, true);
	}

	/**
	 * Constructor.
	 * 
	 * @param requests
	 *        the requests
	 * @param batchMode
	 *        {@literal true} to support batch mode
	 */
	private InsertLocationRequest(List<LocationRequest> requests, boolean batchMode) {
		super();
		this.requests = requireNonNullArgument(requests, "requests");
		this.batchMode = batchMode;
	}

	@Override
	public int getBatchSize() {
		return requests.size();
	}

	@Override
	public String getSql() {
		return "INSERT INTO solarnet.sn_loc_req (user_id, loc_id, status, jdata)\nVALUES (?, ?, ?, ?::jsonb)";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		if ( !batchMode ) {
			setValues(stmt, 0);
		}
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement ps, int i) throws SQLException {
		LocationRequest req = requests.get(i);
		ps.setObject(1, req.getUserId());
		if ( req.getLocationId() != null ) {
			ps.setObject(2, req.getLocationId());
		} else {
			ps.setNull(2, Types.BIGINT);
		}
		ps.setString(3, String.valueOf((char) req.getStatus().getCode()));
		ps.setString(4, req.getJsonData());
	}

}
