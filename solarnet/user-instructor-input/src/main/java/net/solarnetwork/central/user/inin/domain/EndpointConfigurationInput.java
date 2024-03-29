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

package net.solarnetwork.central.user.inin.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;

/**
 * DTO for datum input endpoint configuration.
 *
 * @author matt
 * @version 1.1
 */
public class EndpointConfigurationInput
		extends BaseInstructionInputConfigurationInput<EndpointConfiguration, UserUuidPK> {

	@NotNull
	@NotBlank
	@Size(max = 64)
	private String name;

	private Set<Long> nodeIds;

	@NotNull
	private Long requestTransformId;

	@NotNull
	private Long responseTransformId;

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
		conf.setNodeIds(nodeIds);
		conf.setRequestTransformId(requestTransformId);
		conf.setResponseTransformId(responseTransformId);
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
	 * Get the default node IDs to use, if the associated transform does not
	 * provide any.
	 *
	 * @return the node IDs
	 */
	public Set<Long> getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the default node IDs to use, if the associated transform does not
	 * provide any.
	 *
	 * @param nodeIds
	 *        the node IsD to set
	 */
	public void setNodeIds(Set<Long> nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the ID of the associated request {@link TransformConfiguration}
	 * entity.
	 *
	 * @return the ID
	 */
	public Long getRequestTransformId() {
		return requestTransformId;
	}

	/**
	 * Set the ID of the associated request {@link TransformConfiguration}
	 * entity.
	 *
	 * @param requestTransformId
	 *        the ID to set
	 */
	public void setRequestTransformId(Long requestTransformId) {
		this.requestTransformId = requestTransformId;
	}

	/**
	 * Get the ID of the associated response {@link TransformConfiguration}
	 * entity.
	 *
	 * @return the ID
	 */
	public Long getResponseTransformId() {
		return responseTransformId;
	}

	/**
	 * Set the ID of the associated response {@link TransformConfiguration}
	 * entity.
	 *
	 * @param responseTransformId
	 *        the ID to set
	 */
	public void setResponseTransformId(Long responseTransformId) {
		this.responseTransformId = responseTransformId;
	}

}
