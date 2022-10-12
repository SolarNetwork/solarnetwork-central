/* ==================================================================
 * InsertUserSettings.java - 10/10/2022 10:09:09 am
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * Insert a new {@link UserSettings} entity.
 * 
 * @author matt
 * @version 1.0
 */
public class InsertUserSettings implements PreparedStatementCreator, SqlProvider {

	private final UserSettings entity;

	/**
	 * Constructor.
	 * 
	 * @param entity
	 *        the entity to insert
	 */
	public InsertUserSettings(UserSettings entity) {
		super();
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO solaroscp.oscp_user_settings (user_id,created,modified,pub_in,pub_flux,node_id,source_id_tmpl)
				VALUES (?,?,?,?,?,?,?)
				ON CONFLICT (user_id) DO UPDATE SET
					modified = EXCLUDED.modified
					, pub_in = EXCLUDED.pub_in
					, pub_flux = EXCLUDED.pub_flux
					, node_id = EXCLUDED.node_id
					, source_id_tmpl = EXCLUDED.source_id_tmpl
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		int p = 0;
		stmt.setObject(++p, entity.getUserId());
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, ts);
		stmt.setBoolean(++p, entity.isPublishToSolarIn());
		stmt.setBoolean(++p, entity.isPublishToSolarFlux());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getSourceIdTemplate());
		return stmt;
	}

}
