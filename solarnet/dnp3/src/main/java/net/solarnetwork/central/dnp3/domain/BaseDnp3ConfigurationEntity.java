/* ==================================================================
 * BaseDnp3ServerConfigurationEntity.java - 6/08/2023 10:06:17 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serializable;
import java.time.Instant;
import net.solarnetwork.central.dao.UserRelatedEntity;
import net.solarnetwork.central.domain.CompositeKey;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.CopyingIdentity;
import net.solarnetwork.domain.Differentiable;

/**
 * Base DNP3 user-related configuration entity, where the first composite key
 * component is a Long user ID.
 * 
 * <p>
 * The composite key might be user related, or server related, for example.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseDnp3ConfigurationEntity<C extends BaseDnp3ConfigurationEntity<C, K>, K extends CompositeKey & Comparable<K> & Serializable>
		extends BasicEntity<K> implements UserRelatedEntity<K>, CopyingIdentity<K, C>, Differentiable<C>,
		Serializable, Cloneable {

	private static final long serialVersionUID = 1608207090197702797L;

	private Instant modified;
	private boolean enabled;

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
	public BaseDnp3ConfigurationEntity(K id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
	}

	@SuppressWarnings("unchecked")
	@Override
	public C clone() {
		return (C) super.clone();
	}

	@Override
	public void copyTo(C entity) {
		entity.setModified(modified);
		entity.setEnabled(enabled);
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
	public boolean isSameAs(C other) {
		return (this.enabled == other.isEnabled());
	}

	@Override
	public boolean differsFrom(C other) {
		return !isSameAs(other);
	}

	@Override
	public Long getUserId() {
		K pk = getId();
		return (pk != null ? (Long) pk.keyComponent(0) : null);
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
	 * Get the enabled flag.
	 * 
	 * @return {@literal true} if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag.
	 * 
	 * @param enabled
	 *        the value to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
