/* ==================================================================
 * BaseExportConfigurationEntity.java - 21/03/2018 1:46:50 PM
 *
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.domain;

import java.io.Serial;
import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.solarnetwork.central.dao.BaseIdentifiableUserModifiableEntity;
import net.solarnetwork.central.domain.UserLongCompositePK;

/**
 * Base class for export configuration entities.
 *
 * @author matt
 * @version 1.2
 */
public abstract class BaseExportConfigurationEntity<C extends BaseIdentifiableUserModifiableEntity<C, UserLongCompositePK>>
		extends BaseIdentifiableUserModifiableEntity<C, UserLongCompositePK> {

	@Serial
	private static final long serialVersionUID = 6321748992039317099L;

	private transient Long configId;

	/**
	 * Constructor.
	 * 
	 * @param id
	 *        the primary key
	 * @param created
	 *        the creation date
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public BaseExportConfigurationEntity(UserLongCompositePK id, Instant created) {
		super(id, created);
	}

	/**
	 * Get the configuration ID.
	 *
	 * @return the configuration ID
	 * @since 1.2
	 */
	@JsonProperty("id")
	public Long getConfigId() {
		if ( configId != null ) {
			return configId;
		}
		UserLongCompositePK id = getId();
		return (id != null ? id.getEntityId() : null);
	}

	/**
	 * Set the temporary configuration ID.
	 * 
	 * <p>
	 * This method is here to support DAO mapping that wants to set new primary
	 * key values on creation.
	 * </p>
	 * 
	 * @param configId
	 *        the configuration ID to set
	 */
	public final void setConfigId(Long configId) {
		this.configId = configId;
	}

}
