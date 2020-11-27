/* ==================================================================
 * InsertDatumAuxiliary.java - 28/11/2020 8:57:20 am
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
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.util.JsonUtils;

/**
 * Insert a {@link DatumAuxiliaryEntity} into the
 * {@literal solardatm.da_datm_aux} table.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class InsertDatumAuxiliary implements PreparedStatementCreator, SqlProvider {

	private final DatumAuxiliaryEntity datum;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 * @throws IllegalArgumentException
	 *         if {@code datum} is {@literal null}
	 */
	public InsertDatumAuxiliary(DatumAuxiliaryEntity datum) {
		super();
		if ( datum == null ) {
			throw new IllegalArgumentException("The datum argument must not be null.");
		}
		this.datum = datum;
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("INSERT INTO solardatm.da_datm_aux ")
				.append("(stream_id, ts, atype, notes, jdata_af, jdata_as, jmeta)\n");
		buf.append("VALUES (?, ?, ?::solardatm.da_datm_aux_type, ?, ?::jsonb, ?::jsonb, ?::jsonb)\n");
		buf.append("ON CONFLICT (stream_id, ts, atype) DO UPDATE\n");
		buf.append("SET updated = CURRENT_TIMESTAMP\n");
		buf.append("	, notes = EXCLUDED.notes\n");
		buf.append("	, jdata_af = EXCLUDED.jdata_af\n");
		buf.append("	, jdata_as = EXCLUDED.jdata_as\n");
		buf.append("	, jmeta = EXCLUDED.jmeta");
		return buf.toString();
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());

		stmt.setObject(1, datum.getStreamId(), Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(datum.getTimestamp()));
		stmt.setString(3, datum.getType().name());
		stmt.setString(4, datum.getNotes());

		stmt.setString(5, JsonUtils.getJSONString(
				datum.getSamplesFinal() != null ? datum.getSamplesFinal().getAccumulating() : null,
				null));

		stmt.setString(6, JsonUtils.getJSONString(
				datum.getSamplesStart() != null ? datum.getSamplesStart().getAccumulating() : null,
				null));

		stmt.setString(7, JsonUtils.getJSONString(datum.getMetadata(), null));

		return stmt;
	}

}
