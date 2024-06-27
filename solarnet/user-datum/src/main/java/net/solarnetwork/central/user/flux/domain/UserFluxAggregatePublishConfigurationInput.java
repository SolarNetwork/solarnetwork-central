/* ==================================================================
 * UserFluxAggregatePublishConfigurationInput.java - 25/06/2024 8:09:06 am
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

package net.solarnetwork.central.user.flux.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.util.StringUtils;

/**
 * DTO for user SolarFlux aggregate publish configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class UserFluxAggregatePublishConfigurationInput {

	private Long[] nodeIds;

	private String[] sourceIds;

	private boolean publish;

	private boolean retain;

	/**
	 * Constructor.
	 */
	public UserFluxAggregatePublishConfigurationInput() {
		super();
	}

	/**
	 * Create an entity from the input properties and a given primary key.
	 *
	 * @param id
	 *        the primary key to use
	 * @param date
	 *        the creation date to use
	 * @return the new entity
	 */
	public UserFluxAggregatePublishConfiguration toEntity(UserLongCompositePK id, Instant date) {
		UserFluxAggregatePublishConfiguration conf = new UserFluxAggregatePublishConfiguration(id, date);
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Populate input properties onto a configuration instance.
	 *
	 * @param conf
	 *        the configuration to populate
	 */
	private void populateConfiguration(UserFluxAggregatePublishConfiguration conf) {
		requireNonNullArgument(conf, "conf");
		conf.setModified(conf.getCreated());
		conf.setEnabled(true);
		if ( nodeIds != null && nodeIds.length > 0 ) {
			Long[] sorted = new Long[nodeIds.length];
			System.arraycopy(nodeIds, 0, sorted, 0, nodeIds.length);
			Arrays.sort(sorted);
			conf.setNodeIds(sorted);
		}
		if ( sourceIds != null && sourceIds.length > 0 ) {
			String[] sorted = new String[sourceIds.length];
			System.arraycopy(sourceIds, 0, sorted, 0, sourceIds.length);
			Arrays.sort(sorted);
			conf.setSourceIds(sorted);
		}
		conf.setPublish(publish);
		conf.setRetain(retain);
	}

	/**
	 * Get the node IDs.
	 * 
	 * @return the node IDs, or {@literal null} for any node
	 */
	public Long[] getNodeIds() {
		return nodeIds;
	}

	/**
	 * Set the node IDs.
	 * 
	 * @param nodeIds
	 *        the node IDs to set
	 */
	public void setNodeIds(Long[] nodeIds) {
		this.nodeIds = nodeIds;
	}

	/**
	 * Get the source IDs.
	 * 
	 * <p>
	 * Source ID Ant-style patterns are allowed.
	 * </p>
	 * 
	 * @return the source IDs
	 */
	public String[] getSourceIds() {
		return sourceIds;
	}

	/**
	 * Set the source IDs.
	 * 
	 * @param sourceIds
	 *        the source IDs or source ID Ant-style patterns to set
	 */
	public void setSourceIds(String[] sourceIds) {
		this.sourceIds = sourceIds;
	}

	/**
	 * Get the publish mode.
	 * 
	 * @return {@code true} to publish messages for matching datum streams
	 */
	public boolean isPublish() {
		return publish;
	}

	/**
	 * Set the publish mode.
	 * 
	 * @param publish
	 *        {@code true} to publish messages for matching datum streams
	 */
	public void setPublish(boolean publish) {
		this.publish = publish;
	}

	/**
	 * Get the message retain flag to use.
	 * 
	 * @return {@code true} to set the retain flag on published messages
	 */
	public boolean isRetain() {
		return retain;
	}

	/**
	 * Set the message retain flag to use.
	 * 
	 * @param retain
	 *        {@code true} to set the retain flag on published messages
	 */
	public void setRetain(boolean retain) {
		this.retain = retain;
	}

	/**
	 * Get the node IDs as a comma-delimited string.
	 *
	 * @return the delimited string
	 */
	public String getNodeIdsValue() {
		return StringUtils.commaDelimitedStringFromCollection(
				nodeIds != null ? Arrays.asList(nodeIds) : Collections.emptyList());
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
		setNodeIds(nums != null ? nums.toArray(Long[]::new) : null);
	}

	/**
	 * Get the source IDs as a comma-delimited string.
	 *
	 * @return the delimited string
	 */
	public String getSourceIdsValue() {
		return StringUtils.commaDelimitedStringFromCollection(
				sourceIds != null ? Arrays.asList(sourceIds) : Collections.emptyList());
	}

	/**
	 * Set the source IDs set as a comma-delimited string.
	 *
	 * @param value
	 *        the comma-delimited string of source IDs to set
	 */
	public void setSourceIdsValue(String value) {
		Set<String> vals = StringUtils.commaDelimitedStringToSet(value);
		setSourceIds(vals != null ? vals.toArray(String[]::new) : null);
	}
}
