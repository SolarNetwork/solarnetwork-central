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

import java.io.Serial;
import java.io.Serializable;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Basic implementation of {@link Configuration}.
 *
 * @author matt
 * @version 1.0
 * @since 1.23
 */
public class BasicConfiguration implements Configuration, Serializable {

	@Serial
	private static final long serialVersionUID = 5872632878013036272L;

	private @Nullable String name;
	private @Nullable DataConfiguration dataConfiguration;
	private @Nullable OutputConfiguration outputConfiguration;
	private @Nullable DestinationConfiguration destinationConfiguration;
	private @Nullable ScheduleType schedule;
	private int hourDelayOffset;
	private @Nullable String timeZoneId;

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
	public BasicConfiguration(@Nullable String name, @Nullable ScheduleType schedule,
			int hourDelayOffset) {
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
	public BasicConfiguration(@Nullable Configuration other) {
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
		this.timeZoneId = other.getTimeZoneId();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BasicConfiguration{");
		if ( name != null ) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if ( dataConfiguration != null ) {
			builder.append("dataConfiguration=");
			builder.append(dataConfiguration);
			builder.append(", ");
		}
		if ( outputConfiguration != null ) {
			builder.append("outputConfiguration=");
			builder.append(outputConfiguration);
			builder.append(", ");
		}
		if ( destinationConfiguration != null ) {
			builder.append("destinationConfiguration=");
			builder.append(destinationConfiguration);
			builder.append(", ");
		}
		if ( schedule != null ) {
			builder.append("schedule=");
			builder.append(schedule);
			builder.append(", ");
		}
		builder.append("hourDelayOffset=");
		builder.append(hourDelayOffset);
		builder.append(", ");
		if ( timeZoneId != null ) {
			builder.append("timeZoneId=");
			builder.append(timeZoneId);
		}
		builder.append("}");
		return builder.toString();
	}

	@Override
	public final @Nullable String getName() {
		return name;
	}

	public final void setName(@Nullable String name) {
		this.name = name;
	}

	@Override
	public final @Nullable DataConfiguration getDataConfiguration() {
		return dataConfiguration;
	}

	@JsonDeserialize(as = BasicDataConfiguration.class)
	public final void setDataConfiguration(@Nullable DataConfiguration dataConfiguration) {
		this.dataConfiguration = dataConfiguration;
	}

	@Override
	public final @Nullable OutputConfiguration getOutputConfiguration() {
		return outputConfiguration;
	}

	@JsonDeserialize(as = BasicOutputConfiguration.class)
	public final void setOutputConfiguration(@Nullable OutputConfiguration outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	@Override
	public final @Nullable DestinationConfiguration getDestinationConfiguration() {
		return destinationConfiguration;
	}

	@JsonDeserialize(as = BasicDestinationConfiguration.class)
	public final void setDestinationConfiguration(
			@Nullable DestinationConfiguration destinationConfiguration) {
		this.destinationConfiguration = destinationConfiguration;
	}

	@Override
	public final @Nullable ScheduleType getSchedule() {
		return schedule;
	}

	public final void setSchedule(@Nullable ScheduleType schedule) {
		this.schedule = schedule;
	}

	@Override
	public final int getHourDelayOffset() {
		return hourDelayOffset;
	}

	public final void setHourDelayOffset(int hourDelayOffset) {
		this.hourDelayOffset = hourDelayOffset;
	}

	@Override
	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	public final void setTimeZoneId(@Nullable String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

}
