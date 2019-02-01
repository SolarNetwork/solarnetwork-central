/* ==================================================================
 * UserDatumExportTaskInfo.java - 18/04/2018 9:13:54 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;

/**
 * Entity for user-specific datum export tasks.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDatumExportTaskInfo extends UserAdhocDatumExportTaskInfo {

	private static final long serialVersionUID = -3064516340043437770L;

	private Long userDatumExportConfigurationId;

	@Override
	public String toString() {
		return "UserDatumExportTaskInfo{userId=" + getUserId() + ",date=" + getExportDate()
				+ ",scheduleType=" + getScheduleType() + ",taskId=" + getTaskId() + ",configId="
				+ userDatumExportConfigurationId + "}";
	}

	/**
	 * Get the related {@link UserDatumExportConfiguration#getId()} value.
	 * 
	 * @return the user configuration ID
	 */
	public Long getUserDatumExportConfigurationId() {
		return userDatumExportConfigurationId;
	}

	/**
	 * Set the related {@link UserDatumExportConfiguration#getId()} value.
	 * 
	 * @param userDatumExportConfigurationId
	 *        the user configuration ID to set
	 */
	public void setUserDatumExportConfigurationId(Long userDatumExportConfigurationId) {
		this.userDatumExportConfigurationId = userDatumExportConfigurationId;
	}

	/**
	 * Set the configuration.
	 * 
	 * <p>
	 * If {@code config} is a {@link UserDatumExportConfiguration} then
	 * {@link #setUserDatumExportConfigurationId(Long)} will be invoked with the
	 * value's {@link UserDatumExportConfiguration#getId()}.
	 * </p>
	 * 
	 * @param config
	 */
	@Override
	@JsonDeserialize(as = BasicConfiguration.class)
	public void setConfig(Configuration config) {
		super.setConfig(config);
		if ( config instanceof UserDatumExportConfiguration ) {
			setUserDatumExportConfigurationId(((UserDatumExportConfiguration) config).getId());
		}
	}

}
