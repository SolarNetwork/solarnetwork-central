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

import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.domain.BaseClaimableJob;
import net.solarnetwork.codec.JsonUtils;

/**
 * Entity for user-specific datum import jobs.
 *
 * @author matt
 * @version 2.1
 */
public class DatumImportJobInfo
		extends BaseClaimableJob<Configuration, Long, DatumImportState, UserUuidPK>
		implements UserRelatedEntity<UserUuidPK>, DatumImportRequest, DatumImportResult {

	@Serial
	private static final long serialVersionUID = -7688580940916750418L;

	private Instant importDate;
	private String configJson;
	private String metaJson;

	private transient Map<String, Object> metadata;

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
	public Instant getImportDate() {
		return importDate;
	}

	public void setImportDate(Instant importDate) {
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
	public Instant getCompletionDate() {
		return getCompleted();
	}

	@Override
	@JsonIgnore
	public Instant getCompleted() {
		return super.getCompleted();
	}

	@Override
	@JsonIgnore
	public Instant getStarted() {
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

	/**
	 * Test if a metadata value is present.
	 *
	 * @param key
	 *        the key to check
	 * @param value
	 *        if non-{@code null} then also check if the metadata value equals
	 *        this value
	 * @return {@code true} if the {@code key} exists as metadata, and if
	 *         {@code value} is given then also equals {@code value}
	 * @since 2.1
	 */
	public boolean hasMetadataValue(String key, Object value) {
		Map<String, Object> meta = getMetadata();
		if ( meta == null ) {
			return false;
		}
		Object val = meta.get(key);
		if ( value == null ) {
			return val != null;
		}
		return value.equals(val);
	}

	/**
	 * Get the metadata object as a JSON string.
	 *
	 * @return a JSON encoded string, or {@code null} if no metadata available
	 * @since 2.1
	 */
	@JsonIgnore
	public String getMetaJson() {
		return metaJson;
	}

	/**
	 * Set the metadata object via a JSON string.
	 *
	 * <p>
	 * This method will remove any previously created metadata and replace it
	 * with the values parsed from the JSON.
	 * </p>
	 *
	 * @param json
	 *        the JSON to parse as metadata
	 * @since 2.1
	 */
	@JsonIgnore
	public void setMetaJson(String json) {
		metaJson = json;
		metadata = null;
	}

	/**
	 * Get the metadata.
	 *
	 * <p>
	 * This will decode the {@link #getMetaJson()} value into a map instance.
	 * </p>
	 *
	 * @return the metadata
	 * @since 2.1
	 */
	@JsonIgnore
	public Map<String, Object> getMetadata() {
		if ( metadata == null && metaJson != null ) {
			metadata = JsonUtils.getStringMap(metaJson);
		}
		return metadata;
	}

	/**
	 * Set the metadata to use.
	 *
	 * <p>
	 * This will replace any value set previously via
	 * {@link #setMetaJson(String)} as well.
	 * </p>
	 *
	 * @param metadata
	 *        the metadata to set
	 * @since 2.1
	 */
	@JsonIgnore
	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
		metaJson = JsonUtils.getJSONString(metadata, null);
	}

}
