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

import java.util.UUID;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.domain.BaseObjectEntity;
import net.solarnetwork.util.JsonUtils;

/**
 * Entity for user-specific datum export tasks.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumExportTaskInfo extends BaseObjectEntity<UUID> {

	private static final long serialVersionUID = 8684455161151011352L;

	private ScheduleType scheduleType;
	private DateTime exportDate;
	private DatumExportState status;
	private BasicConfiguration config;
	private String configJson;

	public DateTime getExportDate() {
		return exportDate;
	}

	public void setExportDate(DateTime exportDate) {
		this.exportDate = exportDate;
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
		return (type != null ? type.getKey() : ScheduleType.Daily.getKey());
	}

	public void setScheduleTypeKey(char key) {
		ScheduleType type;
		try {
			type = ScheduleType.forKey(key);
		} catch ( IllegalArgumentException e ) {
			type = ScheduleType.Daily;
		}
		setScheduleType(type);
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

}
