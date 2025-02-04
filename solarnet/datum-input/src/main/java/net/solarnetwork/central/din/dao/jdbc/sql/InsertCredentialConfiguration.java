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

package net.solarnetwork.central.din.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.din.domain.CredentialConfiguration;

/**
 * Support for INSERT {@link CredentialConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class InsertCredentialConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.din_credential (
				  created,modified,user_id,enabled,username,password,expires
			)
			VALUES (?,?,?,?,?,?,?)
			""";

	private final Long userId;
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
	public InsertCredentialConfiguration(Long userId, CredentialConfiguration entity) {
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
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, ts);
		stmt.setObject(++p, userId);

		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getUsername());
		stmt.setString(++p, entity.getPassword());

		Timestamp expires = entity.getExpires() != null ? Timestamp.from(entity.getExpires()) : null;
		stmt.setTimestamp(++p, expires);

		return stmt;
	}

}
