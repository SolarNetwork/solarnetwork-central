/* ==================================================================
 * InsertAggregateDatum.java - 23/11/2020 11:08:09 am
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.dao.AggregateDatumEntity;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.DatumPropertiesStatistics;
import net.solarnetwork.central.domain.Aggregation;

/**
 * Insert an {@link AggregateDatumEntity} into the
 * {@literal solardatm.agg_datm_X} table.
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class InsertAggregateDatum implements PreparedStatementCreator, SqlProvider {

	private final AggregateDatumEntity datum;
	private final Aggregation aggregation;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 * @throws IllegalArgumentException
	 *         if {@code datum} is {@literal null}
	 */
	public InsertAggregateDatum(AggregateDatumEntity datum) {
		super();
		if ( datum == null ) {
			throw new IllegalArgumentException("The datum argument must not be null.");
		}
		this.datum = datum;
		if ( datum.getAggregation() == null ) {
			throw new IllegalArgumentException("The datum aggreation must not be null.");
		}
		switch (datum.getAggregation()) {
			case Hour:
			case Day:
			case Month:
				this.aggregation = datum.getAggregation();
				break;

			default:
				throw new IllegalArgumentException(
						"The " + datum.getAggregation() + " aggreation is not supported.");
		}
	}

	@Override
	public String getSql() {
		StringBuilder buf = new StringBuilder();
		buf.append("INSERT INTO solardatm.agg_datm_");
		switch (aggregation) {
			case Day:
				buf.append("daily");
				break;

			case Month:
				buf.append("monthly");
				break;

			default:
				buf.append("hourly");
				break;
		}
		buf.append(" (stream_id,ts_start,data_i,data_a,data_s,data_t,stat_i,read_a) ");
		buf.append(
				"VALUES (?::uuid,?,?::numeric[],?::numeric[],?::text[],?::text[],?::numeric[][],?::numeric[][])\n");
		buf.append("ON CONFLICT (stream_id, ts_start) DO UPDATE\n");
		buf.append("SET data_i = EXCLUDED.data_i\n");
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
		stmt.setTimestamp(++p, Timestamp.from(datum.getTimestamp()));

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

		DatumPropertiesStatistics stats = datum.getStatistics();
		if ( stats != null && stats.getInstantaneousLength() > 0 ) {
			Array array = con.createArrayOf("NUMERIC", stats.getInstantaneous());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}
		if ( stats != null && stats.getAccumulatingLength() > 0 ) {
			Array array = con.createArrayOf("NUMERIC", stats.getAccumulating());
			stmt.setArray(++p, array);
			array.free();
		} else {
			stmt.setNull(++p, Types.ARRAY);
		}

		return stmt;
	}

}
