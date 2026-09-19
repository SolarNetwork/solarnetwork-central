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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import net.solarnetwork.central.dao.BaseObjectEntity;
import net.solarnetwork.codec.jackson.JsonUtils;

/**
 * Entity for user-specific datum export tasks.
 *
 * @author matt
 * @version 2.1
 */
public class DatumExportTaskInfo extends BaseObjectEntity<UUID>
		implements DatumExportRequest, DatumExportResult {

	@Serial
	private static final long serialVersionUID = -6825907221034388360L;

	private @Nullable Instant exportDate;
	private @Nullable DatumExportState status;
	private @Nullable BasicConfiguration config;
	private @Nullable String configJson;
	private @Nullable Boolean taskSuccess;
	private @Nullable String message;
	private @Nullable Instant completed;
	private @Nullable Long userId;
	private @Nullable String tokenId;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DatumExportTaskInfo(UUID id) {
		super();
		setId(requireNonNullArgument(id, "id"));
	}

	@Override
	public final @Nullable Instant getExportDate() {
		return exportDate;
	}

	public final void setExportDate(@Nullable Instant exportDate) {
		this.exportDate = exportDate;
	}

	@JsonIgnore
	public final @Nullable DatumExportState getStatus() {
		return status;
	}

	public final void setStatus(@Nullable DatumExportState status) {
		this.status = status;
	}

	public final char getStatusKey() {
		DatumExportState status = getStatus();
		return (status != null ? status.getKey() : DatumExportState.Unknown.getKey());
	}

	public final void setStatusKey(char key) {
		DatumExportState status;
		try {
			status = DatumExportState.forKey(key);
		} catch ( IllegalArgumentException e ) {
			status = DatumExportState.Unknown;
		}
		setStatus(status);
	}

	public final @Nullable BasicConfiguration getConfig() {
		if ( config == null && configJson != null ) {
			config = JsonUtils.getObjectFromJSON(configJson, BasicConfiguration.class);
		}
		return config;
	}

	public final void setConfig(@Nullable BasicConfiguration config) {
		this.config = config;
		configJson = null;
	}

	@JsonIgnore
	@Override
	public final @Nullable Configuration getConfiguration() {
		return getConfig();
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
	public final @Nullable Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 *
	 * @param userId
	 *        the ID to set
	 * @since 2.1
	 */
	public final void setUserId(@Nullable Long userId) {
		this.userId = userId;
	}

	@Override
	public final @Nullable String getTokenId() {
		return tokenId;
	}

	/**
	 * Set the authorization token ID.
	 *
	 * @param tokenId
	 *        the token ID to set
	 * @since 2.1
	 */
	public final void setTokenId(@Nullable String tokenId) {
		this.tokenId = tokenId;
	}

	@Override
	public final boolean isSuccess() {
		return (taskSuccess != null && taskSuccess);
	}

	@JsonIgnore
	public final @Nullable Boolean getTaskSuccess() {
		return taskSuccess;
	}

	public final void setTaskSuccess(@Nullable Boolean taskSuccess) {
		this.taskSuccess = taskSuccess;
	}

	@Override
	public final @Nullable String getMessage() {
		return message;
	}

	public final void setMessage(@Nullable String message) {
		this.message = message;
	}

	@Override
	public final @Nullable Instant getCompletionDate() {
		return getCompleted();
	}

	@JsonIgnore
	public final @Nullable Instant getCompleted() {
		return completed;
	}

	public final void setCompleted(@Nullable Instant completed) {
		this.completed = completed;
	}

}
