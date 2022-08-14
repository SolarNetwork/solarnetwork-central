/* ==================================================================
 * InsertCapacityOptimizerConiguration.java - 12/08/2022 6:51:01 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareCodedValue;
import static net.solarnetwork.central.oscp.domain.RegistrationStatus.Pending;
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
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;

/**
 * Insert {@link CapacityOptimizerConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertCapacityOptimizerConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solaruser.user_oscp_co_conf (
				created,modified,user_id,enabled,reg_status,cname,url,token,sprops
			)
			VALUES (?,?,?,?,?,?,?,?,?::jsonb)
			""";

	private final Long userId;
	private final CapacityOptimizerConfiguration entity;

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
	public InsertCapacityOptimizerConfiguration(Long userId, CapacityOptimizerConfiguration entity) {
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
		p = prepareCodedValue(stmt, p, entity.getRegistrationStatus(), Pending, false);
		stmt.setString(++p, entity.getName());
		stmt.setString(++p, entity.getBaseUrl());
		stmt.setString(++p, entity.getToken());

		p = CommonSqlUtils.prepareJsonString(entity.getServiceProps(), stmt, p, true);

		return stmt;
	}

}
