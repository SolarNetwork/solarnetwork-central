/* ==================================================================
 * DatumImportJobInfo.java - 7/11/2018 11:04:40 AM
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

package net.solarnetwork.central.datum.imp.domain;

import java.util.UUID;
import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.BaseClaimableJob;
import net.solarnetwork.central.user.domain.UserRelatedEntity;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.util.JsonUtils;

/**
 * Entity for user-specific datum import jobs.
 * 
 * @author matt
 * @version 1.1
 */
public class DatumImportJobInfo
		extends BaseClaimableJob<Configuration, Long, DatumImportState, UserUuidPK>
		implements UserRelatedEntity<UserUuidPK>, DatumImportRequest, DatumImportResult {

	private static final long serialVersionUID = 8208441028181856685L;

	private DateTime importDate;
	private String configJson;

	@JsonIgnore
	@Override
	public UserUuidPK getId() {
		return super.getId();
	}

	@Override
	public Long getUserId() {
		UserUuidPK pk = getId();
		return (pk != null ? pk.getUserId() : null);
	}

	public void setUserId(Long userId) {
		UserUuidPK pk = getId();
		if ( pk == null ) {
			setId(new UserUuidPK(userId, null));
		} else {
			pk.setUserId(userId);
		}
	}

	@JsonGetter("id")
	public UUID getUuid() {
		UserUuidPK pk = getId();
		return (pk != null ? pk.getId() : null);
	}

	@JsonSetter("id")
	public void setUuid(UUID id) {
		UserUuidPK pk = getId();
		if ( pk == null ) {
			setId(new UserUuidPK(null, id));
		} else {
			pk.setId(id);
		}
	}

	@Override
	public DateTime getImportDate() {
		return importDate;
	}

	public void setImportDate(DateTime importDate) {
		this.importDate = importDate;
	}

	@JsonIgnore
	public DatumImportState getImportState() {
		return getJobState();
	}

	public void setImportState(DatumImportState state) {
		setJobState(state);
	}

	public char getImportStateKey() {
		return getJobStateKey();
	}

	public void setImportStateKey(char key) {
		DatumImportState state;
		try {
			state = DatumImportState.forKey(key);
		} catch ( IllegalArgumentException e ) {
			state = DatumImportState.Unknown;
		}
		setJobState(state);
	}

	public BasicConfiguration getConfig() {
		return (BasicConfiguration) getConfiguration();
	}

	public void setConfig(BasicConfiguration config) {
		setConfiguration(config);
	}

	@JsonIgnore
	@Override
	public Configuration getConfiguration() {
		Configuration config = super.getConfiguration();
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, BasicConfiguration.class);
			super.setConfiguration(config);
		}
		return config;
	}

	@Override
	public void setConfiguration(Configuration config) {
		super.setConfiguration(config);
		this.configJson = null;
	}

	@JsonIgnore
	public String getConfigJson() {
		if ( configJson == null ) {
			configJson = JsonUtils.getJSONString(super.getConfiguration(), null);
		}
		return configJson;
	}

	public void setConfigJson(String configJson) {
		this.configJson = configJson;
		super.setConfiguration(null);
	}

	@Override
	@JsonIgnore
	public Boolean getJobSuccess() {
		return super.getJobSuccess();
	}

	@Override
	public DateTime getCompletionDate() {
		return getCompleted();
	}

	@Override
	@JsonIgnore
	public DateTime getCompleted() {
		return super.getCompleted();
	}

	@Override
	@JsonIgnore
	public DateTime getStarted() {
		return super.getStarted();
	}

	@Override
	public long getLoadedCount() {
		Long result = getResult();
		return (result != null ? result : 0L);
	}

	public void setLoadedCount(long loadedCount) {
		setResult(loadedCount);
	}

}
