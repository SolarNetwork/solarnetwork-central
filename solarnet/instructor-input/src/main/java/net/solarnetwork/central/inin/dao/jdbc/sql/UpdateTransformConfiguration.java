/* ==================================================================
 * UpdateTransformConfiguration.java - 21/02/2024 1:41:55 pm
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
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * Support for UPDATE {@link TransformConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public abstract sealed class UpdateTransformConfiguration<C extends TransformConfiguration<C>>
		implements PreparedStatementCreator, SqlProvider
		permits UpdateTransformConfiguration.UpdateRequestTransformConfiguration,
		UpdateTransformConfiguration.UpdateResponseTransformConfiguration {

	/**
	 * Support for UPDATE {@link RequestTransformConfiguration} entities.
	 */
	public static final class UpdateRequestTransformConfiguration
			extends UpdateTransformConfiguration<RequestTransformConfiguration> {

		private static final String SQL_REQ = SQL.formatted("req");

		/**
		 * Constructor.
		 *
		 * @param id
		 *        the ID
		 * @param entity
		 *        the entity
		 * @throws IllegalArgumentException
		 *         if any argument is {@literal null}
		 */
		public UpdateRequestTransformConfiguration(UserLongCompositePK id,
				RequestTransformConfiguration entity) {
			super(id, entity);
		}

		@Override
		public String getSql() {
			return SQL_REQ;
		}

	}

	/**
	 * Support for UPDATE {@link ResponseTransformConfiguration} entities.
	 */
	public static final class UpdateResponseTransformConfiguration
			extends UpdateTransformConfiguration<ResponseTransformConfiguration> {

		private static final String SQL_RES = SQL.formatted("res");

		/**
		 * Constructor.
		 *
		 * @param id
		 *        the ID
		 * @param entity
		 *        the entity
		 * @throws IllegalArgumentException
		 *         if any argument is {@literal null}
		 */
		public UpdateResponseTransformConfiguration(UserLongCompositePK id,
				ResponseTransformConfiguration entity) {
			super(id, entity);
		}

		@Override
		public String getSql() {
			return SQL_RES;
		}

	}

	private static final String SQL = """
			UPDATE solardin.inin_%s_xform
			SET modified = ?
				, cname = ?
				, sident = ?
				, sprops = ?::jsonb
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongCompositePK id;
	private final C entity;

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
	public UpdateTransformConfiguration(UserLongCompositePK id, C entity) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.entity = requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getPhase(), "entity.phase");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp mod = Timestamp
				.from(entity.getModified() != null ? entity.getModified() : Instant.now());
		int p = 0;
		stmt.setTimestamp(++p, mod);

		stmt.setString(++p, entity.getName());
		stmt.setString(++p, entity.getServiceIdentifier());
		stmt.setString(++p, entity.getServicePropsJson());

		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());

		return stmt;
	}

}
