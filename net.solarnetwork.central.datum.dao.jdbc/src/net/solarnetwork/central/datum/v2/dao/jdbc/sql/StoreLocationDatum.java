/* ==================================================================
 * StoreLocationDatum.java - 21/11/2020 3:24:05 pm
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.domain.GeneralDatumSamples;
import net.solarnetwork.util.JsonUtils;

/**
 * Store a {@link GeneralLocationDatum} via the
 * {@code solardatm.store_loc_datum} stored procedure.
 * 
 * <p>
 * The statement registers an output parameter of type {@code OTHER} at index
 * {@literal 1}, representing the stream ID of the stored datum. The JDBC driver
 * should return an actual {@link java.util.UUID} object on the
 * {@code CallableStatement}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 3.8
 */
public class StoreLocationDatum implements CallableStatementCreator, SqlProvider {

	private final GeneralLocationDatum datum;
	private final Instant timestamp;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 */
	public StoreLocationDatum(GeneralLocationDatum datum) {
		super();
		if ( datum == null ) {
			throw new IllegalArgumentException("The datum argument must not be null.");
		}
		this.datum = datum;
		this.timestamp = (datum.getCreated() != null
				? Instant.ofEpochMilli(datum.getCreated().getMillis())
				: Instant.now());
	}

	@Override
	public String getSql() {
		return "{? = call solardatm.store_loc_datum(?,?,?,?,?)}";
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.registerOutParameter(1, Types.OTHER);
		final Timestamp now = Timestamp.from(Instant.now());
		stmt.setTimestamp(2, datum.getCreated() != null ? Timestamp.from(this.timestamp) : now);
		stmt.setObject(3, datum.getLocationId());
		stmt.setString(4, datum.getSourceId());
		stmt.setTimestamp(5, now);

		final GeneralDatumSamples s = datum.getSamples();
		String json = JsonUtils.getJSONString(s, null);
		stmt.setString(6, json);

		return stmt;
	}

	/**
	 * Get the datum timestamp.
	 * 
	 * @return the timestamp computed for the datum
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

}
