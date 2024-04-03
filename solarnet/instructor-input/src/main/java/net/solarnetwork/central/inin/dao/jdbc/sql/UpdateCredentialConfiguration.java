/* ==================================================================
 * UpdateCredentialConfiguration.java - 21/02/2024 7:00:19 am
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

package net.solarnetwork.central.inin.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.inin.domain.CredentialConfiguration;

/**
 * Support for UPDATE {@link CredentialConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCredentialConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.inin_credential
			SET modified = ?
				, enabled = ?
				, username = ?
				, password = ?
				, expires = ?
				, oauth = ?
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongCompositePK id;
	private final CredentialConfiguration entity;

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
	public UpdateCredentialConfiguration(UserLongCompositePK id, CredentialConfiguration entity) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.entity = requireNonNullArgument(entity, "entity");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp mod = Timestamp
				.from(entity.getModified() != null ? entity.getModified() : Instant.now());
		int p = 0;
		stmt.setTimestamp(++p, mod);

		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getUsername());
		stmt.setString(++p, entity.getPassword());

		Timestamp expires = entity.getExpires() != null ? Timestamp.from(entity.getExpires()) : null;
		stmt.setTimestamp(++p, expires);

		stmt.setBoolean(++p, entity.isOauth());

		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());

		return stmt;
	}

}
