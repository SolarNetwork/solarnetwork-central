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

package net.solarnetwork.central.user.datum.expire.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.domain.BaseClaimableJob;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Entity for user-specific datum delete job status information.
 *
 * @author matt
 * @version 2.0
 */
@JsonIgnoreProperties({ "completed", "configJson", "created", "jobSuccess", "modified", "result",
		"started" })
public class DatumDeleteJobInfo
		extends BaseClaimableJob<GeneralNodeDatumFilter, Long, DatumDeleteJobState, UserUuidPK>
		implements UserRelatedEntity<UserUuidPK> {

	@Serial
	private static final long serialVersionUID = 464029861491855667L;

	private @Nullable String configJson;

	@Override
	public @Nullable Long getUserId() {
		UserUuidPK pk = getId();
		return (pk != null ? pk.getUserId() : null);
	}

	public void setUserId(@Nullable Long userId) {
		UserUuidPK pk = getId();
		if ( pk == null ) {
			setId(new UserUuidPK(userId, null));
		} else {
			pk.setUserId(userId);
		}
	}

	@JsonGetter("id")
	public @Nullable UUID getUuid() {
		UserUuidPK pk = getId();
		return (pk != null ? pk.getId() : null);
	}

	@JsonSetter("id")
	public void setUuid(@Nullable UUID id) {
		UserUuidPK pk = getId();
		if ( pk == null ) {
			setId(new UserUuidPK(null, id));
		} else {
			pk.setId(id);
		}
	}

	public @Nullable String getJobId() {
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
	public @Nullable GeneralNodeDatumFilter didGetConfiguration(
			@Nullable GeneralNodeDatumFilter config) {
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, DatumFilterCommand.class);
			replaceConfiguration(config);
		}
		return config;
	}

	@Override
	public void didSetConfiguration(GeneralNodeDatumFilter config) {
		this.configJson = null;
	}

	@JsonIgnore
	public @Nullable String getConfigJson() {
		if ( configJson == null ) {
			configJson = JsonUtils.getJSONString(configuration(), null);
		}
		return configJson;
	}

	public void setConfigJson(@Nullable String configJson) {
		replaceConfiguration(null);
		this.configJson = configJson;
	}

	public long getResultCount() {
		Long result = getResult();
		return (result != null ? result : 0L);
	}

	public void setResultCount(long deletedCount) {
		setResult(deletedCount);
	}

	public long getSubmitDate() {
		Instant dt = super.getCreated();
		return (dt != null ? dt.toEpochMilli() : 0);
	}

	public long getModifiedDate() {
		Instant dt = super.getModified();
		return (dt != null ? dt.toEpochMilli() : 0);
	}

	public long getStartedDate() {
		Instant dt = super.getStarted();
		return (dt != null ? dt.toEpochMilli() : 0);
	}

	public long getCompletionDate() {
		Instant dt = super.getCompleted();
		return (dt != null ? dt.toEpochMilli() : 0);
	}

}
