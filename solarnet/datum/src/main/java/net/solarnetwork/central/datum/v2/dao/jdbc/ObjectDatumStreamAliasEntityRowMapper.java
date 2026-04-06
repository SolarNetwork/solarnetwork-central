/* ==================================================================
 * ObjectDatumStreamAliasEntityRowMapper.java - 28/03/2026 8:35:58 am
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.uuid;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Map object datum ID rows into {@link ObjectDatumStreamAliasEntity} instances.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>stream_id</li>
 * <li>created</li>
 * <li>modified</li>
 * <li>node_id</li>
 * <li>source_id</li>
 * <li>alias_node_id</li>
 * <li>alias_source_id</li>
 * </ol>
 *
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumStreamAliasEntityRowMapper implements RowMapper<ObjectDatumStreamAliasEntity> {

	/** A default mapper instance . */
	public static final RowMapper<ObjectDatumStreamAliasEntity> INSTANCE = new ObjectDatumStreamAliasEntityRowMapper();

	@Override
	public ObjectDatumStreamAliasEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID streamId = uuid(rs, 1);
		Instant created = timestampInstant(rs, 2);
		Instant modified = timestampInstant(rs, 3);
		Long objId = rs.getObject(4, Long.class);
		String sourceId = rs.getString(5);
		Long aliasObjId = rs.getObject(6, Long.class);
		String aliasSourceId = rs.getString(7);
		return new ObjectDatumStreamAliasEntity(streamId, created, modified, ObjectDatumKind.Node,
				aliasObjId, aliasSourceId, objId, sourceId);
	}

}
