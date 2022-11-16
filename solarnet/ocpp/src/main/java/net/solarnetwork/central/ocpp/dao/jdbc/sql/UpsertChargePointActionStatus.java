/* ==================================================================
 * UpsertChargePointActionStatus.java - 16/11/2022 5:44:19 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.ocpp.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.ocpp.domain.ChargePointActionStatus;

/**
 * Update a {@link ChargePointActionStatus} entity, creating it if not already
 * present.
 * 
 * <p>
 * This instance can be re-used by a single thread by passing in a mutable list
 * to {@link #UpsertChargePointActionStatus(List)}. Call
 * {@link #createPreparedStatement(Connection)} to create the
 * {@code PreparedStatement}. Then after each call to
 * {@link #setValues(PreparedStatement)} call
 * {@link PreparedStatement#execute()} and then you can change the list contents
 * and repeat the
 * {@link #setValues(PreparedStatement)}/{@link PreparedStatement#execute()}
 * calls again.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class UpsertChargePointActionStatus implements PreparedStatementCreator, SqlProvider,
		BatchPreparedStatementSetter, PreparedStatementSetter {

	private final List<ChargePointActionStatus> statuses;
	private final boolean batchMode;

	/**
	 * Constructor.
	 * 
	 * @param statuses
	 *        the statuses
	 */
	public UpsertChargePointActionStatus(ChargePointActionStatus statuses) {
		this(Collections.singletonList(requireNonNullArgument(statuses, "statuses")), false);
	}

	/**
	 * Constructor.
	 * 
	 * @param statuses
	 *        the statuses
	 */
	public UpsertChargePointActionStatus(List<ChargePointActionStatus> statuses) {
		this(statuses, true);
	}

	/**
	 * Constructor.
	 * 
	 * @param statuses
	 *        the statuses
	 * @param batchMode
	 *        {@literal true} to support batch mode
	 */
	private UpsertChargePointActionStatus(List<ChargePointActionStatus> statuses, boolean batchMode) {
		super();
		this.statuses = requireNonNullArgument(statuses, "statuses");
		this.batchMode = batchMode;
	}

	@Override
	public int getBatchSize() {
		return statuses.size();
	}

	@Override
	public String getSql() {
		return """
				INSERT INTO solarev.ocpp_charge_point_action_status (user_id, cp_id, conn_id, action, ts)
				VALUES (?, ?, ?, ?)
				ON CONFLICT (user_id, cp, conn_id, action) DO UPDATE
				SET ts = EXCLUDED.ts
				""";
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		if ( !batchMode ) {
			setValues(stmt, 0);
		}
		return stmt;
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		setValues(ps, 0);
	}

	@Override
	public void setValues(PreparedStatement ps, int i) throws SQLException {
		ChargePointActionStatus status = statuses.get(i);
		ps.setLong(1, status.getUserId());
		ps.setLong(2, status.getChargePointId());
		ps.setInt(3, status.getConnectorId());
		ps.setString(4, status.getAction());
		ps.setTimestamp(5, Timestamp.from(status.getTimestamp()));
	}

}
