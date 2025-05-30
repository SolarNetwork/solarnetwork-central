/* ==================================================================
 * BaseUserModifiableEntity.java - 7/08/2023 10:47:02 am
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

package net.solarnetwork.central.dao;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.Serial;
import java.time.Instant;
import net.solarnetwork.central.domain.BasePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.dao.BasicEntity;

/**
 * Base mutable user-related configuration entity, where the first component of
 * its primary key is a Long user ID.
 *
 * @param <T>
 *        the identity type
 * @param <K>
 *        the key type
 * @author matt
 * @version 2.0
 */
public abstract class BaseUserModifiableEntity<T extends BaseUserModifiableEntity<T, K>, K extends UserRelatedCompositeKey<K>>
		extends BasicEntity<T, K> implements UserRelatedStdEntity<T, K> {

	@Serial
	private static final long serialVersionUID = -8201311252309117005L;

	private Instant modified;
	private boolean enabled;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 * @since 1.2
	 */
	public BaseUserModifiableEntity(K id) {
		super(requireNonNullArgument(id, "id"), null);
	}

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
	public BaseUserModifiableEntity(K id, Instant created) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
	}

	@Override
	public void copyTo(T entity) {
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
	public boolean isSameAs(T other) {
		return (this.enabled == other.isEnabled());
	}

	@Override
	public boolean differsFrom(T other) {
		return !isSameAs(other);
	}

	@Override
	public Long getUserId() {
		K pk = getId();
		return (pk != null ? (Long) pk.keyComponent(0) : null);
	}

	/**
	 * Get a short identifier string.
	 *
	 * @return the identifier, which includes the {@code modified} epoch
	 *         seconds, if available
	 * @since 1.1
	 */
	public String ident() {
		K id = getId();
		StringBuilder buf = new StringBuilder(64);
		if ( id instanceof BasePK pk ) {
			buf.append(pk.getId());
		} else if ( id != null ) {
			buf.append(id);
		}
		Instant mod = getModified();
		if ( mod != null ) {
			buf.append('.');
			buf.append(mod.getEpochSecond());
		}
		return buf.toString();
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
