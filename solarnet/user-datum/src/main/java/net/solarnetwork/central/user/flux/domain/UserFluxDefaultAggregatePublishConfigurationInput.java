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

/**
 * DTO for user SolarFlux default aggregate publish configuration.
 * 
 * @author matt
 * @version 1.0
 */
public class UserFluxDefaultAggregatePublishConfigurationInput {

	private boolean publish;

	private boolean retain;

	/**
	 * Constructor.
	 */
	public UserFluxDefaultAggregatePublishConfigurationInput() {
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
	public UserFluxDefaultAggregatePublishConfiguration toEntity(Long id, Instant date) {
		UserFluxDefaultAggregatePublishConfiguration conf = new UserFluxDefaultAggregatePublishConfiguration(
				id, date);
		populateConfiguration(conf);
		return conf;
	}

	/**
	 * Populate input properties onto a configuration instance.
	 *
	 * @param conf
	 *        the configuration to populate
	 */
	private void populateConfiguration(UserFluxDefaultAggregatePublishConfiguration conf) {
		requireNonNullArgument(conf, "conf");
		conf.setModified(conf.getCreated());
		conf.setPublish(publish);
		conf.setRetain(retain);
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

}
