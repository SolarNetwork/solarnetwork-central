/* ==================================================================
 * UserInstructionTaskEntityRowMapper.java - 10/11/2025 4:54:11 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.user.domain.UserNodeInstructionTaskEntity;

/**
 * Row mapper for {@link UserNodeInstructionTaskEntity} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (BIGINT)</li>
 * <li>enabled (BOOL)</li>
 * <li>node_id (BIGINT)</li>
 * <li>topic (TEXT)</li>
 * <li>cname (TEXT)</li>
 * <li>schedule (TEXT)</li>
 * <li>status (CHARACTER)</li>
 * <li>exec_at (TIMESTAMP)</li>
 * <li>sprops (TEXT)</li>
 * <li>last_exec_at (TIMESTAMP)</li>
 * <li>message (TEXT)</li>
 * <li>rprops (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class UserInstructionTaskEntityRowMapper implements RowMapper<UserNodeInstructionTaskEntity> {

	/** A default instance. */
	public static final RowMapper<UserNodeInstructionTaskEntity> INSTANCE = new UserInstructionTaskEntityRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public UserInstructionTaskEntityRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public UserInstructionTaskEntityRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public UserNodeInstructionTaskEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		UserNodeInstructionTaskEntity conf = new UserNodeInstructionTaskEntity(userId, entityId);
		conf.setEnabled(rs.getBoolean(++p));
		conf.setName(rs.getString(++p));
		conf.setNodeId(rs.getObject(++p, Long.class));
		conf.setTopic(rs.getString(++p));
		conf.setSchedule(rs.getString(++p));
		conf.setState(BasicClaimableJobState.fromValue(rs.getString(++p)));
		conf.setExecuteAt(getTimestampInstant(rs, ++p));
		conf.setServicePropsJson(rs.getString(++p));
		conf.setLastExecuteAt(getTimestampInstant(rs, ++p));
		conf.setMessage(rs.getString(++p));
		conf.setResultPropsJson(rs.getString(++p));
		return conf;
	}

}
