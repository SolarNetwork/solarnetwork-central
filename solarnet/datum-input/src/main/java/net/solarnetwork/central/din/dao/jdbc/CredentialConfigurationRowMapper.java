/* ==================================================================
 * CredentialConfigurationRowMapper.java - 21/02/2024 8:25:37 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.din.domain.CredentialConfiguration;

/**
 * Row mapper for {@link CredentialConfiguration} entities.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>id (LONG)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>username (TEXT)</li>
 * <li>password (TEXT)</li>
 * <li>expires (TIMESTAMP)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class CredentialConfigurationRowMapper implements RowMapper<CredentialConfiguration> {

	/** A default instance. */
	public static final RowMapper<CredentialConfiguration> INSTANCE = new CredentialConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public CredentialConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public CredentialConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public CredentialConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = nonnull(rs.getObject(++p, Long.class), "userId");
		Long entityId = nonnull(rs.getObject(++p, Long.class), "entityId");
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		boolean enabled = rs.getBoolean(++p);
		String username = nonnull(rs.getString(++p), "username");

		final var conf = new CredentialConfiguration(userId, entityId, ts, username);
		conf.setModified(mod);
		conf.setEnabled(enabled);
		conf.setPassword(rs.getString(++p));
		conf.setExpires(CommonJdbcUtils.getTimestampInstant(rs, ++p));
		return conf;
	}

}
