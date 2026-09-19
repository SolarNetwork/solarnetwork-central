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

package net.solarnetwork.central.user.datum.export.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.codec.jackson.JsonUtils;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Entity for user-specific datum export tasks.
 *
 * @author matt
 * @version 1.2
 */
public class UserDatumExportTaskInfo extends BaseObjectEntity<UserDatumExportTaskPK>
		implements UserRelatedEntity<UserDatumExportTaskPK> {

	@Serial
	private static final long serialVersionUID = -7053341262497665231L;

	private final Long userDatumExportConfigurationId;
	private @Nullable UUID taskId;
	private @Nullable Configuration config;
	private @Nullable String configJson;
	private @Nullable DatumExportTaskInfo task;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @param userDatumExportConfigurationId
	 *        the export configuration ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDatumExportTaskInfo(UserDatumExportTaskPK id, Instant created,
			Long userDatumExportConfigurationId) {
		super();
		setId(requireNonNullArgument(id, "id"));
		setCreated(requireNonNullArgument(created, "created"));
		this.userDatumExportConfigurationId = requireNonNullArgument(userDatumExportConfigurationId,
				"userDatumExportConfigurationId");
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param scheduleType
	 *        the schedule type
	 * @param date
	 *        the date
	 * @param created
	 *        the creation date
	 * @param userDatumExportConfigurationId
	 *        the export configuration ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserDatumExportTaskInfo(Long userId, ScheduleType scheduleType, Instant date, Instant created,
			Long userDatumExportConfigurationId) {
		this(new UserDatumExportTaskPK(userId, scheduleType, date), created,
				userDatumExportConfigurationId);
	}

	@Override
	public String toString() {
		return "UserDatumExportTaskInfo{userId=" + getUserId() + ",date=" + getExportDate()
				+ ",scheduleType=" + getScheduleType() + ",taskId=" + taskId + ",configId="
				+ userDatumExportConfigurationId + "}";
	}

	/**
	 * Get the related {@link UserDatumExportConfiguration#getId()} value.
	 *
	 * @return the user configuration ID
	 */
	public final Long getUserDatumExportConfigurationId() {
		return userDatumExportConfigurationId;
	}

	public final @Nullable UUID getTaskId() {
		return taskId;
	}

	public final void setTaskId(@Nullable UUID taskId) {
		this.taskId = taskId;
	}

	public final @Nullable DatumExportTaskInfo getTask() {
		return task;
	}

	public final void setTask(@Nullable DatumExportTaskInfo task) {
		this.task = task;
		if ( task != null ) {
			setConfig(task.getConfig());
		}
	}

	public final @Nullable Configuration getConfig() {
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, BasicConfiguration.class);
		}
		return config;
	}

	/**
	 * Set the configuration.
	 *
	 * @param config
	 *        the configuration to set
	 */
	@JsonDeserialize(as = BasicConfiguration.class)
	public final void setConfig(@Nullable Configuration config) {
		if ( config != null && !config.getClass().equals(BasicConfiguration.class) ) {
			config = new BasicConfiguration(config);
		}
		this.config = config;
		configJson = null;
	}

	@JsonIgnore
	public final @Nullable String getConfigJson() {
		if ( configJson == null ) {
			configJson = JsonUtils.getJSONString(config, null);
		}
		return configJson;
	}

	public final void setConfigJson(@Nullable String configJson) {
		this.configJson = configJson;
		config = null;
	}

	@Override
	public final Long getUserId() {
		return id().getUserId();
	}

	@JsonIgnore
	public final ScheduleType getScheduleType() {
		return id().getScheduleType();
	}

	public final char getScheduleTypeKey() {
		return id().getScheduleTypeKey();
	}

	public final Instant getExportDate() {
		return id().getDate();
	}

}
