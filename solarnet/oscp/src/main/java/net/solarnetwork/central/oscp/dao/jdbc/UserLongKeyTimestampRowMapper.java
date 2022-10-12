/* ==================================================================
 * UserLongKeyRowMapper.java - 16/08/2022 5:54:44 pm
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

package net.solarnetwork.central.oscp.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.UserLongKeyTimestamp;

/**
 * Row mapper for {@link UserLongKeyTimestamp} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>entity_id (BIGINT)</li>
 * <li>ts (TIMESTAMP)</li>
 * </ol>
 */
public class UserLongKeyTimestampRowMapper implements RowMapper<UserLongKeyTimestamp> {

	/** A default instance. */
	public static final RowMapper<UserLongKeyTimestamp> INSTANCE = new UserLongKeyTimestampRowMapper();

	@Override
	public UserLongKeyTimestamp mapRow(ResultSet rs, int rowNum) throws SQLException {
		Timestamp ts = rs.getTimestamp(3);
		return new UserLongKeyTimestamp(
				new UserLongCompositePK(rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
				ts != null ? ts.toInstant() : null);
	}

}
