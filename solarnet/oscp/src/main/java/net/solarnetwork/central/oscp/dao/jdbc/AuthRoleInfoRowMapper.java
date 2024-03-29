/* ==================================================================
 * AuthRoleInfoRowMapper.java - 17/08/2022 11:09:11 am
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
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Row mapper for {@link AssetConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>entity_id (BIGINT)</li>
 * <li>role_alias (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class AuthRoleInfoRowMapper implements RowMapper<AuthRoleInfo> {

	/** A default instance. */
	public static final RowMapper<AuthRoleInfo> INSTANCE = new AuthRoleInfoRowMapper();

	@Override
	public AuthRoleInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long userId = rs.getLong(1);
		Long entityId = rs.getLong(2);
		OscpRole role = OscpRole.forAlias(rs.getString(3));
		return new AuthRoleInfo(new UserLongCompositePK(userId, entityId), role);
	}

}
