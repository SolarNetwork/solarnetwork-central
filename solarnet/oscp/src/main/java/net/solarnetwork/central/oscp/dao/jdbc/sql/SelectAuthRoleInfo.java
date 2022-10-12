/* ==================================================================
 * SelectAuthRoleInfo.java - 17/08/2022 11:15:08 am
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

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.OscpRole;

/**
 * Select authorization role information.
 * 
 * @author matt
 * @version 1.0
 */
public class SelectAuthRoleInfo implements PreparedStatementCreator, SqlProvider {

	private final OscpRole type;
	private final UserLongCompositePK authId;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of configuration to search in
	 * @param authId
	 *        the authorization ID to search for
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectAuthRoleInfo(OscpRole type, UserLongCompositePK authId) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.authId = requireNonNullArgument(authId, "authId");
		if ( !authId.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("The entity ID must be assigned.");
		}
	}

	@Override
	public String getSql() {
		return format("SELECT user_id, entity_id, role_alias FROM solaroscp.conf_id_for_%s_id(?, ?)",
				type.getAlias());
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, authId.getUserId(), Types.BIGINT);
		stmt.setObject(2, authId.getEntityId(), Types.BIGINT);
		return stmt;
	}

}
