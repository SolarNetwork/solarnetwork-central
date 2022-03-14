/* ==================================================================
 * DatumEntityRowMapper.java - 13/11/2020 10:22:21 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.AppSetting;

/**
 * Map datum rows into {@link AppSetting} instances.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>skey (VARCHAR)</li>
 * <li>stype (VARCHAR)</li>
 * <li>svalue (VARCHAR)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
public class AppSettingRowMapper implements RowMapper<AppSetting> {

	/** A default instance. */
	public static final RowMapper<AppSetting> INSTANCE = new AppSettingRowMapper();

	@Override
	public AppSetting mapRow(ResultSet rs, int rowNum) throws SQLException {
		Instant created = rs.getTimestamp(1).toInstant();
		Instant modified = rs.getTimestamp(2).toInstant();
		String key = rs.getString(3);
		String type = rs.getString(4);
		String value = rs.getString(5);
		return new AppSetting(key, type, created, modified, value);
	}

}
