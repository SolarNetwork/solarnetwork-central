/* ==================================================================
 * DatumDeleteJobInfo.java - 26/11/2018 7:01:23 AM
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

package net.solarnetwork.central.user.expire.domain;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.domain.BaseClaimableJob;
import net.solarnetwork.central.user.domain.UserRelatedEntity;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.util.JsonUtils;

/**
 * Entity for user-specific datum delete job status information.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumDeleteJobInfo
		extends BaseClaimableJob<GeneralNodeDatumFilter, Long, DatumDeleteJobState, UserUuidPK>
		implements UserRelatedEntity<UserUuidPK> {

	private static final long serialVersionUID = 464029861491855667L;

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

	public String getJobId() {
		UserUuidPK id = getId();
		return (id != null && id.getId() != null ? id.getId().toString() : null);
	}

	public void setJobStateKey(char key) {
		DatumDeleteJobState state;
		try {
			state = DatumDeleteJobState.forKey(key);
		} catch ( IllegalArgumentException e ) {
			state = DatumDeleteJobState.Unknown;
		}
		setJobState(state);
	}

	@Override
	public GeneralNodeDatumFilter getConfiguration() {
		GeneralNodeDatumFilter config = super.getConfiguration();
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, DatumFilterCommand.class);
			super.setConfiguration(config);
		}
		return config;
	}

	@Override
	public void setConfiguration(GeneralNodeDatumFilter config) {
		super.setConfiguration(config);
		this.configJson = null;
	}

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
	@JsonIgnore
	public Long getResult() {
		return super.getResult();
	}

	public long getResultCount() {
		Long result = getResult();
		return (result != null ? result : 0L);
	}

	public void setResultCount(long deletedCount) {
		setResult(deletedCount);
	}

}
