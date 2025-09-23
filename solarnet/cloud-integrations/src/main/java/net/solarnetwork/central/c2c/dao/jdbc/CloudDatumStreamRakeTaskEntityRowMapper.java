/* ==================================================================
 * CloudDatumStreamRakeTaskEntityRowMapper.java - 20/09/2025 7:09:40 pm
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

package net.solarnetwork.central.c2c.dao.jdbc;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getTimestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.BasicClaimableJobState;

/**
 * Row mapper for {@link CloudDatumStreamRakeTaskEntity} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (BIGINT)</li>
 * <li>ds_id (BIGINT)</li>
 * <li>status (CHARACTER)</li>
 * <li>exec_at (TIMESTAMP)</li>
 * <li>offset (INTERVAL)</li>
 * <li>message (TEXT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CloudDatumStreamRakeTaskEntityRowMapper
		implements RowMapper<CloudDatumStreamRakeTaskEntity> {

	/** A default instance. */
	public static final RowMapper<CloudDatumStreamRakeTaskEntity> INSTANCE = new CloudDatumStreamRakeTaskEntityRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CloudDatumStreamRakeTaskEntityRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CloudDatumStreamRakeTaskEntityRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CloudDatumStreamRakeTaskEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		CloudDatumStreamRakeTaskEntity conf = new CloudDatumStreamRakeTaskEntity(userId, entityId);
		conf.setDatumStreamId(rs.getObject(++p, Long.class));
		conf.setState(BasicClaimableJobState.fromValue(rs.getString(++p)));
		conf.setExecuteAt(getTimestampInstant(rs, ++p));
		conf.setOffset(CommonJdbcUtils.getIntervalPeriod(rs, ++p));
		conf.setMessage(rs.getString(++p));
		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
