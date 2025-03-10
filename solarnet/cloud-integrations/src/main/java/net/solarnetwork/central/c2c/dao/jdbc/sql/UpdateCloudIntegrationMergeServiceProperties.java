/* ==================================================================
 * UpdateCloudIntegrationMergeServiceProperties.java - 10/03/2025 5:36:31 pm
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.codec.JsonUtils;

/**
 * SQL update to merge a set of service properties into a cloud integration
 * entity.
 *
 * @author matt
 * @version 1.0
 */
public class UpdateCloudIntegrationMergeServiceProperties
		implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solardin.cin_integration
			SET sprops = COALESCE(sprops, '{}'::jsonb) || ?::jsonb
			WHERE user_id = ?
			AND id = ?
			""";

	private final UserLongCompositePK id;
	private final Map<String, ?> serviceProperties;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param serviceProperties
	 *        the service properties to merge
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UpdateCloudIntegrationMergeServiceProperties(UserLongCompositePK id,
			Map<String, ?> serviceProperties) {
		this.id = requireNonNullArgument(id, "id");
		this.serviceProperties = requireNonNullArgument(serviceProperties, "serviceProperties");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setString(++p, JsonUtils.getJSONString(serviceProperties, "{}"));
		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());
		return stmt;
	}

}
