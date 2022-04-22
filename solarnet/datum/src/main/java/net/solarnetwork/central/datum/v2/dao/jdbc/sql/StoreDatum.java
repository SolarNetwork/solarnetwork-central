/* ==================================================================
 * StoreNodeDatum.java - 21/11/2020 3:24:05 pm
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
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.domain.datum.DatumProperties;

/**
 * Store a {@link GeneralNodeDatum} via the {@code solardatm.store_datum} stored
 * procedure.
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
 * @since 1.2
 */
public class StoreDatum implements CallableStatementCreator, SqlProvider {

	private final DatumEntity datum;
	private final UUID streamId;
	private final Instant timestamp;
	private final Instant received;

	/**
	 * Constructor.
	 * 
	 * @param datum
	 *        the datum to store
	 */
	public StoreDatum(DatumEntity datum) {
		super();
		this.datum = requireNonNullArgument(datum, "datum");
		this.streamId = requireNonNullArgument(datum.getStreamId(), "streamId");
		this.timestamp = requireNonNullArgument(datum.getTimestamp(), "timestamp");
		this.received = (datum.getReceived() != null ? datum.getReceived() : Instant.now());
	}

	@Override
	public String getSql() {
		return "{call solardatm.store_stream_datum(?,?,?,?,?,?,?)}";
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		final CallableStatement stmt = con.prepareCall(getSql());
		stmt.setObject(1, this.streamId, Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(this.timestamp));
		stmt.setTimestamp(3, Timestamp.from(this.received));

		final DatumProperties p = datum.getProperties();

		if ( p == null || p.getInstantaneousLength() < 1 ) {
			stmt.setNull(4, Types.OTHER);
		} else {
			Array a = con.createArrayOf("NUMERIC", p.getInstantaneous());
			stmt.setArray(4, a);
			a.free();
		}

		if ( p == null || p.getAccumulatingLength() < 1 ) {
			stmt.setNull(5, Types.OTHER);
		} else {
			Array a = con.createArrayOf("NUMERIC", p.getAccumulating());
			stmt.setArray(5, a);
			a.free();
		}

		if ( p == null || p.getStatusLength() < 1 ) {
			stmt.setNull(6, Types.OTHER);
		} else {
			Array a = con.createArrayOf("TEXT", p.getStatus());
			stmt.setArray(6, a);
			a.free();
		}

		if ( p == null || p.getTagsLength() < 1 ) {
			stmt.setNull(7, Types.OTHER);
		} else {
			Array a = con.createArrayOf("TEXT", p.getTags());
			stmt.setArray(7, a);
			a.free();
		}

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

	/**
	 * Get the datum received timestamp.
	 * 
	 * @return the received timestamp computed for the datum
	 */
	public Instant getReceived() {
		return received;
	}

}
