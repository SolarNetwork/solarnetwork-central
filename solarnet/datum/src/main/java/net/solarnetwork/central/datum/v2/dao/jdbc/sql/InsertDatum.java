/* ==================================================================
 * InsertDatum.java - 19/11/2020 6:02:48 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;

/**
 * Insert a {@link DatumEntity} into the {@literal solardatm.da_datm} table.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class InsertDatum implements PreparedStatementCreator, SqlProvider {

	private final DatumEntity datum;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 * @throws IllegalArgumentException
	 *         if {@code datum} is {@literal null}
	 */
	public InsertDatum(DatumEntity datum) {
		super();
		this.datum = requireNonNullArgument(datum, "datum");
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("INSERT INTO solardatm.da_datm (stream_id");
		if ( datum.getTimestamp() != null ) {
			buf.append(", ts");
		}
		if ( datum.getReceived() != null ) {
			buf.append(", received");
		}
		buf.append(", data_i, data_a, data_s, data_t)\n");
		buf.append("VALUES (?");
		if ( datum.getTimestamp() != null ) {
			buf.append(",?");
		}
		if ( datum.getReceived() != null ) {
			buf.append(",?");
		}
		buf.append(",?,?,?,?)\n");
		buf.append("ON CONFLICT (stream_id, ts) DO UPDATE\n");
		buf.append("SET received = EXCLUDED.received\n");
		buf.append("\t, data_i = EXCLUDED.data_i\n");
		buf.append("\t, data_a = EXCLUDED.data_a\n");
		buf.append("\t, data_s = EXCLUDED.data_s\n");
		buf.append("\t, data_t = EXCLUDED.data_t\n");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setObject(++p, datum.getStreamId(), Types.OTHER);
		if ( datum.getTimestamp() != null ) {
			stmt.setTimestamp(++p, Timestamp.from(datum.getTimestamp()));
		}
		if ( datum.getReceived() != null ) {
			stmt.setTimestamp(++p, Timestamp.from(datum.getReceived()));
		}

		DatumProperties props = datum.getProperties();
		if ( props != null && props.getInstantaneousLength() > 0 ) {
			Array array = con.createArrayOf("NUMERIC", props.getInstantaneous());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}
		if ( props != null && props.getAccumulatingLength() > 0 ) {
			Array array = con.createArrayOf("NUMERIC", props.getAccumulating());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}
		if ( props != null && props.getStatusLength() > 0 ) {
			Array array = con.createArrayOf("TEXT", props.getStatus());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}
		if ( props != null && props.getTagsLength() > 0 ) {
			Array array = con.createArrayOf("TEXT", props.getTags());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}

		return stmt;
	}

}
