/* ==================================================================
 * CloudDatumStreamConfigurationInput.java - 4/10/2024 1:56:30 pm
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.c2c.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.dao.BaseUserRelatedStdIdentifiableConfigurationInput;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * DTO for cloud datum stream configuration.
 *
 * @author matt
 * @version 1.1
 */
public class CloudDatumStreamConfigurationInput extends
		BaseUserRelatedStdIdentifiableConfigurationInput<CloudDatumStreamConfiguration, UserLongCompositePK>
		implements
		CloudIntegrationsConfigurationInput<CloudDatumStreamConfiguration, UserLongCompositePK> {

	private Long datumStreamMappingId;

	private String schedule;

	@NotNull
	private ObjectDatumKind kind;

	private Long objectId;

	@Size(max = 64)
	private String sourceId;

	/**
	 * Constructor.
	 */
	public CloudDatumStreamConfigurationInput() {
		super();
	}

	@Override
	public CloudDatumStreamConfiguration toEntity(UserLongCompositePK id, Instant date) {
		CloudDatumStreamConfiguration conf = new CloudDatumStreamConfiguration(
				requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(CloudDatumStreamConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setDatumStreamMappingId(datumStreamMappingId);
		conf.setSchedule(schedule);
		conf.setKind(kind);
		conf.setObjectId(objectId);
		conf.setSourceId(sourceId);
	}

	/**
	 * Get the associated {@link CloudDatumStreamMappingConfiguration}
	 * {@code configId}.
	 *
	 * @return the datum stream mapping ID
	 */
	public final Long getDatumStreamMappingId() {
		return datumStreamMappingId;
	}

	/**
	 * Set the associated {@link CloudDatumStreamMappingConfiguration}
	 * {@code configId}.
	 *
	 * @param datumStreamMappingId
	 *        the datum stream mapping ID to set
	 */
	public final void setDatumStreamMappingId(Long datumStreamMappingId) {
		this.datumStreamMappingId = datumStreamMappingId;
	}

	/**
	 * Get the schedule at which to poll for data.
	 *
	 * @return the schedule, as either a cron schedule or a number of seconds,
	 *         or {@literal null} if polling is not used
	 */
	public final String getSchedule() {
		return schedule;
	}

	/**
	 * Set the schedule at which to pull data.
	 *
	 * @param schedule
	 *        the schedule to set, as either a cron schedule or a number of
	 *        seconds, or {@literal null} if polling is not used
	 */
	public final void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the datum stream kind.
	 *
	 * @return the kind
	 */
	public final ObjectDatumKind getKind() {
		return kind;
	}

	/**
	 * Set the datum stream kind.
	 *
	 * @param kind
	 *        the kind to set
	 */
	public final void setKind(ObjectDatumKind kind) {
		this.kind = kind;
	}

	/**
	 * Get the datum stream object ID.
	 *
	 * @return the object ID
	 */
	public final Long getObjectId() {
		return objectId;
	}

	/**
	 * Set the datum stream object ID.
	 *
	 * @param objectId
	 *        the object ID to set
	 */
	public final void setObjectId(Long objectId) {
		this.objectId = objectId;
	}

	/**
	 * Get the datum stream source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the datum stream source ID.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public final void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

}
