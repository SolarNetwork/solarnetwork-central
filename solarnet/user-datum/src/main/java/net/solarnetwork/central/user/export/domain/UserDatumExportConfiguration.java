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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.lang.NonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.BaseEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DataConfiguration;
import net.solarnetwork.central.datum.export.domain.DestinationConfiguration;
import net.solarnetwork.central.datum.export.domain.OutputConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * User related {@link Configuration} entity.
 * 
 * @author matt
 * @version 2.0
 */
public class UserDatumExportConfiguration extends BaseEntity
		implements Configuration, UserRelatedEntity<Long>, Serializable {

	private static final long serialVersionUID = 7392841763656888488L;

	private Long userId;
	private String name;
	private ScheduleType schedule;
	private int hourDelayOffset;
	private UserDataConfiguration userDataConfiguration;
	private UserOutputConfiguration userOutputConfiguration;
	private UserDestinationConfiguration userDestinationConfiguration;
	private Instant minimumExportDate;
	private String timeZoneId;
	private String tokenId;

	@Override
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

	@JsonIgnore
	@Override
	public ScheduleType getSchedule() {
		return schedule;
	}

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

	@JsonSetter("dataConfiguration")
	public void setUserDataConfiguration(UserDataConfiguration userDataConfiguration) {
		this.userDataConfiguration = userDataConfiguration;
	}

	/**
	 * Set the data configuration ID.
	 * 
	 * @param id
	 *        the ID to set
	 * @since 1.2
	 */
	@JsonSetter("dataConfigurationId")
	public void setUserDataConfigurationId(Long id) {
		UserDataConfiguration conf = getUserDataConfiguration();
		if ( conf == null ) {
			conf = new UserDataConfiguration();
			setUserDataConfiguration(conf);
		}
		conf.setId(id);
	}

	/**
	 * Get the {@link UserDataConfiguration} ID.
	 * 
	 * @return the ID, or {@literal null}
	 */
	@JsonIgnore
	public Long getUserDataConfigurationId() {
		UserDataConfiguration conf = getUserDataConfiguration();
		return (conf != null ? conf.getId() : null);
	}

	@Override
	public DataConfiguration getDataConfiguration() {
		return getUserDataConfiguration();
	}

	@JsonIgnore
	public UserOutputConfiguration getUserOutputConfiguration() {
		return userOutputConfiguration;
	}

	@JsonSetter("outputConfiguration")
	public void setUserOutputConfiguration(UserOutputConfiguration userOutputConfiguration) {
		this.userOutputConfiguration = userOutputConfiguration;
	}

	/**
	 * Set the output configuration ID.
	 * 
	 * @param id
	 *        the ID to set
	 * @since 1.2
	 */
	@JsonSetter("outputConfigurationId")
	public void setUserOutputConfigurationId(Long id) {
		UserOutputConfiguration conf = getUserOutputConfiguration();
		if ( conf == null ) {
			conf = new UserOutputConfiguration();
			setUserOutputConfiguration(conf);
		}
		conf.setId(id);
	}

	/**
	 * Get the {@link UserOutputConfiguration} ID.
	 * 
	 * @return the ID, or {@literal null}
	 */
	@JsonIgnore
	public Long getUserOutputConfigurationId() {
		UserOutputConfiguration conf = getUserOutputConfiguration();
		return (conf != null ? conf.getId() : null);
	}

	@Override
	public OutputConfiguration getOutputConfiguration() {
		return getUserOutputConfiguration();
	}

	@JsonIgnore
	public UserDestinationConfiguration getUserDestinationConfiguration() {
		return userDestinationConfiguration;
	}

	@JsonSetter("destinationConfiguration")
	public void setUserDestinationConfiguration(
			UserDestinationConfiguration userDestinationConfiguration) {
		this.userDestinationConfiguration = userDestinationConfiguration;
	}

	/**
	 * Set the destination configuration ID.
	 * 
	 * @param id
	 *        the ID to set
	 * @since 1.2
	 */
	@JsonSetter("destinationConfigurationId")
	public void setUserDestinationConfigurationId(Long id) {
		UserDestinationConfiguration conf = getUserDestinationConfiguration();
		if ( conf == null ) {
			conf = new UserDestinationConfiguration();
			setUserDestinationConfiguration(conf);
		}
		conf.setId(id);
	}

	/**
	 * Get the {@link UserDestinationConfiguration} ID.
	 * 
	 * @return the ID, or {@literal null}
	 */
	@JsonIgnore
	public Long getUserDestinationConfigurationId() {
		UserDestinationConfiguration conf = getUserDestinationConfiguration();
		return (conf != null ? conf.getId() : null);
	}

	@Override
	public DestinationConfiguration getDestinationConfiguration() {
		return getUserDestinationConfiguration();
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
	 * Get the minimum export date as a local date.
	 * 
	 * <p>
	 * The date will be in the {@link #getTimeZoneId()} time zone, or
	 * {@code UTC} if that is unavailable.
	 * </p>
	 * 
	 * @return the minimum export date as a local date
	 * @see #getMinimumExportDate()
	 */
	public LocalDateTime getStartingExportDate() {
		Instant dt = getMinimumExportDate();
		if ( dt == null ) {
			return null;
		}
		return dt.atZone(zone()).toLocalDateTime();
	}

	/**
	 * Set the minimum export date as a local date.
	 * 
	 * <p>
	 * The date will be in the {@link #getTimeZoneId()} time zone, or
	 * {@code UTC} if that is unavailable.
	 * </p>
	 * 
	 * @param date
	 *        the date to set
	 * @see #setMinimumExportDate(Instant)
	 */
	public void setStartingExportDate(LocalDateTime date) {
		setMinimumExportDate(date.atZone(zone()).toInstant());
	}

	public void setTimeZoneId(String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	@Override
	public String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Get the configured time zone.
	 * 
	 * @return the configured time zone, defaulting to {@literal UTC} if a
	 *         specific time zone ID is not configured
	 * @since 2.0
	 */
	@NonNull
	public ZoneId zone() {
		String tzId = getTimeZoneId();
		return (tzId != null ? ZoneId.of(tzId) : ZoneOffset.UTC);
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
