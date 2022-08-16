/* ==================================================================
 * CreateAuthToken.java - 16/08/2022 3:54:27 pm
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
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select an authorization token.
 * 
 * <p>
 * This query only returns a row if the given token filter matches and is not
 * disabled.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SelectAuthTokenId implements PreparedStatementCreator, SqlProvider {

	private final AuthTokenType type;
	private final String token;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *        the type of token to create
	 * @param token
	 *        the token to get the ID for
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public SelectAuthTokenId(AuthTokenType type, String token) {
		super();
		this.type = requireNonNullArgument(type, "type");
		this.token = requireNonNullArgument(token, "token");
	}

	@Override
	public String getSql() {
		return String.format("SELECT user_id, id FROM solaroscp.%s_id_for_token(?)", type.getAlias());
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, token);
		return stmt;
	}

}
