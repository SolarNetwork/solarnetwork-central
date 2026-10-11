/* ==================================================================
 * SelectGncLotSplits.java - 11/10/2026 2:48:41 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql;

import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getNumeric;
import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getString;
import static net.solarnetwork.central.user.billing.snf.gnucash.dao.jdbc.sql.GnucashSqlUtils.getTimestamp;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncLotSplit;
import net.solarnetwork.central.user.billing.snf.gnucash.domain.GncSplit;

/**
 * Select the splits in a GnuCash lot, with their transaction details.
 *
 * <p>
 * The splits are ordered by the post date of their transaction.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public final class SelectGncLotSplits implements PreparedStatementCreator, SqlProvider {

	/** A row mapper for the results. */
	public static final RowMapper<GncLotSplit> ROW_MAPPER = SelectGncLotSplits::mapRow;

	private final String schema;
	private final String lotGuid;

	/**
	 * Constructor.
	 *
	 * @param schema
	 *        the schema of the GnuCash tables
	 * @param lotGuid
	 *        the GUID of the lot
	 */
	public SelectGncLotSplits(String schema, String lotGuid) {
		super();
		this.schema = GnucashSqlUtils.schemaName(schema);
		this.lotGuid = requireNonNullArgument(lotGuid, "lotGuid");
	}

	@Override
	public String getSql() {
		return """
				SELECT s.guid, s.tx_guid, s.account_guid, s.memo, s.action
					, s.reconcile_state, s.reconcile_date, s.value_num, s.value_denom
					, s.quantity_num, s.quantity_denom, s.lot_guid
					, tt.string_val AS txn_type, t.post_date
				FROM {schema}.splits s
				INNER JOIN {schema}.transactions t ON t.guid = s.tx_guid
				LEFT OUTER JOIN {schema}.slots tt ON tt.obj_guid = t.guid
					AND tt.name = 'trans-txn-type'
				WHERE s.lot_guid = ?
				ORDER BY t.post_date, s.guid
				""".replace("{schema}", schema);
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		final PreparedStatement stmt = con.prepareStatement(getSql());
		stmt.setString(1, lotGuid);
		return stmt;
	}

	private static GncLotSplit mapRow(ResultSet rs, int rowNum) throws SQLException {
		final Instant reconciled = getTimestamp(rs, 7);
		final GncSplit split = new GncSplit(rs.getString(1), rs.getString(2), rs.getString(3),
				getString(rs, 4), getString(rs, 5), rs.getString(6),
				(reconciled != null ? reconciled : Instant.EPOCH), getNumeric(rs, 8),
				getNumeric(rs, 10), rs.getString(12));
		return new GncLotSplit(split, rs.getString(13),
				nonnull(getTimestamp(rs, 14), "Transaction post date"));
	}

}
