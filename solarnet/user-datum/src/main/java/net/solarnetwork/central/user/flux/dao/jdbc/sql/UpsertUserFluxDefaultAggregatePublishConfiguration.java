/* ==================================================================
 * UpsertUserFluxDefaultAggregatePublishConfiguration.java - 25/06/2024 10:50:04 am
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
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.flux.domain.UserFluxDefaultAggregatePublishConfiguration;

/**
 * Upsert {@link UserFluxDefaultAggregatePublishConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UpsertUserFluxDefaultAggregatePublishConfiguration
		implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solaruser.user_flux_default_agg_pub_settings (
				created,modified,user_id,publish,retain
			)
			VALUES (?,?,?,?,?)
			ON CONFLICT (user_id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, publish = EXCLUDED.publish
					, retain = EXCLUDED.retain
			""";

	private final Long userId;
	private final UserFluxDefaultAggregatePublishConfiguration entity;

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
	public UpsertUserFluxDefaultAggregatePublishConfiguration(Long userId,
			UserFluxDefaultAggregatePublishConfiguration entity) {
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
		PreparedStatement stmt = con.prepareStatement(getSql());
		final Instant now = Instant.now();
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : now);
		Timestamp mod = Timestamp.from(entity.getModified() != null ? entity.getModified() : now);
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, userId);
		stmt.setBoolean(++p, entity.isPublish());
		stmt.setBoolean(++p, entity.isRetain());
		return stmt;
	}

}
