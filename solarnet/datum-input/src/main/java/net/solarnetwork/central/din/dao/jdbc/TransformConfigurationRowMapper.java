/* ==================================================================
 * TransformConfigurationRowMapper.java - 21/02/2024 1:35:54 pm
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
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.din.domain.TransformConfiguration;

/**
 * Row mapper for {@link TransformConfiguration} entities.
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
 * <li>cname (TEXT)</li>
 * <li>sident (TEXT)</li>
 * <li>sprops (JSON TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class TransformConfigurationRowMapper implements RowMapper<TransformConfiguration> {

	/** A default instance. */
	public static final RowMapper<TransformConfiguration> INSTANCE = new TransformConfigurationRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public TransformConfigurationRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public TransformConfigurationRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public TransformConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long entityId = rs.getObject(++p, Long.class);
		Timestamp ts = rs.getTimestamp(++p);
		TransformConfiguration conf = new TransformConfiguration(userId, entityId, ts.toInstant());
		conf.setModified(rs.getTimestamp(++p).toInstant());
		conf.setName(rs.getString(++p));
		conf.setServiceIdentifier(rs.getString(++p));
		conf.setServicePropsJson(rs.getString(++p));
		return conf;
	}

}
