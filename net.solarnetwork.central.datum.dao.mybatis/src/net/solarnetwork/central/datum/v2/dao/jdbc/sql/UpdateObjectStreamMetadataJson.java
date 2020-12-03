/* ==================================================================
 * UpdateObjectStreamMetadataJson.java - 27/11/2020 4:48:50 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.ObjectMetadataCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;

/**
 * Update the JSON value of a datum metadata stream.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class UpdateObjectStreamMetadataJson implements PreparedStatementCreator, SqlProvider {

	private final ObjectMetadataCriteria filter;
	private final ObjectDatumKind kind;
	private final String json;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * The {@code filter.getObjectKind()} value will be used if available,
	 * otherwise the {@code Node} kind will be used.
	 * </p>
	 * 
	 * @param filter
	 *        the filter
	 * @param json
	 *        the JSON to save
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public UpdateObjectStreamMetadataJson(ObjectMetadataCriteria filter, String json) {
		this(filter, filter.getObjectKind() != null ? filter.getObjectKind() : ObjectDatumKind.Node,
				json);
	}

	/**
	 * Constructor.
	 * 
	 * @param filter
	 *        the filter
	 * @param kind
	 *        the kind
	 * @param json
	 *        the JSON to save
	 * 
	 * @throws IllegalArgumentException
	 *         if {@code filter} is {@literal null}
	 */
	public UpdateObjectStreamMetadataJson(ObjectMetadataCriteria filter, ObjectDatumKind kind,
			String json) {
		super();
		if ( filter == null ) {
			throw new IllegalArgumentException("The filter argument must not be null.");
		}
		if ( kind == null ) {
			throw new IllegalArgumentException("The kind argument must not be null.");
		}
		this.filter = filter;
		this.kind = kind;
		this.json = json;
	}

	private String sqlTableName() {
		if ( kind == ObjectDatumKind.Location ) {
			return "da_loc_datm_meta";
		}
		return "da_datm_meta";
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("UPDATE solardatm.").append(sqlTableName()).append("\n");
		buf.append("SET jdata = ?::jsonb\n");
		buf.append("WHERE");
		if ( kind == ObjectDatumKind.Location ) {
			buf.append(" loc_id = ?\n");
		} else {
			buf.append(" node_id = ?\n");
		}
		buf.append("	AND source_id = ?\n");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, json);
		stmt.setObject(2, filter.getObjectId());
		stmt.setString(3, filter.getSourceId());
		return stmt;
	}

}
