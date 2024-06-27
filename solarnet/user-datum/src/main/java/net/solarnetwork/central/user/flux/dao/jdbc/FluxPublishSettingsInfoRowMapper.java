/* ==================================================================
 * FluxPublishSettingsInfoRowMapper.java - 24/06/2024 9:45:00 am
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

package net.solarnetwork.central.user.flux.dao.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettingsInfo;

/**
 * Row mapper for {@link FluxPublishSettings} instances.
 *
 * <p>
 * The expected column order in the SQL results is:
 * </p>
 *
 * <ol>
 * <li>publish (BOOLEAN)</li>
 * <li>retain (BOOLEAN)</li>
 * </ol>
 * 
 * @author matt
 * @version 1.0
 */
public class FluxPublishSettingsInfoRowMapper implements RowMapper<FluxPublishSettings> {

	/** A default instance. */
	public static final RowMapper<FluxPublishSettings> INSTANCE = new FluxPublishSettingsInfoRowMapper();

	private final int columnOffset;

	/**
	 * Constructor.
	 * 
	 * <p>
	 * Uses a column offset value of {@literal 0}.
	 * </p>
	 */
	public FluxPublishSettingsInfoRowMapper() {
		this(0);
	}

	/**
	 * Constructor.
	 * 
	 * @param columnOffset
	 *        a column offset to use
	 */
	public FluxPublishSettingsInfoRowMapper(int columnOffset) {
		super();
		this.columnOffset = columnOffset;
	}

	@Override
	public FluxPublishSettings mapRow(final ResultSet rs, final int rowNum) throws SQLException {
		final boolean publish = rs.getBoolean(columnOffset + 1);
		final boolean retain = rs.getBoolean(columnOffset + 2);
		if ( publish && retain ) {
			return FluxPublishSettingsInfo.PUBLISH_RETAINED;
		} else if ( publish ) {
			return FluxPublishSettingsInfo.PUBLISH_NOT_RETAINED;
		} else if ( retain ) {
			return FluxPublishSettingsInfo.RETAINED;
		}
		return FluxPublishSettingsInfo.NOT_PUBLISHED;
	}

}
