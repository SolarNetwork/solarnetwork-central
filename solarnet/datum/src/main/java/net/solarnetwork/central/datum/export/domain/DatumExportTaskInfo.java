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

package net.solarnetwork.central.datum.export.domain;

import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.codec.JsonUtils;

/**
 * Entity for user-specific datum export tasks.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumExportTaskInfo extends BaseObjectEntity<UUID>
		implements DatumExportRequest, DatumExportResult {

	private static final long serialVersionUID = -6825907221034388360L;

	private Instant exportDate;
	private DatumExportState status;
	private BasicConfiguration config;
	private String configJson;
	private Boolean taskSuccess;
	private String message;
	private Instant completed;

	@Override
	public Instant getExportDate() {
		return exportDate;
	}

	public void setExportDate(Instant exportDate) {
		this.exportDate = exportDate;
	}

	@JsonIgnore
	public DatumExportState getStatus() {
		return status;
	}

	public void setStatus(DatumExportState status) {
		this.status = status;
	}

	public char getStatusKey() {
		DatumExportState status = getStatus();
		return (status != null ? status.getKey() : DatumExportState.Unknown.getKey());
	}

	public void setStatusKey(char key) {
		DatumExportState status;
		try {
			status = DatumExportState.forKey(key);
		} catch ( IllegalArgumentException e ) {
			status = DatumExportState.Unknown;
		}
		setStatus(status);
	}

	public BasicConfiguration getConfig() {
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, BasicConfiguration.class);
		}
		return config;
	}

	public void setConfig(BasicConfiguration config) {
		this.config = config;
		configJson = null;
	}

	@JsonIgnore
	@Override
	public Configuration getConfiguration() {
		return getConfig();
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
	public boolean isSuccess() {
		return (taskSuccess != null && taskSuccess.booleanValue());
	}

	@JsonIgnore
	public Boolean getTaskSuccess() {
		return taskSuccess;
	}

	public void setTaskSuccess(Boolean taskSuccess) {
		this.taskSuccess = taskSuccess;
	}

	@Override
	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public Instant getCompletionDate() {
		return getCompleted();
	}

	@JsonIgnore
	public Instant getCompleted() {
		return completed;
	}

	public void setCompleted(Instant completed) {
		this.completed = completed;
	}

}
