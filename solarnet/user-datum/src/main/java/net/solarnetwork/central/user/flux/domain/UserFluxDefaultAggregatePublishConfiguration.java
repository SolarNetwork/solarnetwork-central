/* ==================================================================
 * UserFluxDefaultAggregatePublishConfiguration.java - 25/06/2024 8:18:12 am
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

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.datum.flux.domain.FluxPublishSettings;
import net.solarnetwork.central.user.dao.UserRelatedEntity;
import net.solarnetwork.dao.BasicLongEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * User SolarFlux default aggregate publish configuration.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "created", "modified", "publish", "retain" })
public class UserFluxDefaultAggregatePublishConfiguration extends BasicLongEntity
		implements FluxPublishSettings, Differentiable<UserFluxDefaultAggregatePublishConfiguration>,
		UserRelatedEntity<Long> {

	private static final long serialVersionUID = -543871871370406533L;

	private Instant modified;
	private boolean publish;
	private boolean retain;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public UserFluxDefaultAggregatePublishConfiguration(Long id, Instant created) {
		super(id, created);
	}

	@Override
	public Long getUserId() {
		return getId();
	}

	@Override
	public UserFluxDefaultAggregatePublishConfiguration clone() {
		return (UserFluxDefaultAggregatePublishConfiguration) super.clone();
	}

	@Override
	public boolean differsFrom(UserFluxDefaultAggregatePublishConfiguration other) {
		return !isSameAs(other);
	}

	/**
	 * Test if this entity has the same property values as another.
	 * 
	 * <p>
	 * The {@code id}, {@code created}, and {@code modified} properties are not
	 * compared.
	 * </p>
	 * 
	 * @param other
	 *        the entity to compare to
	 * @return {@literal true} if the properties of this entity are equal to the
	 *         other's
	 */
	public boolean isSameAs(UserFluxDefaultAggregatePublishConfiguration other) {
		return (this.publish == other.publish && this.retain == other.retain);
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modified date
	 */
	public Instant getModified() {
		return modified;
	}

	/**
	 * Set the modification date.
	 * 
	 * @param modified
	 *        the modified date to set
	 */
	public void setModified(Instant modified) {
		this.modified = modified;
	}

	/**
	 * Get the publish mode.
	 * 
	 * @return {@code true} to publish messages for matching datum streams
	 */
	@Override
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
	@Override
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
