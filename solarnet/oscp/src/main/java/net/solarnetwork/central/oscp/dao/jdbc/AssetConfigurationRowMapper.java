/* ==================================================================
 * AssetConfigurationRowMapper.java - 12/08/2022 4:09:13 pm
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

package net.solarnetwork.central.oscp.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.oscp.domain.AssetCategory;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.AssetEnergyDatumConfiguration;
import net.solarnetwork.central.oscp.domain.AssetInstantaneousDatumConfiguration;
import net.solarnetwork.central.oscp.domain.EnergyDirection;
import net.solarnetwork.central.oscp.domain.EnergyType;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.StatisticType;
import net.solarnetwork.codec.JsonUtils;

/**
 * Row mapper for {@link AssetConfiguration} entities.
 * 
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 * 
 * <ol>
 * <li>id (BIGINT)</li>
 * <li>created (TIMESTAMP)</li>
 * <li>modified (TIMESTAMP)</li>
 * <li>user_id (BIGINT)</li>
 * <li>enabled (BOOLEAN)</li>
 * <li>cname (TEXT)</li>
 * <li>cg_id (BIGINT)</li>
 * <li>ident (TEXT)</li>
 * <li>audience (SMALLINT)</li>
 * <li>node_id (BIGINT)</li>
 * <li>source_id (TEXT)</li>
 * <li>category (SMALLINT)</li>
 * <li>phase (SMALLINT)</li>
 * <li>iprops (TEXT[])</li>
 * <li>iprops_stat (SMALLINT)</li>
 * <li>iprops_unit (SMALLINT)</li>
 * <li>iprops_mult (DECIMAL)</li>
 * <li>eprops (TEXT[])</li>
 * <li>eprops_stat (SMALLINT)</li>
 * <li>eprops_unit (SMALLINT)</li>
 * <li>eprops_mult (DECIMAL)</li>
 * <li>etype (SMALLINT)</li>
 * <li>edir (SMALLINT)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class AssetConfigurationRowMapper implements RowMapper<AssetConfiguration> {

	/** A default instance. */
	public static final RowMapper<AssetConfiguration> INSTANCE = new AssetConfigurationRowMapper();

	@Override
	public AssetConfiguration mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long entityId = rs.getLong(1);
		Timestamp created = rs.getTimestamp(2);
		Long userId = rs.getLong(4);
		AssetConfiguration conf = new AssetConfiguration(userId, entityId, created.toInstant());
		conf.setModified(rs.getTimestamp(3).toInstant());
		conf.setEnabled(rs.getBoolean(5));
		conf.setName(rs.getString(6));
		conf.setCapacityGroupId(rs.getLong(7));
		conf.setIdentifier(rs.getString(8));
		conf.setAudience(OscpRole.forCode(rs.getInt(9)));
		conf.setNodeId(rs.getLong(10));
		conf.setSourceId(rs.getString(11));
		conf.setCategory(AssetCategory.forCode(rs.getInt(12)));
		conf.setPhase(Phase.forCode(rs.getInt(13)));

		AssetInstantaneousDatumConfiguration inst = new AssetInstantaneousDatumConfiguration();
		inst.setPropertyNames(CommonJdbcUtils.getArray(rs, 14));
		int c = rs.getInt(15);
		if ( !rs.wasNull() ) {
			inst.setStatisticType(StatisticType.forCode(c));
		}
		c = rs.getInt(16);
		if ( !rs.wasNull() ) {
			inst.setUnit(MeasurementUnit.forCode(c));
		}
		inst.setMultiplier(rs.getBigDecimal(17));
		if ( inst.getPropertyNames() != null ) {
			conf.setInstantaneous(inst);
		}

		AssetEnergyDatumConfiguration energy = new AssetEnergyDatumConfiguration();
		energy.setPropertyNames(CommonJdbcUtils.getArray(rs, 18));
		c = rs.getInt(19);
		if ( !rs.wasNull() ) {
			energy.setStatisticType(StatisticType.forCode(c));
		}
		c = rs.getInt(20);
		if ( !rs.wasNull() ) {
			energy.setUnit(MeasurementUnit.forCode(c));
		}
		energy.setMultiplier(rs.getBigDecimal(21));
		if ( energy.getPropertyNames() != null ) {
			conf.setInstantaneous(inst);
		}
		c = rs.getInt(22);
		if ( !rs.wasNull() ) {
			energy.setType(EnergyType.forCode(c));
		}
		c = rs.getInt(23);
		if ( !rs.wasNull() ) {
			energy.setDirection(EnergyDirection.forCode(c));
		}
		if ( energy.getPropertyNames() != null ) {
			conf.setEnergy(energy);
		}

		conf.setServiceProps(JsonUtils.getStringMap(rs.getString(24)));
		return conf;
	}

}
