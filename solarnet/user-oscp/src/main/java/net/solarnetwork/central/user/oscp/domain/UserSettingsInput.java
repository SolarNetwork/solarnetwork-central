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
import javax.validation.constraints.NotNull;
import net.solarnetwork.central.oscp.domain.UserSettings;

/**
 * DTO for user group settings.
 * 
 * @author matt
 * @version 1.0
 */
public class UserSettingsInput implements OscpConfigurationInput<UserSettings, Long> {

	private boolean publishToSolarIn = true;
	private boolean publishToSolarFlux = true;
	private String sourceIdTemplate;

	@NotNull
	private Long nodeId;

	@Override
	public UserSettings toEntity(Long id) {
		UserSettings conf = new UserSettings(id, Instant.now());
		populateConfiguration(conf);
		return conf;
	}

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
	public boolean isPublishToSolarIn() {
		return publishToSolarIn;
	}

	/**
	 * Set the "publish to SolarIn" toggle.
	 * 
	 * @param publishToSolarIn
	 *        {@literal true} if data from this group should be published to
	 *        SolarIn
	 */
	public void setPublishToSolarIn(boolean publishToSolarIn) {
		this.publishToSolarIn = publishToSolarIn;
	}

	/**
	 * Get the "publish to SolarFlux" toggle.
	 * 
	 * @return {@literal true} if data from this group should be published to
	 *         SolarFlux
	 */
	public boolean isPublishToSolarFlux() {
		return publishToSolarFlux;
	}

	/**
	 * Set the "publish to SolarFlux" toggle.
	 * 
	 * @param publishToSolarFlux
	 *        {@literal true} if data from this group should be published to
	 *        SolarFlux
	 */
	public void setPublishToSolarFlux(boolean publishToSolarFlux) {
		this.publishToSolarFlux = publishToSolarFlux;
	}

	/**
	 * Get the source ID template.
	 * 
	 * @return the source ID template
	 */
	public String getSourceIdTemplate() {
		return sourceIdTemplate;
	}

	/**
	 * Set the source ID template.
	 * 
	 * @param sourceIdTemplate
	 *        the template to set
	 */
	public void setSourceIdTemplate(String sourceIdTemplate) {
		this.sourceIdTemplate = sourceIdTemplate;
	}

	/**
	 * Get the node ID.
	 * 
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the node ID.
	 * 
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
}
