/* ==================================================================
 * BaseExternalSystemConfigurationRowMapper.java - 19/08/2022 5:09:53 pm
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
import java.time.Instant;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.common.dao.jdbc.ColumnCountProvider;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.codec.JsonUtils;

/**
 * Base RowMapper for external system configuration entities.
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
 * <li>fp_id (BIGINT)</li>
 * <li>reg_status (SMALLINT)</li>
 * <li>cname (TEXT)</li>
 * <li>url (TEXT)</li>
 * <li>oscp_ver (TEXT)</li>
 * <li>heartbeat_secs (SMALLINT)</li>
 * <li>meas_styles (SMALLINT[])</li>
 * <li>heartbeat_at (TIMESTAMP)</li>
 * <li>offline_at (TIMESTAMP)</li>
 * <li>sprops (TEXT)</li>
 * </ol>
 * 
 * @param <C>
 *        the configuration type
 * @author matt
 * @version 1.0
 */
public abstract class BaseExternalSystemConfigurationRowMapper<C extends BaseOscpExternalSystemConfiguration<C>>
		implements RowMapper<C>, ColumnCountProvider {

	/** The number of columns mapped by this mapper. */
	public static final int COLUMN_COUNT = 15;

	@Override
	public final C mapRow(ResultSet rs, int rowNum) throws SQLException {
		Long entityId = rs.getObject(1, Long.class);
		Timestamp created = rs.getTimestamp(2);
		Long userId = rs.getObject(4, Long.class);
		C conf = createConfiguration(userId, entityId, created.toInstant());
		populateConfiguration(rs, rowNum, conf);
		return conf;
	}

	/**
	 * Create a new entity.
	 * 
	 * @param userId
	 *        the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @return the new entity
	 */
	protected abstract C createConfiguration(Long userId, Long entityId, Instant created);

	/**
	 * Populate the configuration entity from a row.
	 * 
	 * @param rs
	 *        the result set
	 * @param rowNum
	 *        the row number
	 * @param conf
	 *        the configuration to populate
	 * @throws SQLException
	 *         if any SQL error occurs
	 */
	protected void populateConfiguration(ResultSet rs, int rowNum, C conf) throws SQLException {
		conf.setModified(rs.getTimestamp(3).toInstant());
		conf.setEnabled(rs.getBoolean(5));
		conf.setFlexibilityProviderId(rs.getObject(6, Long.class));
		conf.setRegistrationStatus(RegistrationStatus.forCode(rs.getInt(7)));
		conf.setName(rs.getString(8));
		conf.setBaseUrl(rs.getString(9));
		conf.setOscpVersion(rs.getString(10));

		Integer hbSecs = rs.getObject(11, Integer.class);
		Set<MeasurementStyle> styles = CommonJdbcUtils.getCodedValueSet(rs, 12, MeasurementStyle.class);
		conf.setSettings(new SystemSettings(hbSecs, styles));

		Timestamp ts = rs.getTimestamp(13);
		if ( ts != null ) {
			conf.setHeartbeatDate(ts.toInstant());
		}
		ts = rs.getTimestamp(14);
		if ( ts != null ) {
			conf.setOfflineDate(ts.toInstant());
		}

		conf.setServiceProps(JsonUtils.getStringMap(rs.getString(15)));
	}

	@Override
	public int getColumnCount() {
		return COLUMN_COUNT;
	}

}
