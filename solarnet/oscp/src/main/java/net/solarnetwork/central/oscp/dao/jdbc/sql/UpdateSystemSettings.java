/* ==================================================================
 * InsertCapacityProviderConiguration.java - 12/08/2022 6:51:01 am
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
import java.sql.Types;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.SystemSettings;

/**
 * Update {@link CapacityProviderConfiguration} entity settings.
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateSystemSettings implements PreparedStatementCreator, SqlProvider {

	private final OscpRole type;
	private final UserLongCompositePK id;
	private final SystemSettings settings;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of settings
	 * @param id
	 *        the ID of the the entity to update
	 * @param entity
	 *        the entity data to update
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpdateSystemSettings(OscpRole type, UserLongCompositePK id, SystemSettings settings) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.id = requireNonNullArgument(id, "id");
		this.settings = requireNonNullArgument(settings, "settings");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
	}

	@Override
	public String getSql() {
		return String.format("""
				UPDATE solaroscp.oscp_%s_conf SET
					  modified = ?
					, heartbeat_secs = ?
					, meas_styles = ?
				WHERE user_id = ? AND id = ?
				""", type.getAlias());
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setTimestamp(++p, Timestamp.from(Instant.now()));
		if ( settings.heartbeatSeconds() != null ) {
			stmt.setObject(++p, settings.heartbeatSeconds(), Types.SMALLINT);
		} else {
			stmt.setNull(++p, Types.SMALLINT);
		}
		p = CommonSqlUtils.prepareCodedValuesArray(con, stmt, p, settings.measurementStyles(), true);
		stmt.setObject(++p, id.getUserId(), Types.BIGINT);
		stmt.setObject(++p, id.getEntityId(), Types.BIGINT);
		return stmt;
	}

}
