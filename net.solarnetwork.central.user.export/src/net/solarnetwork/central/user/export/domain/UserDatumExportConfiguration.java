/* ==================================================================
 * UserDatumExportConfiguration.java - 21/03/2018 11:13:14 AM
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

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.datum.domain.export.Configuration;
import net.solarnetwork.central.datum.domain.export.DataConfiguration;
import net.solarnetwork.central.datum.domain.export.DestinationConfiguration;
import net.solarnetwork.central.datum.domain.export.OutputConfiguration;
import net.solarnetwork.central.datum.domain.export.ScheduleType;

/**
 * User related {@link Configuration} entity.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDatumExportConfiguration extends net.solarnetwork.central.domain.BaseEntity
		implements Configuration, Serializable {

	private static final long serialVersionUID = -1797774828456852275L;

	private Long userId;
	private String name;
	private ScheduleType schedule;
	private int hourDelayOffset;
	private UserDataConfiguration userDataConfiguration;
	private UserOutputConfiguration userOutputConfiguration;
	private UserDestinationConfiguration userDestinationConfiguration;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public ScheduleType getSchedule() {
		return schedule;
	}

	public void setSchedule(ScheduleType schedule) {
		this.schedule = schedule;
	}

	@Override
	public int getHourDelayOffset() {
		return hourDelayOffset;
	}

	public void setHourDelayOffset(int hourDelayOffset) {
		this.hourDelayOffset = hourDelayOffset;
	}

	@JsonIgnore
	public UserDataConfiguration getUserDataConfiguration() {
		return userDataConfiguration;
	}

	public void setUserDataConfiguration(UserDataConfiguration userDataConfiguration) {
		this.userDataConfiguration = userDataConfiguration;
	}

	@Override
	public DataConfiguration getDataConfiguration() {
		return getUserDataConfiguration();
	}

	@JsonIgnore
	public UserOutputConfiguration getUserOutputConfiguration() {
		return userOutputConfiguration;
	}

	public void setUserOutputConfiguration(UserOutputConfiguration userOutputConfiguration) {
		this.userOutputConfiguration = userOutputConfiguration;
	}

	@Override
	public OutputConfiguration getOutputConfiguration() {
		return getUserOutputConfiguration();
	}

	@JsonIgnore
	public UserDestinationConfiguration getUserDestinationConfiguration() {
		return userDestinationConfiguration;
	}

	public void setUserDestinationConfiguration(
			UserDestinationConfiguration userDestinationConfiguration) {
		this.userDestinationConfiguration = userDestinationConfiguration;
	}

	@Override
	public DestinationConfiguration getDestinationConfiguration() {
		return getUserDestinationConfiguration();
	}

}
