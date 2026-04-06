/* ==================================================================
 * UserDatumExportConfigurationInput.java - 17/03/2025 5:01:53 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.dao.BaseUserRelatedStdInput;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.user.datum.export.domain.UserDatumExportConfiguration;

/**
 * Input DTO for {@link UserDatumExportConfiguration} entities.
 *
 * @author matt
 * @version 1.0
 */
public final class UserDatumExportConfigurationInput
		extends BaseUserRelatedStdInput<UserDatumExportConfiguration, UserLongCompositePK> {

	private @Nullable Long id;
	private @Nullable String name;
	private @Nullable ScheduleType schedule;
	private int hourDelayOffset;
	private @Nullable Instant minimumExportDate;
	private @Nullable String timeZoneId;
	private @Nullable String tokenId;

	private @Nullable Long dataConfigurationId;
	private @Nullable Long destinationConfigurationId;
	private @Nullable Long outputConfigurationId;

	private @Nullable UserDataConfigurationInput dataConfiguration;
	private @Nullable UserOutputConfigurationInput outputConfiguration;
	private @Nullable UserDestinationConfigurationInput destinationConfiguration;

	/**
	 * Constructor.
	 */
	public UserDatumExportConfigurationInput() {
		super();
		this.minimumExportDate = now().truncatedTo(HOURS);
	}

	@Override
	public UserDatumExportConfiguration toEntity(UserLongCompositePK id, Instant date) {
		UserDatumExportConfiguration entity = new UserDatumExportConfiguration(id, date, getName(),
				schedule, hourDelayOffset, minimumExportDate);
		populateConfiguration(entity);
		return entity;
	}

	@Override
	protected void populateConfiguration(UserDatumExportConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setName(name);
		conf.setSchedule(schedule);
		conf.setHourDelayOffset(hourDelayOffset);
		conf.setMinimumExportDate(minimumExportDate);
		conf.setTimeZoneId(timeZoneId);
		conf.setTokenId(tokenId);

		if ( dataConfiguration != null ) {
			if ( dataConfiguration.getName() == null ) {
				dataConfiguration.setName(name);
			}
			if ( dataConfiguration.getServiceIdentifier() == null ) {
				dataConfiguration.setServiceIdentifier("");
			}
			conf.setUserDataConfiguration(
					dataConfiguration.toEntity(
							new UserLongCompositePK(conf.getUserId(),
									dataConfiguration.getId() != null ? dataConfiguration.getId()
											: UserLongCompositePK.UNASSIGNED_ENTITY_ID),
							conf.getCreated()));
		} else if ( dataConfigurationId != null ) {
			conf.setUserDataConfigurationId(dataConfigurationId);
		}

		if ( outputConfiguration != null ) {
			if ( outputConfiguration.getName() == null ) {
				outputConfiguration.setName(name);
			}
			if ( outputConfiguration.getServiceIdentifier() == null ) {
				outputConfiguration.setServiceIdentifier("");
			}
			conf.setUserOutputConfiguration(
					outputConfiguration.toEntity(
							new UserLongCompositePK(conf.getUserId(),
									outputConfiguration.getId() != null ? outputConfiguration.getId()
											: UserLongCompositePK.UNASSIGNED_ENTITY_ID),
							conf.getCreated()));
		} else if ( outputConfigurationId != null ) {
			conf.setUserOutputConfigurationId(outputConfigurationId);
		}

		if ( destinationConfiguration != null ) {
			if ( destinationConfiguration.getName() == null ) {
				destinationConfiguration.setName(name);
			}
			if ( destinationConfiguration.getServiceIdentifier() == null ) {
				destinationConfiguration.setServiceIdentifier("");
			}
			conf.setUserDestinationConfiguration(
					destinationConfiguration.toEntity(
							new UserLongCompositePK(conf.getUserId(),
									destinationConfiguration.getId() != null
											? destinationConfiguration.getId()
											: UserLongCompositePK.UNASSIGNED_ENTITY_ID),
							conf.getCreated()));
		} else if ( destinationConfigurationId != null ) {
			conf.setUserDestinationConfigurationId(destinationConfigurationId);
		}
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getId() {
		return id;
	}

	/**
	 * Set the configuration ID.
	 *
	 * @param id
	 *        the ID to set
	 */
	public final void setId(@Nullable Long id) {
		this.id = id;
	}

	/**
	 * Get the name.
	 *
	 * @return the name to set
	 */
	public final @Nullable String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public final void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Get the schedule.
	 *
	 * @return the schedule
	 */
	public final @Nullable ScheduleType getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule.
	 *
	 * @param schedule
	 *        the schedule to set
	 */
	public final void setSchedule(@Nullable ScheduleType schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the schedule type key value.
	 *
	 * @return the schedule type; if {@link #getSchedule()} is {@code null} this
	 *         will return the key value for {@link ScheduleType#Daily}
	 */
	public final char getScheduleKey() {
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
	public final void setScheduleKey(char key) {
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
	public final int getHourDelayOffset() {
		return hourDelayOffset;
	}

	/**
	 * Set the hour delay offset.
	 *
	 * @param hourDelayOffset
	 *        the offset to set
	 */
	public final void setHourDelayOffset(int hourDelayOffset) {
		this.hourDelayOffset = hourDelayOffset;
	}

	/**
	 * Get the data configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getDataConfigurationId() {
		return dataConfigurationId;
	}

	/**
	 * Set the data configuration ID.
	 *
	 * @param userDataConfigurationId
	 *        the ID to set
	 */
	public final void setDataConfigurationId(@Nullable Long userDataConfigurationId) {
		this.dataConfigurationId = userDataConfigurationId;
	}

	/**
	 * Get the destination configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getDestinationConfigurationId() {
		return destinationConfigurationId;
	}

	/**
	 * Set the destination configuration ID.
	 *
	 * @param userDestinationConfigurationId
	 *        the ID to set
	 */
	public final void setDestinationConfigurationId(@Nullable Long userDestinationConfigurationId) {
		this.destinationConfigurationId = userDestinationConfigurationId;
	}

	/**
	 * Get the output configuration ID.
	 *
	 * @return the ID
	 */
	public final @Nullable Long getOutputConfigurationId() {
		return outputConfigurationId;
	}

	/**
	 * Set the output configuration ID.
	 *
	 * @param userOutputConfigurationId
	 *        the ID to set
	 */
	public final void setOutputConfigurationId(@Nullable Long userOutputConfigurationId) {
		this.outputConfigurationId = userOutputConfigurationId;
	}

	/**
	 * Get the minimum export date that can be scheduled for execution.
	 *
	 * @return the minimum export date
	 */
	public final @Nullable Instant getMinimumExportDate() {
		return minimumExportDate;
	}

	/**
	 * Set the minimum export date that can be scheduled for execution.
	 *
	 * @param minimumExportDate
	 *        the minimum export date to use
	 */
	public final void setMinimumExportDate(@Nullable Instant minimumExportDate) {
		this.minimumExportDate = minimumExportDate;
	}

	/**
	 * Get the time zone ID.
	 *
	 * @return the ID
	 */
	public final @Nullable String getTimeZoneId() {
		return timeZoneId;
	}

	/**
	 * Set the time zone ID.
	 *
	 * @param timeZoneId
	 *        the ID to set
	 */
	public final void setTimeZoneId(@Nullable String timeZoneId) {
		this.timeZoneId = timeZoneId;
	}

	/**
	 * Get the token ID.
	 *
	 * @return the token ID
	 */
	public final @Nullable String getTokenId() {
		return tokenId;
	}

	/**
	 * Set the token ID.
	 *
	 * @param tokenId
	 *        the token ID to set
	 */
	public final void setTokenId(@Nullable String tokenId) {
		this.tokenId = tokenId;
	}

	/**
	 * Get the data configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable UserDataConfigurationInput getDataConfiguration() {
		return dataConfiguration;
	}

	/**
	 * Set the data configuration.
	 *
	 * @param dataConfiguration
	 *        the configuration to set
	 */
	public final void setDataConfiguration(@Nullable UserDataConfigurationInput dataConfiguration) {
		this.dataConfiguration = dataConfiguration;
	}

	/**
	 * Get the output configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable UserOutputConfigurationInput getOutputConfiguration() {
		return outputConfiguration;
	}

	/**
	 * Set the output configuration.
	 *
	 * @param outputConfiguration
	 *        the configuration to set
	 */
	public final void setOutputConfiguration(
			@Nullable UserOutputConfigurationInput outputConfiguration) {
		this.outputConfiguration = outputConfiguration;
	}

	/**
	 * Get the destination configuration.
	 *
	 * @return the configuration
	 */
	public final @Nullable UserDestinationConfigurationInput getDestinationConfiguration() {
		return destinationConfiguration;
	}

	/**
	 * Set the destination configuration.
	 *
	 * @param destinationConfiguration
	 *        the configuration to set
	 */
	public final void setDestinationConfiguration(
			@Nullable UserDestinationConfigurationInput destinationConfiguration) {
		this.destinationConfiguration = destinationConfiguration;
	}

}
