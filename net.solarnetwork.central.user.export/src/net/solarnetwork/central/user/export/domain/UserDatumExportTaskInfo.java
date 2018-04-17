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
import org.joda.time.DateTime;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.BaseObjectEntity;
import net.solarnetwork.central.user.domain.UserRelatedEntity;
import net.solarnetwork.util.JsonUtils;

/**
 * Entity for user-specific datum export tasks.
 * 
 * @author matt
 * @version 1.0
 */
public class UserDatumExportTaskInfo extends BaseObjectEntity<UserDatumExportTaskPK>
		implements UserRelatedEntity<UserDatumExportTaskPK> {

	private static final long serialVersionUID = 2326178444641325856L;

	private UUID taskId;
	private UserDatumExportConfiguration config;
	private String configJson;

	public UUID getTaskId() {
		return taskId;
	}

	public void setTaskId(UUID taskId) {
		this.taskId = taskId;
	}

	public UserDatumExportConfiguration getConfig() {
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, UserDatumExportConfiguration.class);
		}
		return config;
	}

	public void setConfig(UserDatumExportConfiguration config) {
		this.config = config;
		configJson = null;
	}

	public String getConfigJson() {
		if ( configJson == null ) {
			configJson = JsonUtils.getJSONString(configJson, null);
		}
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
		config = null;
	}

	@Override
	public Long getUserId() {
		UserDatumExportTaskPK id = getId();
		return (id != null ? id.getUserId() : null);
	}

	public void setUserId(Long userId) {
		UserDatumExportTaskPK id = getId();
		if ( id == null ) {
			id = new UserDatumExportTaskPK();
			setId(id);
		}
		id.setUserId(userId);
	}

	public char getScheduleTypeKey() {
		UserDatumExportTaskPK id = getId();
		return (id != null ? id.getScheduleTypeKey() : ScheduleType.Daily.getKey());
	}

	public void setScheduleTypeKey(char key) {
		UserDatumExportTaskPK id = getId();
		if ( id == null ) {
			id = new UserDatumExportTaskPK();
			setId(id);
		}
		id.setScheduleTypeKey(key);
	}

	public DateTime getExportDate() {
		UserDatumExportTaskPK id = getId();
		return (id != null ? id.getDate() : null);
	}

	public void setExportDate(DateTime date) {
		UserDatumExportTaskPK id = getId();
		if ( id == null ) {
			id = new UserDatumExportTaskPK();
			setId(id);
		}
		id.setDate(date);
	}

}
