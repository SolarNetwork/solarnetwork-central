/* ==================================================================
 * InsertObjectDatumStreamAliasGrantEntity.java - 31/10/2026 4:22:15 pm
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareArrayParameter;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;

/**
 * Support for INSERT for {@link ObjectDatumStreamAliasGrantEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertObjectDatumStreamAliasGrantEntity
		implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solaruser.da_datm_alias_grant (
				created,modified,user_id,enabled,allow_public,grantee_id,node_ids,source_ids
			)
			VALUES (?,?,?,?,?,?,?,?)
			""";

	private final Long userId;
	private final ObjectDatumStreamAliasGrantEntity entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public InsertObjectDatumStreamAliasGrantEntity(Long userId,
			ObjectDatumStreamAliasGrantEntity entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : ts;
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, userId);
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setBoolean(++p, entity.isAllowPublic());
		stmt.setObject(++p, entity.getGranteeId());
		p = prepareArrayParameter(con, stmt, p, entity.getNodeIds(), true);
		prepareArrayParameter(con, stmt, p, entity.getSourceIds(), true);
		return stmt;
	}

}
