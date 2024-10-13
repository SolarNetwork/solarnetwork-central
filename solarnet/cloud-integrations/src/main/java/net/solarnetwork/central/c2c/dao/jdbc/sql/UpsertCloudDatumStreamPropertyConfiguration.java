/* ==================================================================
 * UpsertCloudDatumStreamPropertyConfiguration.java - 4/10/2024 9:02:53 am
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

package net.solarnetwork.central.c2c.dao.jdbc.sql;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;

/**
 * Support for INSERT ... ON CONFLICT
 * {@link CloudDatumStreamPropertyConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public class UpsertCloudDatumStreamPropertyConfiguration
		implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			INSERT INTO solardin.cin_datum_stream_prop (
				  created,modified,user_id,ds_id,idx,enabled
				, ptype,pname,vtype,vref,mult,scale
			)
			VALUES (
				  ?,?,?,?,?,?
				, ?,?,?,?,?,?)
			ON CONFLICT (user_id, ds_id, idx) DO UPDATE
				SET modified = COALESCE(EXCLUDED.modified, CURRENT_TIMESTAMP)
					, enabled = EXCLUDED.enabled
					, ptype = EXCLUDED.ptype
					, pname = EXCLUDED.pname
					, vtype = EXCLUDED.vtype
					, vref = EXCLUDED.vref
					, mult = EXCLUDED.mult
					, scale = EXCLUDED.scale
			""";

	private final Long userId;
	private final Long datumStreamId;
	private final Integer index;
	private final CloudDatumStreamPropertyConfiguration entity;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param datumStreamId
	 *        the datum stream ID
	 * @param entity
	 *        the entity
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpsertCloudDatumStreamPropertyConfiguration(Long userId, Long datumStreamId,
			CloudDatumStreamPropertyConfiguration entity) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
		this.datumStreamId = requireNonNullArgument(datumStreamId, "datumStreamId");
		this.index = requireNonNullArgument(entity.getIndex(), "entity.index");
		this.entity = requireNonNullArgument(entity, "entity");
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql(), Statement.NO_GENERATED_KEYS);
		Timestamp ts = Timestamp.from(entity.getCreated() != null ? entity.getCreated() : Instant.now());
		Timestamp mod = entity.getModified() != null ? Timestamp.from(entity.getModified()) : null;
		int p = 0;
		stmt.setTimestamp(++p, ts);
		stmt.setTimestamp(++p, mod);
		stmt.setObject(++p, userId);
		stmt.setObject(++p, datumStreamId);
		stmt.setObject(++p, index);
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getPropertyType().keyValue());
		stmt.setString(++p, entity.getPropertyName());
		stmt.setString(++p, entity.getValueType().keyValue());
		stmt.setString(++p, entity.getValueReference());
		stmt.setBigDecimal(++p, entity.getMultiplier());
		stmt.setObject(++p, entity.getScale());
		return stmt;
	}

}
