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
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration;
import net.solarnetwork.util.StringUtils;

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

	private int maxExecutionSeconds = EndpointConfiguration.DEFAULT_MAX_EXECUTION_SECONDS;

	private String userMetadataPath;

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
		conf.setMaxExecutionSeconds(maxExecutionSeconds);
		conf.setUserMetadataPath(userMetadataPath);
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
	 * Get the node IDs as a comma-delimited string.
	 *
	 * @return the delimited string
	 */
	public String getNodeIdsValue() {
		return StringUtils.commaDelimitedStringFromCollection(nodeIds);
	}

	/**
	 * Set the node IDs set as a comma-delimited string.
	 *
	 * @param value
	 *        the comma-delimited string of node IDs to set
	 */
	public void setNodeIdsValue(String value) {
		Set<String> vals = StringUtils.commaDelimitedStringToSet(value);
		Set<Long> nums = null;
		if ( vals != null ) {
			nums = new LinkedHashSet<>(vals.size());
			for ( String val : vals ) {
				try {
					nums.add(Long.valueOf(val));
				} catch ( IllegalArgumentException e ) {
					// ignore and continue
				}
			}
			if ( nums.isEmpty() ) {
				nums = null;
			}
		}
		setNodeIds(nums);
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

	/**
	 * Get the maximum execution seconds.
	 *
	 * @return the seconds; defaults to {@link #DEFAULT_MAX_EXECUTION_SECONDS}
	 */
	public int getMaxExecutionSeconds() {
		return maxExecutionSeconds;
	}

	/**
	 * Set the maximum execution seconds.
	 *
	 * @param maxExecutionSeconds
	 *        the seconds to set; anything less than 1 will be saved as 1
	 */
	public void setMaxExecutionSeconds(int maxExecutionSeconds) {
		this.maxExecutionSeconds = (maxExecutionSeconds > 0 ? maxExecutionSeconds : 1);
	}

	/**
	 * Get the user metadata path to provide to the transforms.
	 *
	 * @return the userMetadataPath the user metadata path to extract
	 * @since 1.1
	 */
	public String getUserMetadataPath() {
		return userMetadataPath;
	}

	/**
	 * Set the user metadata path to provide to the transforms.
	 *
	 * @param userMetadataPath
	 *        the user metadata path to set
	 * @see net.solarnetwork.domain.datum.DatumMetadataOperations#metadataAtPath(String)
	 * @since 1.1
	 */
	public void setUserMetadataPath(String userMetadataPath) {
		this.userMetadataPath = userMetadataPath;
	}

}
