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

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.codec.JsonUtils;

/**
 * Entity for user-specific datum export tasks.
 * 
 * @author matt
 * @version 1.0
 * @since 1.1
 */
@JsonPropertyOrder({ "id", "userId", "scheduleTypeKey", "created", "config", "task" })
public class UserAdhocDatumExportTaskInfo extends BaseObjectEntity<UUID>
		implements UserRelatedEntity<UUID> {

	private static final long serialVersionUID = -2432426770743118556L;

	private Long userId;
	private ScheduleType scheduleType;
	private Configuration config;
	private String configJson;
	private DatumExportTaskInfo task;

	@Override
	public String toString() {
		return "UserAdhocDatumExportTaskInfo{userId=" + getUserId() + ",created=" + getCreated()
				+ ",scheduleType=" + getScheduleType() + ",id=" + getId() + "}";
	}

	public DatumExportTaskInfo getTask() {
		return task;
	}

	public void setTask(DatumExportTaskInfo task) {
		this.task = task;
		if ( task != null ) {
			setConfig(task.getConfig());
		}
	}

	public Configuration getConfig() {
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
	public void setConfig(Configuration config) {
		if ( !config.getClass().equals(BasicConfiguration.class) ) {
			config = new BasicConfiguration(config);
		}
		this.config = config;
		configJson = null;
	}

	@JsonIgnore
	public String getConfigJson() {
		if ( configJson == null ) {
			configJson = JsonUtils.getJSONString(config, null);
		}
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
		config = null;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@JsonIgnore
	public ScheduleType getScheduleType() {
		return scheduleType;
	}

	public void setScheduleType(ScheduleType scheduleType) {
		this.scheduleType = scheduleType;
	}

	public char getScheduleTypeKey() {
		ScheduleType type = getScheduleType();
		return (type != null ? type.getKey() : ScheduleType.Adhoc.getKey());
	}

	public void setScheduleTypeKey(char key) {
		try {
			setScheduleType(ScheduleType.forKey(key));
		} catch ( IllegalArgumentException e ) {
			setScheduleType(ScheduleType.Adhoc);
		}
	}

}
