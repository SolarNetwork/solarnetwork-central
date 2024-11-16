/* ==================================================================
 * StoreSolarNodeMetadata.java - 12/11/2024 9:34:19 pm
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.domain.SolarNodeMetadata;

/**
 * Insert or update {@link SolarNodeMetadata} via a stored procedure.
 * 
 * @author matt
 * @version 1.0
 */
public class StoreSolarNodeMetadata implements CallableStatementCreator, SqlProvider {

	/** The stored procedure call. */
	public static final String SQL = "{call solarnet.store_node_meta(?, ?, ?)}";

	private final Long nodeId;
	private final Instant timestamp;
	private final String metaJson;

	/**
	 * Constructor.
	 * 
	 * @param entity
	 *        the entity to save
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public StoreSolarNodeMetadata(SolarNodeMetadata entity) {
		this(requireNonNullArgument(entity, "entity").getNodeId(), entity.getCreated(),
				entity.getMetaJson());
	}

	/**
	 * Constructor.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param timestamp
	 *        the timestamp
	 * @param metaJson
	 *        the JSON
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public StoreSolarNodeMetadata(Long nodeId, Instant timestamp, String metaJson) {
		super();
		this.nodeId = requireNonNullArgument(nodeId, "nodeId");
		this.timestamp = requireNonNullArgument(timestamp, "timestamp");
		this.metaJson = requireNonNullArgument(metaJson, "metaJson");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement cs = con.prepareCall(getSql());
		cs.setTimestamp(1, Timestamp.from(timestamp));
		cs.setObject(2, nodeId);
		cs.setString(3, metaJson);
		return cs;
	}

}
