/* ==================================================================
 * SelectUserAuthTokenNodes.java - 6/10/2021 3:17:17 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;

/**
 * Select for {@code [token,nodeId]} tuples.
 * 
 * <p>
 * The result columns in the SQL are:
 * </p>
 * 
 * <ol>
 * <li>token_id (VARCHAR)</li>
 * <li>node_id (BIGINT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class SelectUserAuthTokenNodes implements PreparedStatementCreator, SqlProvider {

	private final String tokenId;

	/**
	 * Constructor.
	 * 
	 * @param tokenId
	 *        the token ID
	 * @throws IllegalArgumentException
	 *         if {@code tokenId} is {@literal null}
	 */
	public SelectUserAuthTokenNodes(String tokenId) {
		super();
		this.tokenId = requireNonNullArgument(tokenId, "tokenId");
	}

	private void sqlCore(StringBuilder buf) {
		buf.append("SELECT auth_token, node_id\n");
		buf.append("FROM solaruser.user_auth_token_nodes\n");
		buf.append("WHERE auth_token = ?\n");
		buf.append("ORDER BY node_id");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		sqlCore(buf);
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
		stmt.setString(1, tokenId);
		return stmt;
	}

}
