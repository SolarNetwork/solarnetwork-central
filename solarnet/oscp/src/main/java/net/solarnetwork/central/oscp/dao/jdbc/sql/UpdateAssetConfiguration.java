/* ==================================================================
 * InsertAssetConiguration.java - 12/08/2022 6:51:01 am
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

package net.solarnetwork.central.oscp.dao.jdbc.sql;

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareArrayParameter;
import static net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils.prepareCodedValue;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonSqlUtils;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.Phase;

/**
 * Update {@link AssetConfiguration} entities.
 * 
 * @author matt
 * @version 1.0
 */
public class UpdateAssetConfiguration implements PreparedStatementCreator, SqlProvider {

	private static final String SQL = """
			UPDATE solaruser.user_oscp_asset_conf SET
				  modified = ?
				, enabled = ?
				, cname = ?
				, cg_id = ?
				, node_id = ?
				, source_id = ?
				, category = ?
				, iprops = ?
				, iprops_unit = ?
				, iprops_mult = ?
				, iprops_phase = ?
				, eprops = ?
				, eprops_unit = ?
				, eprops_mult = ?
				, etype = ?
				, sprops = ?::jsonb
			WHERE user_id = ? AND id = ?
			""";

	private final UserLongPK id;
	private final AssetConfiguration entity;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the ID of the the entity to update
	 * @param entity
	 *        the entity data to update
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UpdateAssetConfiguration(UserLongPK id, AssetConfiguration entity) {
		super();
		this.id = requireNonNullArgument(id, "id");
		this.entity = requireNonNullArgument(entity, "entity");
		if ( !id.entityIdIsAssigned() ) {
			throw new IllegalArgumentException("Entity ID must be assigned");
		}
	}

	@Override
	public String getSql() {
		return SQL;
	}

	@Override
	public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
		PreparedStatement stmt = con.prepareStatement(getSql());
		int p = 0;
		stmt.setTimestamp(++p,
				Timestamp.from(entity.getModified() != null ? entity.getModified() : Instant.now()));
		stmt.setBoolean(++p, entity.isEnabled());
		stmt.setString(++p, entity.getName());
		stmt.setObject(++p, entity.getCapacityGroupId());
		stmt.setObject(++p, entity.getNodeId());
		stmt.setString(++p, entity.getSourceId());
		p = prepareCodedValue(stmt, p, entity.getCategory(), AssetCategory.Charging, false);
		p = prepareArrayParameter(con, stmt, p, entity.getInstantaneousPropertyNames(), true);
		p = prepareCodedValue(stmt, p, entity.getInstantaneousUnit(), MeasurementUnit.W, false);
		if ( entity.getInstantaneousMultiplier() != null ) {
			stmt.setBigDecimal(++p, entity.getInstantaneousMultiplier());
		} else {
			stmt.setNull(++p, Types.DECIMAL);
		}
		p = prepareCodedValue(stmt, p, entity.getInstantaneousPhase(), Phase.All, false);
		p = prepareArrayParameter(con, stmt, p, entity.getEnergyPropertyNames(), true);
		p = prepareCodedValue(stmt, p, entity.getEnergyUnit(), MeasurementUnit.Wh, false);
		if ( entity.getEnergyMultiplier() != null ) {
			stmt.setBigDecimal(++p, entity.getEnergyMultiplier());
		} else {
			stmt.setNull(++p, Types.DECIMAL);
		}
		p = prepareCodedValue(stmt, p, entity.getEnergyType(), EnergyType.Total, false);
		p = CommonSqlUtils.prepareJsonString(entity.getServiceProps(), stmt, p, true);

		stmt.setObject(++p, id.getUserId());
		stmt.setObject(++p, id.getEntityId());
		return stmt;
	}

}
