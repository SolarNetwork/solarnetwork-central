/* ==================================================================
 * UpsertUserSettingsEntity.java - 28/10/2024 7:38:26 am
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;

/**
 * Support for INSERT ... ON CONFLICT {@link UserSettingsEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertUserSettingsEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.cin_user_settings (
				user_id,created,modified,pub_in,pub_flux
			)
			VALUES (?,?,?,?,?)
			ON CONFLICT (user_id) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, pub_in = EXCLUDED.pub_in
					, pub_flux = EXCLUDED.pub_flux
			""";

	private final Long userId;
	private final UserSettingsEntity entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertUserSettingsEntity(Long userId, UserSettingsEntity entity) {
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
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : ts;
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		int p = 0;
		stmt.setObject(++p, userId);
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setBoolean(++p, entity.isPublishToSolarIn());
		stmt.setBoolean(++p, entity.isPublishToSolarFlux());
		return stmt;
	}

}
