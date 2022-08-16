/* ==================================================================
 * InsertAuthToken.java - 17/08/2022 8:00:48 am
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Insert an authorization token (i.e. for an external system).
 * 
 * @author matt
 * @version 1.0
 */
public class InsertAuthToken implements CallableStatementCreator, SqlProvider {

	private final AuthTokenType type;
	private final UserLongCompositePK id;
	private final String token;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of token to create
	 * @param id
	 *        the ID associated with the token
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null} or the {@code id} is not
	 *         assigned
	 */
	public InsertAuthToken(AuthTokenType type, UserLongCompositePK id, String token) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.id = requireNonNullArgument(id, "id");
		this.token = requireNonNullArgument(token, "token");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("The entity ID must be assigned.");
		} else if ( type == AuthTokenType.FlexibilityProvider ) {
			throw new IllegalArgumentException("Flexibility Provider tokens cannot be saved.");
		}
	}

	@Override
	public String getSql() {
		return String.format("{call solaroscp.save_%s_token(?, ?, ?)}", type.getAlias());
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.setObject(1, id.getUserId(), Types.BIGINT);
		stmt.setObject(2, id.getEntityId(), Types.BIGINT);
		stmt.setString(3, token);
		return stmt;
	}

}
