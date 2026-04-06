/* ==================================================================
 * UserSettingsInput.java - 11/10/2022 5:50:27 pm
 *
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.oscp.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import jakarta.validation.constraints.NotNull;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * DTO for user group settings.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("MultipleNullnessAnnotations")
public class UserSettingsInput implements OscpConfigurationInput<UserSettings, Long> {

	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private @Nullable String sourceIdTemplate;

	@NotNull
	private @Nullable Long nodeId;

	@SuppressWarnings("NullAway")
	@Override
	public UserSettings toEntity(Long id) {
		UserSettings conf = new UserSettings(id, Instant.now(), nodeId);
		populateConfiguration(conf);
		return conf;
	}

	@SuppressWarnings("NullAway")
	private void populateConfiguration(UserSettings conf) {
		conf.setPublishToSolarIn(publishToSolarIn);
		conf.setPublishToSolarFlux(publishToSolarFlux);
		conf.setSourceIdTemplate(sourceIdTemplate);
		conf.setNodeId(nodeId);
	}

	/**
	 * Get the "publish to SolarIn" toggle.
	 *
	 * @return {@literal true} if data from this group should be published to
	 *         SolarIn
	 */
	public final boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 *
	 * @param publishToSolarIn
	 *        {@literal true} if data from this group should be published to
	 *        SolarIn
	 */
	public final void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 *
	 * @return {@literal true} if data from this group should be published to
	 *         SolarFlux
	 */
	public final boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 *
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this group should be published to
	 *        SolarFlux
	 */
	public final void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

	/**
	 * Get the source ID template.
	 *
	 * @return the source ID template
	 */
	public final @Nullable String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 *
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public final void setSourceIdTemplate(@Nullable String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public final @Nullable Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public final void setNodeId(@Nullable Long nodeId) {
		this.nodeId = nodeId;
	}

}
