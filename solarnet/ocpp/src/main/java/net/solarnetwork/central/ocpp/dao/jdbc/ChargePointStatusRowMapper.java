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
 * <li>connected_to</li>
 * <li>connected_date</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class ChargePointStatusRowMapper implements RowMapper<ChargePointStatus> {

	/** A default instance for null aggregates. */
	public static final RowMapper<ChargePointStatus> INSTANCE = new ChargePointStatusRowMapper();

	@Override
	public ChargePointStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant created = rs.getTimestamp(1).toInstant();
		long userId = rs.getLong(2);
		long cpId = rs.getLong(3);
		String connectedTo = rs.getString(4);
		Instant connectedDate = rs.getTimestamp(5).toInstant();
		return new ChargePointStatus(userId, cpId, created, connectedTo, connectedDate);
	}

}
