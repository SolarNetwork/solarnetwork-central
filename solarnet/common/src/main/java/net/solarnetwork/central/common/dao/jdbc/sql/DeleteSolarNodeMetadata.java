/* ==================================================================
 * DeleteSolarNodeMetadata.java - 13/11/2024 7:00:07 am
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

package net.solarnetwork.central.common.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.SolarNodeMetadata;

/**
 * Delete a {@link SolarNodeMetadata} entity.
 * 
 * @author matt
 * @version 1.0
 */
public class DeleteSolarNodeMetadata implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			DELETE FROM solarnet.sn_node_meta
			WHERE node_id = ?""";

	private final Long id;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID to delete
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DeleteSolarNodeMetadata(Long id) {
		super();
		this.id = requireNonNullArgument(id, "id");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setObject(1, id);
		return stmt;
	}

}