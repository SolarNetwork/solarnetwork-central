/* ==================================================================
 * LocationRequestRowMapper.java - 19/05/2022 3:01:13 pm
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

import static net.solarnetwork.domain.CodedValue.forCodeValue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestStatus;

/**
 * Row mapper for {@link LocationRequest} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>user_id (BIGINT)</li>
 * <li>status (CHAR)</li>
 * <li>jdata (TEXT)</li>
 * <li>loc_id (BIGINT)</li>
 * <li>message (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public class LocationRequestRowMapper implements RowMapper<LocationRequest> {

	/** A default instance. */
	public static final RowMapper<LocationRequest> INSTANCE = new LocationRequestRowMapper();

	@Override
	public LocationRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long id = rs.getLong(1);
		Instant created = rs.getTimestamp(2).toInstant();
		LocationRequest result = new LocationRequest(id, created);
		result.setModified(rs.getTimestamp(3).toInstant());
		result.setUserId((Long) rs.getObject(4));
		result.setStatus(
				forCodeValue((int) rs.getString(5).charAt(0), LocationRequestStatus.class, null));
		result.setJsonData(rs.getString(6));
		result.setLocationId((Long) rs.getObject(7));
		result.setMessage(rs.getString(8));
		return result;
	}

}
