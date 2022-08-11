/* ==================================================================
 * BaseOscpConfigurationEntity.java - 11/08/2022 9:45:01 am
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

package net.solarnetwork.central.oscp.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.UserLongPK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.dao.Entity;

/**
 * Base OSCP configuration entity.
 * 
 * @author matt
 * @version 1.0
 */
@JsonIgnoreProperties({ "id" })
@JsonPropertyOrder({ "userId", "configId", "created", "name" })
public abstract class BaseOscpConfigurationEntity extends BasicEntity<UserLongPK>
		implements Entity<UserLongPK>, UserRelatedEntity<UserLongPK>, Serializable, Cloneable {

	private static final long serialVersionUID = 3413718946531052354L;

	private String name;

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
	public BaseOscpConfigurationEntity(UserLongPK id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
	}

	/**
	 * Constructor.
	 * 
	 * @param user
	 *        ID the user ID
	 * @param entityId
	 *        the entity ID
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseOscpConfigurationEntity(Long userId, Long entityId, Instant created) {
		super(new UserLongPK(userId, entityId), created);
	}

	@Override
	public BaseOscpConfigurationEntity clone() {
		return (BaseOscpConfigurationEntity) super.clone();
	}

	@Override
	public Long getUserId() {
		return getId().getUserId();
	}

	@JsonProperty("configId")
	public Long getEntityId() {
		return getId().getEntityId();
	}

	/**
	 * Get a display name for the configuration.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set a display name for the configuration.
	 * 
	 * @param name
	 *        the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

}
