/* ==================================================================
 * DatumExportProperties.java - 17/04/2018 9:41:27 AM
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

package net.solarnetwork.central.reg.web.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * DTO for datum export configuration.
 * 
 * @author matt
 * @version 1.0
 * @since 1.26
 */
@JsonIgnoreProperties({ "dataConfiguration", "destinationConfiguration", "outputConfiguration" })
public class DatumExportProperties extends UserDatumExportConfiguration {

	private static final long serialVersionUID = 6553837696183586118L;

	private Long dataConfigurationId;
	private Long destinationConfigurationId;
	private Long outputConfigurationId;

	public DatumExportProperties() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the configuration to copy the properties from
	 */
	public DatumExportProperties(UserDatumExportConfiguration other) {
		super();
		setCreated(other.getCreated());
		setId(other.getId());
		setUserId(other.getUserId());

		setHourDelayOffset(other.getHourDelayOffset());
		setName(other.getName());
		setSchedule(other.getSchedule());
		setMinimumExportDate(other.getMinimumExportDate());
		setTimeZoneId(other.getTimeZoneId());

		setUserDataConfiguration(other.getUserDataConfiguration());
		setUserDestinationConfiguration(other.getUserDestinationConfiguration());
		setUserOutputConfiguration(other.getUserOutputConfiguration());

		setDataConfigurationId(getUserDataConfigurationId());
		setDestinationConfigurationId(getUserDestinationConfigurationId());
		setOutputConfigurationId(getUserOutputConfigurationId());
	}

	public Long getDataConfigurationId() {
		return dataConfigurationId;
	}

	public void setDataConfigurationId(Long userDataConfigurationId) {
		this.dataConfigurationId = userDataConfigurationId;
	}

	public Long getDestinationConfigurationId() {
		return destinationConfigurationId;
	}

	public void setDestinationConfigurationId(Long userDestinationConfigurationId) {
		this.destinationConfigurationId = userDestinationConfigurationId;
	}

	public Long getOutputConfigurationId() {
		return outputConfigurationId;
	}

	public void setOutputConfigurationId(Long userOutputConfigurationId) {
		this.outputConfigurationId = userOutputConfigurationId;
	}

}
