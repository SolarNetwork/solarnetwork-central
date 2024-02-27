/* ==================================================================
 * ChargePointActionStatusRowMapper.java - 17/11/2022 8:22:08 am
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

/**
 * Map datum rows into {@link ChargePointActionStatus} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>created</li>
 * <li>user_id</li>
 * <li>cp_id</li>
 * <li>evse_id</li>
 * <li>conn_id</li>
 * <li>action</li>
 * <li>msg_id</li>
 * <li>ts</li>
 * </ol>
 * 
 * @author matt
 * @version 1.1
 */
public class ChargePointActionStatusRowMapper implements RowMapper<ChargePointActionStatus> {

	/** A default instance. */
	public static final RowMapper<ChargePointActionStatus> INSTANCE = new ChargePointActionStatusRowMapper();

	@Override
	public ChargePointActionStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant created = rs.getTimestamp(1).toInstant();
		long userId = rs.getLong(2);
		long cpId = rs.getLong(3);
		int evseId = rs.getInt(4);
		int connId = rs.getInt(5);
		String action = rs.getString(6);
		String messageId = rs.getString(7);
		Instant date = rs.getTimestamp(8).toInstant();
		var status = new ChargePointActionStatus(userId, cpId, evseId, connId, action, created);
		status.setMessageId(messageId);
		status.setTimestamp(date);
		return status;
	}

}
