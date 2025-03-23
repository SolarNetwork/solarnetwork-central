/* ==================================================================
 * UserKeyPairEntityRowMapper.java - 22/03/2025 7:48:25 am
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
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;

/**
 * Row mapper for {@link UserKeyPairEntity} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>skey (TEXT)
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>keystore (BYTE[])</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class UserKeyPairEntityRowMapper implements RowMapper<UserKeyPairEntity> {

	/** A default instance. */
	public static final RowMapper<UserKeyPairEntity> INSTANCE = new UserKeyPairEntityRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public UserKeyPairEntityRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public UserKeyPairEntityRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public UserKeyPairEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		String key = rs.getString(++p);
		Instant ts = getTimestampInstant(rs, ++p);
		Instant mod = getTimestampInstant(rs, ++p);
		byte[] keystore = rs.getBytes(++p);
		UserKeyPairEntity conf = new UserKeyPairEntity(userId, key, ts, mod, keystore);
		return conf;
	}

}
