/* ==================================================================
 * UserFluxAggregatePublishConfigurationRowMapper.java - 24/06/2024 2:21:47 pm
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

package net.solarnetwork.central.user.flux.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.arrayValue;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;

/**
 * Row mapper for {@link UserFluxAggregatePublishConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>node_ids (BIGINT[])</li>
 * <li>source_ids (TEXT[])</li>
 * <li>publish (BOOLEAN)</li>
 * <li>retain (BOOLEAN)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class UserFluxAggregatePublishConfigurationRowMapper
		implements RowMapper<UserFluxAggregatePublishConfiguration> {

	/** A default instance. */
	public static final RowMapper<UserFluxAggregatePublishConfiguration> INSTANCE = new UserFluxAggregatePublishConfigurationRowMapper();

	/**
	 * Constructor.
	 */
	public UserFluxAggregatePublishConfigurationRowMapper() {
		super();
	}

	@Override
	public UserFluxAggregatePublishConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = 0;
		final Long userId = rs.getLong(++p);
		final Long id = rs.getLong(++p);
		final Timestamp ts = rs.getTimestamp(++p);
		UserFluxAggregatePublishConfiguration result = new UserFluxAggregatePublishConfiguration(userId,
				id, ts.toInstant());
		result.setModified(rs.getTimestamp(++p).toInstant());
		result.setNodeIds(arrayValue(rs.getArray(++p)));
		result.setSourceIds(arrayValue(rs.getArray(++p)));
		result.setPublish(rs.getBoolean(++p));
		result.setRetain(rs.getBoolean(++p));
		return result;
	}

}
