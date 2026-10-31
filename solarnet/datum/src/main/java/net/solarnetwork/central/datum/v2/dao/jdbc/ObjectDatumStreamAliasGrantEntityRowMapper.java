/* ==================================================================
 * ObjectDatumStreamAliasGrantEntityRowMapper.java - 31/10/2026 4:20:05 pm
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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.getArray;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;

/**
 * Row mapper for {@link ObjectDatumStreamAliasGrantEntity} entities.
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
 * <li>enabled (BOOLEAN)</li>
 * <li>allow_public (BOOLEAN)</li>
 * <li>grantee_id (BIGINT)</li>
 * <li>node_ids (BIGINT[])</li>
 * <li>source_ids (TEXT[])</li>
 * </ol>
 *
 * @author matt
 * @version 1.0
 */
public class ObjectDatumStreamAliasGrantEntityRowMapper
		implements RowMapper<ObjectDatumStreamAliasGrantEntity> {

	/** A default instance. */
	public static final RowMapper<ObjectDatumStreamAliasGrantEntity> INSTANCE = new ObjectDatumStreamAliasGrantEntityRowMapper();

	private final int columnOffset;

	/**
	 * Default constructor.
	 */
	public ObjectDatumStreamAliasGrantEntityRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 *
	 * @param columnOffset
	 *        a column offset to apply
	 */
	public ObjectDatumStreamAliasGrantEntityRowMapper(int columnOffset) {
		this.columnOffset = columnOffset;
	}

	@Override
	public ObjectDatumStreamAliasGrantEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
		int p = columnOffset;
		Long userId = rs.getObject(++p, Long.class);
		Long grantId = rs.getObject(++p, Long.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		boolean enabled = rs.getBoolean(++p);
		boolean allowPublic = rs.getBoolean(++p);
		Long granteeId = rs.getObject(++p, Long.class);

		final ObjectDatumStreamAliasGrantEntity grant = new ObjectDatumStreamAliasGrantEntity(userId,
				grantId, ts, granteeId);
		grant.setModified(mod);
		grant.setEnabled(enabled);
		grant.setAllowPublic(allowPublic);
		grant.setNodeIds(getArray(rs, ++p));
		grant.setSourceIds(getArray(rs, ++p));
		return grant;
	}

}
