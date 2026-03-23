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
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
 * @since 1.1
 */
@JsonPropertyOrder({ "id", "userId", "scheduleTypeKey", "created", "config", "tokenId", "task" })
public class UserAdhocDatumExportTaskInfo extends BaseObjectEntity<UUID>
		implements UserRelatedEntity<UUID> {

	@Serial
	private static final long serialVersionUID = 3607582937761384987L;

	private final Long userId;
	private @Nullable ScheduleType scheduleType;
	private @Nullable Configuration config;
	private @Nullable String configJson;
	private @Nullable String tokenId;
	private @Nullable DatumExportTaskInfo task;

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public UserAdhocDatumExportTaskInfo(Long userId) {
		super();
		this.userId = requireNonNullArgument(userId, "userId");
	}

	@Override
	public String toString() {
		return "UserAdhocDatumExportTaskInfo{userId=" + getUserId() + ",created=" + getCreated()
				+ ",scheduleType=" + getScheduleType() + ",id=" + getId() + "}";
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
		return userId;
	}

	@JsonIgnore
	public final @Nullable ScheduleType getScheduleType() {
		return scheduleType;
	}

	public final void setScheduleType(@Nullable ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}

	public final char getScheduleTypeKey() {
		ScheduleType type = getScheduleType();
		return (type != null ? type.getKey() : ScheduleType.Adhoc.getKey());
	}

	public final void setScheduleTypeKey(char key) {
		try {
			setScheduleType(ScheduleType.forKey(key));
		} catch ( IllegalArgumentException e ) {
			setScheduleType(ScheduleType.Adhoc);
		}
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

}
