/* ==================================================================
 * SolarNodeMetadataRowMapper.java - 12/11/2024 8:37:12 pm
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

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.SolarNodeMetadata;

/**
 * {@link RowMapper} for {@link SolarNodeMetadata} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>node_id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>jdata (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class SolarNodeMetadataRowMapper implements RowMapper<SolarNodeMetadata> {

	public static final RowMapper<SolarNodeMetadata> INSTANCE = new SolarNodeMetadataRowMapper();

	/**
	 * Constructor.
	 */
	public SolarNodeMetadataRowMapper() {
		super();
	}

	@Override
	public SolarNodeMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
		SolarNodeMetadata result = new SolarNodeMetadata();
		result.setId(rs.getObject(1, Long.class));
		result.setCreated(getTimestampInstant(rs, 2));
		result.setUpdated(getTimestampInstant(rs, 3));
		result.setMetaJson(rs.getString(4));
		return result;
	}

}
