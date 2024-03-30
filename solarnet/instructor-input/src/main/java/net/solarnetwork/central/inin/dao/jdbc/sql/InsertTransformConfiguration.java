/* ==================================================================
 * InsertTransformConfiguration.java - 21/02/2024 1:41:28 pm
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
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;

/**
 * Support for INSERT {@link TransformConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public abstract sealed class InsertTransformConfiguration<C extends TransformConfiguration<C>>
		implements PreparedStatementCreator, SqlProvider
		permits InsertTransformConfiguration.InsertRequestTransformConfiguration,
		InsertTransformConfiguration.InsertResponseTransformConfiguration {

	/**
	 * Support for INSERT {@link RequestTransformConfiguration} entities.
	 */
	public static final class InsertRequestTransformConfiguration
			extends InsertTransformConfiguration<RequestTransformConfiguration> {

		private static final String SQL_REQ = SQL.formatted("req");

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
		public InsertRequestTransformConfiguration(Long userId, RequestTransformConfiguration entity) {
			super(userId, entity);
		}

		@Override
		public String getSql() {
			return SQL_REQ;
		}

	}

	/**
	 * Support for INSERT {@link ResponseTransformConfiguration} entities.
	 */
	public static final class InsertResponseTransformConfiguration
			extends InsertTransformConfiguration<ResponseTransformConfiguration> {

		private static final String SQL_RES = SQL.formatted("res");

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
		public InsertResponseTransformConfiguration(Long userId, ResponseTransformConfiguration entity) {
			super(userId, entity);
		}

		@Override
		public String getSql() {
			return SQL_RES;
		}

	}

	private static final String SQL = """
			INSERT INTO solardin.inin_%s_xform (
				  created,modified,user_id,cname,sident,sprops
			)
			VALUES (?,?,?,?,?,?::jsonb)
			""";

	private final Long userId;
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
	public InsertTransformConfiguration(Long userId, C entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.entity = requireNonNullArgument(entity, "entity");
		requireNonNullArgument(entity.getPhase(), "entity.phase");
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.RETURN_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, ts);
		stmt.setObject(++p, userId);

		stmt.setString(++p, entity.getName());
		stmt.setString(++p, entity.getServiceIdentifier());
		stmt.setString(++p, entity.getServicePropsJson());

		return stmt;
	}

}
