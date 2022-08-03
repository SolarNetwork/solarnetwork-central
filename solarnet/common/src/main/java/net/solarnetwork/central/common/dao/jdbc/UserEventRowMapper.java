/* ==================================================================
 * UserEventRowMapper.java - 3/08/2022 11:31:27 am
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getUuid;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.UserEvent;

/**
 * Row mapper for {@link UserEvent} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>ts (TIMESTAMP)</li>
 * <li>id (UUID)</li>
 * <li>kind (TEXT)</li>
 * <li>message (TEXT)</li>
 * <li>jdata (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class UserEventRowMapper implements RowMapper<UserEvent> {

	/** A default instance. */
	public static final RowMapper<UserEvent> INSTANCE = new UserEventRowMapper();

	@Override
	public UserEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long userId = rs.getLong(1);
		Instant created = rs.getTimestamp(2).toInstant();
		UUID eventId = getUuid(rs, 3);
		String kind = rs.getString(4);
		String message = rs.getString(5);
		String json = rs.getString(6);
		return new UserEvent(userId, created, eventId, kind, message, json);
	}

}
