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

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;

/**
 * DTO for datum export configuration.
 *
 * @author matt
 * @version 1.1
 * @since 1.26
 */
@JsonIgnoreProperties({ "dataConfiguration", "destinationConfiguration", "outputConfiguration" })
public final class DatumExportProperties {

	private Long userId;
	private Long id;
	private Instant created;
	private String name;
	private ScheduleType schedule;
	private int hourDelayOffset;
	private Instant minimumExportDate;
	private String timeZoneId;
	private String tokenId;

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
		setId(other.getConfigId());
		setUserId(other.getUserId());

		setHourDelayOffset(other.getHourDelayOffset());
		setName(other.getName());
		setSchedule(other.getSchedule());
		setMinimumExportDate(other.getMinimumExportDate());
		setTimeZoneId(other.getTimeZoneId());

		setDataConfigurationId(other.getUserDataConfigurationId());
		setDestinationConfigurationId(other.getUserDestinationConfigurationId());
		setOutputConfigurationId(other.getUserOutputConfigurationId());
	}

	/**
	 * Get the user ID.
	 *
	 * @return the user ID
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 *
	 * @param userId
	 *        the user ID to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Get the creation date.
	 *
	 * @return the creation date
	 */
	public Instant getCreated() {
		return created;
	}

	/**
	 * Set the creation date.
	 *
	 * @param created
	 *        the date to set
	 */
	public void setCreated(Instant created) {
		this.created = created;
	}

	/**
	 * Get the name.
	 *
	 * @return the name to set
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public ScheduleType getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule.
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public void setSchedule(ScheduleType schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the schedule type key value.
	 *
	 * @return the schedule type; if {@link #getSchedule()} is {@literal null}
	 *         this will return the key value for {@link ScheduleType#Daily}
	 */
	public char getScheduleKey() {
		ScheduleType type = getSchedule();
		return (type != null ? type.getKey() : ScheduleType.Daily.getKey());
	}

	/**
	 * Set the schedule type via its key value.
	 *
	 * @param key
	 *        the key of the schedule type to set; if {@code key} is
	 *        unsupported, the compression will be set to
	 *        {@link ScheduleType#Daily}
	 */
	public void setScheduleKey(char key) {
		ScheduleType type = ScheduleType.Daily;
		try {
			type = ScheduleType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}
		setSchedule(type);
	}

	/**
	 * Get the hour delay offset.
	 *
	 * @return the offset
	 */
	public int getHourDelayOffset() {
		return hourDelayOffset;
	}

	/**
	 * Set the hour delay offset.
	 *
	 * @param hourDelayOffset
	 *        the offset to set
	 */
	public void setHourDelayOffset(int hourDelayOffset) {
		this.hourDelayOffset = hourDelayOffset;
	}

	/**
	 * Get the data configuration ID.
	 *
	 * @return the ID
	 */
	public Long getDataConfigurationId() {
		return dataConfigurationId;
	}

	/**
	 * Set the data configuration ID.
	 *
	 * @param userDataConfigurationId
	 *        the ID to set
	 */
	public void setDataConfigurationId(Long userDataConfigurationId) {
		this.dataConfigurationId = userDataConfigurationId;
	}

	/**
	 * Get the destination configuration ID.
	 *
	 * @return the ID
	 */
	public Long getDestinationConfigurationId() {
		return destinationConfigurationId;
	}

	/**
	 * Set the destination configuration ID.
	 *
	 * @param userDestinationConfigurationId
	 *        the ID to set
	 */
	public void setDestinationConfigurationId(Long userDestinationConfigurationId) {
		this.destinationConfigurationId = userDestinationConfigurationId;
	}

	/**
	 * Get the output configuration ID.
	 *
	 * @return the ID
	 */
	public Long getOutputConfigurationId() {
		return outputConfigurationId;
	}

	/**
	 * Set the output configuration ID.
	 *
	 * @param userOutputConfigurationId
	 *        the ID to set
	 */
	public void setOutputConfigurationId(Long userOutputConfigurationId) {
		this.outputConfigurationId = userOutputConfigurationId;
	}

	/**
	 * Get the minimum export date that can be scheduled for execution.
	 *
	 * @return the minimum export date
	 */
	public Instant getMinimumExportDate() {
		return minimumExportDate;
	}

	/**
	 * Set the minimum export date that can be scheduled for execution.
	 *
	 * <p>
	 * This date will be updated over time as export tasks complete. It
	 * represents the minimum export date that can be scheduled for future
	 * export tasks, so that we can know at what date the next scheduled export
	 * task should use for its export date.
	 * </p>
	 *
	 * @param minimumExportDate
	 *        the minimum export date to use
	 */
	public void setMinimumExportDate(Instant minimumExportDate) {
		this.minimumExportDate = minimumExportDate;
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the ID
	 */
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 *
	 * @param timeZoneId
	 *        the ID to set
	 */
	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	/**
	 * Get the token ID.
	 *
	 * @return the token ID
	 */
	public String getTokenId() {
		return tokenId;
	}

	/**
	 * Set the token ID.
	 *
	 * @param tokenId
	 *        the token ID to set
	 */
	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

}
