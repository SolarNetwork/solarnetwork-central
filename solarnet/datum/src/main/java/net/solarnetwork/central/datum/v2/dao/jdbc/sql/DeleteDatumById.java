/* ==================================================================
 * DeleteDatumById.java - 12/02/2025 5:57:52 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;

/**
 * Delete datum entities by ID.
 *
 * @author matt
 * @version 1.0
 */
public class DeleteDatumById
		implements PreparedStatementCreator, SqlProvider, BatchPreparedStatementSetter {

	/** The SQL statement used. */
	public static final String SQL = """
			SELECT stream_id, ts, agg_kind, obj_id, source_id, kind
			FROM solardatm.delete_datm(?, ?, ?::UUID, ?, ?, ?)
			""";

	private final Long userId;
	private final List<ObjectDatumId> ids;
	private final boolean trackStale;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID that owns all referenced datum
	 * @param ids
	 *        the IDs of the datum to delete
	 * @param trackStale
	 *        {@code true} to track stale datum rows
	 */
	public DeleteDatumById(Long userId, List<ObjectDatumId> ids, boolean trackStale) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.ids = requireNonNullArgument(ids, "ids");
		this.trackStale = trackStale;
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement ps = con.prepareStatement(getSql());
		// set constant batch parameters
		ps.setObject(1, userId);
		ps.setBoolean(6, trackStale);
		return ps;
	}

	@Override
	public int getBatchSize() {
		return ids.size();
	}

	@Override
	public void setValues(PreparedStatement ps, int i) throws SQLException {
		ObjectDatumId id = ids.get(i);
		ps.setTimestamp(2, Timestamp.from(id.getTimestamp()));
		if ( id.getStreamId() != null ) {
			ps.setString(3, id.getStreamId().toString());
		} else {
			ps.setNull(3, Types.VARCHAR);
		}
		if ( id.getObjectId() != null ) {
			ps.setObject(4, id.getObjectId());
		} else {
			ps.setNull(4, Types.BIGINT);
		}
		if ( id.getSourceId() != null ) {
			ps.setString(5, id.getSourceId());
		} else {
			ps.setNull(5, Types.VARCHAR);
		}
	}

}
