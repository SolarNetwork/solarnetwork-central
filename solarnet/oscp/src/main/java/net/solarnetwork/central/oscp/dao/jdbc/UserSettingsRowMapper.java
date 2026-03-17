/* ==================================================================
 * UserSettingsRowMapper.java - 10/10/2022 9:05:10 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * Row mapper for user settings.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>pub_in (BOOLEAN)</li>
 * <li>pub_flux (BOOLEAN)</li>
 * <li>node_id (BIGINT)</li>
 * <li>source_id_tmpl (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class UserSettingsRowMapper implements RowMapper<UserSettings> {

	/** A default instance. */
	public static final RowMapper<UserSettings> INSTANCE = new UserSettingsRowMapper();

	@Override
	public UserSettings mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = 0;
		Long userId = rs.getObject(++p, Long.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		boolean pubSolarIn = rs.getBoolean(++p);
		boolean pubSolarFlux = rs.getBoolean(++p);
		Long nodeId = rs.getObject(++p, Long.class);

		final var entity = new UserSettings(userId, ts, nodeId);
		entity.setModified(mod);
		entity.setPublishToSolarIn(pubSolarIn);
		entity.setPublishToSolarFlux(pubSolarFlux);
		entity.setSourceIdTemplate(rs.getString(++p));
		return entity;
	}

}
