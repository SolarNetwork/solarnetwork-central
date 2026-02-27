/* ==================================================================
 * BasicUserEntity.java - 21/03/2025 5:15:54 pm
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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
import static net.solarnetwork.util.ObjectUtils.nonnull;
import java.io.Serial;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.BasePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.dao.BasicEntity;

/**
 * Base immutable user-related entity, where the first component of its primary
 * key is a Long user ID.
 * 
 * @author matt
 * @version 1.0
 * @see BaseUserModifiableEntity
 */
public abstract class BasicUserEntity<C extends BasicUserEntity<C, K>, K extends UserRelatedCompositeKey<K>>
		extends BasicEntity<K> implements UserRelatedStdEntity<C, K> {

	@Serial
	private static final long serialVersionUID = 1520924063897057459L;

	private final @Nullable Instant modified;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicUserEntity(K id) {
		super(requireNonNullArgument(id, "id"), null);
		this.modified = null;
	}

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param modified
	 *        the modification date
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public BasicUserEntity(K id, Instant created, Instant modified) {
		super(requireNonNullArgument(id, "id"), requireNonNullArgument(created, "created"));
		this.modified = requireNonNullArgument(modified, "modified");
	}

	@SuppressWarnings("unchecked")
	@Override
	public C clone() {
		return (C) super.clone();
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
	public abstract boolean isSameAs(@Nullable C other);

	@Override
	public boolean differsFrom(@Nullable C other) {
		return !isSameAs(other);
	}

	@Override
	public void copyTo(C entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Long getUserId() {
		return nonnull(getId(), "id").getUserId();
	}

	/**
	 * Get a short identifier string.
	 *
	 * @return the identifier, which includes the {@code modified} epoch
	 *         seconds, if available
	 */
	public String ident() {
		K id = getId();
		StringBuilder buf = new StringBuilder(64);
		if ( id instanceof BasePK pk ) {
			buf.append(pk.getId());
		} else if ( id != null ) {
			buf.append(id);
		}
		return buf.toString();
	}

	/**
	 * Get the modification date.
	 * 
	 * @return the modified date
	 */
	public final @Nullable Instant getModified() {
		return modified;
	}

}
