/* ==================================================================
 * EndpointConfigurationInput.java - 25/02/2024 7:47:43 am
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

package net.solarnetwork.central.user.din.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.UserUuidPK;

/**
 * DTO for datum input endpoint configuration.
 *
 * @author matt
 * @version 1.0
 */
public class EndpointConfigurationInput
		extends BaseDatumInputConfigurationInput<EndpointConfiguration, UserUuidPK> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	private Long nodeId;
	private String sourceId;

	@NotNull
	private Long transformId;

	/**
	 * Constructor.
	 */
	public EndpointConfigurationInput() {
		super();
	}

	@Override
	public EndpointConfiguration toEntity(UserUuidPK id, Instant date) {
		EndpointConfiguration conf = new EndpointConfiguration(requireNonNullArgument(id, "id"), date);
		populateConfiguration(conf);
		return conf;
	}

	@Override
	protected void populateConfiguration(EndpointConfiguration conf) {
		super.populateConfiguration(conf);
		conf.setName(name);
		conf.setNodeId(nodeId);
		conf.setSourceId(sourceId);
		conf.setTransformId(transformId);
	}

	/**
	 * Get the name.
	 *
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the node ID
	 */
	public Long getNodeId() {
		return nodeId;
	}

	/**
	 * Set the default node ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param nodeId
	 *        the node ID to set
	 */
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Get the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @return the source ID
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * Set the default source ID to use, if the associated transform does not
	 * provide one.
	 *
	 * @param sourceId
	 *        the source ID to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Get the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @return the ID
	 */
	public Long getTransformId() {
		return transformId;
	}

	/**
	 * Set the ID of the associated {@link TransformConfiguration} entity.
	 *
	 * @param transformId
	 *        the ID to set
	 */
	public void setTransformId(Long transformId) {
		this.transformId = transformId;
	}

}
