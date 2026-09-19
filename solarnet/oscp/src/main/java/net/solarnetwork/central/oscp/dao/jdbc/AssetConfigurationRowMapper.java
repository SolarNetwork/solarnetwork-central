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

import static net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils.timestampInstant;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
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
import net.solarnetwork.codec.jackson.JsonUtils;

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
		int p = 0;

		Long entityId = rs.getObject(++p, Long.class);
		Instant ts = timestampInstant(rs, ++p);
		Instant mod = timestampInstant(rs, ++p);
		Long userId = rs.getObject(++p, Long.class);
		boolean enabled = rs.getBoolean(++p);
		String name = rs.getString(++p);
		Long capacityGroupId = rs.getObject(++p, Long.class);
		String identifier = rs.getString(++p);
		OscpRole audience = OscpRole.forCode(rs.getInt(++p));
		Long nodeId = rs.getObject(++p, Long.class);
		String sourceId = rs.getString(++p);
		AssetCategory category = AssetCategory.forCode(rs.getInt(++p));

		final var conf = new AssetConfiguration(userId, entityId, ts, name, capacityGroupId, identifier,
				audience, nodeId, sourceId, category);
		conf.setModified(mod);
		conf.setEnabled(enabled);
		conf.setPhase(Phase.forCode(rs.getInt(++p)));

		String @Nullable [] propNames = CommonJdbcUtils.getArray(rs, ++p);
		Integer statType = rs.getObject(++p, Integer.class);
		Integer unit = rs.getObject(++p, Integer.class);
		@Nullable
		BigDecimal mult = rs.getBigDecimal(++p);

		if ( propNames != null && propNames.length > 0 && statType != null && unit != null ) {
			var inst = new AssetInstantaneousDatumConfiguration(propNames, MeasurementUnit.forCode(unit),
					StatisticType.forCode(statType));
			inst.setMultiplier(mult);
			conf.setInstantaneous(inst);
		}

		propNames = CommonJdbcUtils.getArray(rs, ++p);
		statType = rs.getObject(++p, Integer.class);
		unit = rs.getObject(++p, Integer.class);
		mult = rs.getBigDecimal(++p);
		Integer energyType = rs.getObject(++p, Integer.class);
		Integer energyDirection = rs.getObject(++p, Integer.class);

		if ( propNames != null && propNames.length > 0 && statType != null && unit != null
				&& energyType != null && energyDirection != null ) {
			var energy = new AssetEnergyDatumConfiguration(propNames, MeasurementUnit.forCode(unit),
					StatisticType.forCode(statType), EnergyType.forCode(energyType),
					EnergyDirection.forCode(energyDirection));
			energy.setMultiplier(mult);
			conf.setEnergy(energy);
		}

		conf.setServiceProps(JsonUtils.getStringMap(rs.getString(++p)));
		return conf;
	}

}
