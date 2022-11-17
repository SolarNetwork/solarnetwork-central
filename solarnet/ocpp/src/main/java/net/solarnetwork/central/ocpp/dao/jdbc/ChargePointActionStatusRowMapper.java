/* ==================================================================
 * ChargePointStatusRowMapper.java - 17/11/2022 8:22:08 am
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

package net.solarnetwork.central.ocpp.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;
import net.solarnetwork.central.ocpp.domain.ChargePointStatus;

/**
 * Map datum rows into {@link ChargePointStatus} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>created</li>
 * <li>user_id</li>
 * <li>cp_id</li>
 * <li>conn_id</li>
 * <li>action</li>
 * <li>ts</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointActionStatusRowMapper implements RowMapper<ChargePointActionStatus> {

	/** A default instance. */
	public static final RowMapper<ChargePointActionStatus> INSTANCE = new ChargePointActionStatusRowMapper();

	@Override
	public ChargePointActionStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant created = rs.getTimestamp(1).toInstant();
		long userId = rs.getLong(2);
		long cpId = rs.getLong(3);
		int connId = rs.getInt(4);
		String action = rs.getString(5);
		Instant date = rs.getTimestamp(6).toInstant();
		var status = new ChargePointActionStatus(userId, cpId, connId, action, created);
		status.setTimestamp(date);
		return status;
	}

}
