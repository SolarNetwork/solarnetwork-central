/* ==================================================================
 * InsertUserFluxAggregatePublishConfiguration.java - 24/06/2024 10:23:29 am
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

package net.solarnetwork.central.user.flux.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.user.flux.domain.UserFluxAggregatePublishConfiguration;

/**
 * Upsert {@link UserFluxAggregatePublishConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UpsertUserFluxAggregatePublishConfiguration
		implements PreparedStatementCreator, SqlProvider {

	@SuppressWarnings("InlineFormatString")
	private static final String SQL = """
			INSERT INTO solaruser.user_flux_agg_pub_settings (
				created,modified,user_id%s,node_ids,source_ids,publish,retain
			)
			VALUES (?,?,?%s,?,?,?,?)
			ON CONFLICT (user_id, id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, node_ids = EXCLUDED.node_ids
					, source_ids = EXCLUDED.source_ids
					, publish = EXCLUDED.publish
					, retain = EXCLUDED.retain
			""";

	private final Long userId;
	private final UserFluxAggregatePublishConfiguration entity;
	private final boolean update;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the ID of the user to associate the entity with
	 * @param entity
	 *        the entity to insert
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertUserFluxAggregatePublishConfiguration(Long userId,
			UserFluxAggregatePublishConfiguration entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
		this.update = entity.getId().entityIdIsAssigned();
	}

	@Override
	public String getSql() {
		final String idCol = (update ? ",id" : "");
		final String idPlaceholder = (update ? ",?" : "");
		return SQL.formatted(idCol, idPlaceholder);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		final Instant now = Instant.now();
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : now);
		Timestamp mod = Timestamp.from(entity.getModified() != null ? entity.getModified() : now);
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, userId);
		if ( update ) {
			stmt.setObject(++p, entity.getConfigurationId());
		}
		p = CommonSqlUtils.prepareArrayParameter(con, stmt, p, entity.getNodeIds(), true);
		p = CommonSqlUtils.prepareArrayParameter(con, stmt, p, entity.getSourceIds(), true);
		stmt.setBoolean(++p, entity.isPublish());
		stmt.setBoolean(++p, entity.isRetain());
		return stmt;
	}

}
