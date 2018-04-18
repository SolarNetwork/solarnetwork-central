/* ==================================================================
 * BasicConfiguration.java - 21/03/2018 1:33:51 PM
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

package net.solarnetwork.central.datum.export.domain;

import java.io.Serializable;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic implementation of {@link Configuration}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class BasicConfiguration implements Configuration, Serializable {

	private static final long serialVersionUID = 8180339992339193694L;

	private String name;
	private DataConfiguration dataConfiguration;
	private OutputConfiguration outputConfiguration;
	private DestinationConfiguration destinationConfiguration;
	private ScheduleType schedule;
	private int hourDelayOffset;

	/**
	 * Default constructor.
	 */
	public BasicConfiguration() {
		super();
	}

	/**
	 * Construct with values.
	 * 
	 * @param name
	 *        the name
	 * @param schedule
	 *        the schedule
	 * @param hourDelayOffset
	 *        the offset
	 */
	public BasicConfiguration(String name, ScheduleType schedule, int hourDelayOffset) {
		super();
		setName(name);
		setSchedule(schedule);
		setHourDelayOffset(hourDelayOffset);
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *        the configuration to copy
	 */
	public BasicConfiguration(Configuration other) {
		super();
		if ( other == null ) {
			return;
		}
		this.name = other.getName();
		this.dataConfiguration = other.getDataConfiguration();
		this.outputConfiguration = other.getOutputConfiguration();
		this.destinationConfiguration = other.getDestinationConfiguration();
		this.schedule = other.getSchedule();
		this.hourDelayOffset = other.getHourDelayOffset();
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public DataConfiguration getDataConfiguration() {
		return dataConfiguration;
	}

	@JsonDeserialize(as = BasicDataConfiguration.class)
	public void setDataConfiguration(DataConfiguration dataConfiguration) {
		this.dataConfiguration = dataConfiguration;
	}

	@Override
	public OutputConfiguration getOutputConfiguration() {
		return outputConfiguration;
	}

	@JsonDeserialize(as = BasicOutputConfiguration.class)
	public void setOutputConfiguration(OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	@Override
	public DestinationConfiguration getDestinationConfiguration() {
		return destinationConfiguration;
	}

	@JsonDeserialize(as = BasicDestinationConfiguration.class)
	public void setDestinationConfiguration(DestinationConfiguration destinationConfiguration) {
		this.destinationConfiguration = destinationConfiguration;
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

}
