/* ==================================================================
 * StoreGeneralObjectDatum.java - 11/10/2024 8:56:26 am
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

package net.solarnetwork.central.datum.v2.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Map.Entry;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Store a {@link GeneralObjectDatum}.
 *
 * <p>
 * Note that non-finite number values will be dropped from the JSON string
 * generated for the sample properties. This is because they cannot be handled
 * as {@code java.math.BigDecimal} values when later read back from the
 * database.
 * </p>
 *
 * @author matt
 * @version 1.1
 */
public final class StoreGeneralObjectDatum implements CallableStatementCreator, SqlProvider {

	/** The SQL to store node datum. */
	public static final String STORE_NODE_DATUM_SQL = "{? = call solardatm.store_datum(?,?,?,?,?)}";

	/** The SQL to store location datum. */
	public static final String STORE_LOC_DATUM_SQL = "{? = call solardatm.store_loc_datum(?,?,?,?,?)}";

	private final GeneralObjectDatum<?> datum;
	private final Instant timestamp;

	/**
	 * Constructor.
	 *
	 * @param datum
	 *        the datum to store
	 */
	public StoreGeneralObjectDatum(GeneralObjectDatum<?> datum) {
		super();
		this.datum = requireNonNullArgument(datum, "datum");
		this.timestamp = (datum.getCreated() != null ? datum.getCreated() : Instant.now());
	}

	@Override
	public String getSql() {
		return datum.getId().getKind() == ObjectDatumKind.Location ? STORE_LOC_DATUM_SQL
				: STORE_NODE_DATUM_SQL;
	}

	@Override
	public CallableStatement createCallableStatement(Connection con) throws SQLException {
		CallableStatement stmt = con.prepareCall(getSql());
		stmt.registerOutParameter(1, Types.OTHER);
		stmt.setTimestamp(2, Timestamp.from(this.timestamp));
		stmt.setObject(3, datum.getId().getObjectId());
		stmt.setString(4, datum.getId().getSourceId());
		stmt.setTimestamp(5, Timestamp.from(Instant.now()));

		final DatumSamples s = samplesWithOnlyFiniteNumbers(datum.getSamples());

		String json = JsonUtils.getJSONString(s, null);
		stmt.setString(6, json);

		return stmt;
	}

	/**
	 * Remove all non-finite numbers because they are not supported by
	 * BigDecimal when read back from DB.
	 *
	 * @param s
	 *        the samples to inspect
	 * @return {@code samples} or a copy with non-finite numbers removed
	 */
	private DatumSamples samplesWithOnlyFiniteNumbers(DatumSamples s) {
		if ( s == null ) {
			return null;
		}
		DatumSamples copy = null;
		if ( s.getInstantaneous() != null ) {
			for ( Entry<String, Number> e : s.getInstantaneous().entrySet() ) {
				Number n = e.getValue();
				if ( !numberIsFinite(n) ) {
					if ( copy == null ) {
						copy = new DatumSamples(s);
					}
					copy.putInstantaneousSampleValue(e.getKey(), null);
				}
			}
			if ( copy != null && copy.getInstantaneous().isEmpty() ) {
				copy.setInstantaneous(null);
			}
		}
		if ( s.getAccumulating() != null ) {
			for ( Entry<String, Number> e : s.getAccumulating().entrySet() ) {
				Number n = e.getValue();
				if ( !numberIsFinite(n) ) {
					if ( copy == null ) {
						copy = new DatumSamples(s);
					}
					copy.putAccumulatingSampleValue(e.getKey(), null);
				}
			}
			if ( copy != null && copy.getAccumulating().isEmpty() ) {
				copy.setAccumulating(null);
			}
		}
		if ( s.getStatus() != null ) {
			for ( Entry<String, Object> e : s.getStatus().entrySet() ) {
				Object o = e.getValue();
				if ( o instanceof Number n && !numberIsFinite(n) ) {
					if ( copy == null ) {
						copy = new DatumSamples(s);
					}
					copy.putStatusSampleValue(e.getKey(), null);
				}
			}
			if ( copy != null && copy.getStatus().isEmpty() ) {
				copy.setStatus(null);
			}
		}
		return (copy != null ? copy : s);
	}

	private boolean numberIsFinite(Number n) {
		return switch (n) {
			case Float f -> Float.isFinite(f);
			case Double d -> Double.isFinite(d);
			case null -> true;
			default -> true;
		};
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
