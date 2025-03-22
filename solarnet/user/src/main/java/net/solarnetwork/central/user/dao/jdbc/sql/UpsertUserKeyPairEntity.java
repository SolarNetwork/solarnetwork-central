/* ==================================================================
 * UpsertUserKeyPairEntity.java - 22/03/2025 6:44:28 am
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

package net.solarnetwork.central.user.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;

/**
 * Support for INSERT ... ON CONFLICT {@link UserKeyPairEntity} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertUserKeyPairEntity implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solaruser.user_keypair (
				user_id, skey, created, modified, keystore
			)
			VALUES (?,?,?,?,?)
			ON CONFLICT (user_id, skey) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, keystore = EXCLUDED.keystore
			""";

	private final Long userId;
	private final UserKeyPairEntity entity;

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
	public UpsertUserKeyPairEntity(Long userId, UserKeyPairEntity entity) {
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
		stmt.setObject(1, userId);
		stmt.setString(2, entity.getKey());
		stmt.setTimestamp(3, ts);
		stmt.setTimestamp(4, mod);
		stmt.setBytes(5, entity.keyStoreData());
		return stmt;
	}

}
