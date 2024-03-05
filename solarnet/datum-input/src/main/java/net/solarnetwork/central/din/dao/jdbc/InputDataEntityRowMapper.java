/* ==================================================================
 * InputDataEntityRowMapper.java - 5/03/2024 10:54:12 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.din.domain.InputDataEntity;

/**
 * Row mapper for {@link InputDataEntity} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>node_id (BIGINT)</li>
 * <li>source_id (TEXT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>input_data (BYTE ARRAY)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class InputDataEntityRowMapper implements RowMapper<InputDataEntity> {

	/** A default instance. */
	public static final RowMapper<InputDataEntity> INSTANCE = new InputDataEntityRowMapper();

	/**
	 * Default constructor.
	 */
	public InputDataEntityRowMapper() {
		super();
	}

	@Override
	public InputDataEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long userId = rs.getLong(1);
		Long nodeId = rs.getLong(2);
		String sourceId = rs.getString(3);
		Instant ts = CommonJdbcUtils.getTimestampInstant(rs, 4);
		byte[] data = rs.getBytes(5);
		return new InputDataEntity(userId, nodeId, sourceId, ts, data);
	}

}
