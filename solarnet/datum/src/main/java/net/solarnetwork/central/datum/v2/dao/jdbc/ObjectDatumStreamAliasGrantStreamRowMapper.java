/* ==================================================================
 * ObjectDatumStreamAliasGrantStreamRowMapper.java - 31/10/2026 8:15:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;

/**
 * Row mapper for {@link ObjectDatumStreamAliasGrantStream} objects.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>user_id (BIGINT)</li>
 * <li>node_id (BIGINT)</li>
 * <li>source_id (TEXT)</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumStreamAliasGrantStreamRowMapper
		implements RowMapper<ObjectDatumStreamAliasGrantStream> {

	/** A default instance. */
	public static final RowMapper<ObjectDatumStreamAliasGrantStream> INSTANCE = new ObjectDatumStreamAliasGrantStreamRowMapper();

	/**
	 * Constructor.
	 */
	public ObjectDatumStreamAliasGrantStreamRowMapper() {
		super();
	}

	@Override
	public ObjectDatumStreamAliasGrantStream mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new ObjectDatumStreamAliasGrantStream(rs.getObject(1, Long.class),
				rs.getObject(2, Long.class), rs.getString(3));
	}

}
