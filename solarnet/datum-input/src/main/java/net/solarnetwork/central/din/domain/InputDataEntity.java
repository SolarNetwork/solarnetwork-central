/* ==================================================================
 * InputDataEntity.java - 5/03/2024 10:37:22 am
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

package net.solarnetwork.central.din.domain;

import java.io.Serial;
import java.time.Instant;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.dao.BasicEntity;
import net.solarnetwork.domain.Differentiable;

/**
 * Input data entity, to support previous input tracking.
 *
 * @author matt
 * @version 1.0
 */
public class InputDataEntity extends BasicEntity<UserLongStringCompositePK>
		implements Differentiable<InputDataEntity> {

	@Serial
	private static final long serialVersionUID = -8181765903228308150L;

	private final byte @Nullable [] data;

	/**
	 * Constructor.
	 *
	 * @param id
	 *        the ID
	 * @param created
	 *        the creation date
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         if any argument except {@code data} is {@code null}
	 */
	public InputDataEntity(UserLongStringCompositePK id, Instant created, byte @Nullable [] data) {
		super(id, created);
		this.data = data;
	}

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param created
	 *        the creation date
	 * @param data
	 *        the data
	 * @throws IllegalArgumentException
	 *         if any argument except {@code data} is {@code null}
	 */
	public InputDataEntity(Long userId, Long nodeId, String sourceId, Instant created,
			byte @Nullable [] data) {
		this(new UserLongStringCompositePK(userId, nodeId, sourceId), created, data);
	}

	/**
	 * Test if this entity has the same property values as another.
	 *
	 * <p>
	 * The {@code id} and {@code created} properties are not compared.
	 * </p>
	 *
	 * @param other
	 *        the entity to compare to
	 * @return {@literal true} if the properties of this entity are equal to the
	 *         other's
	 */
	public boolean isSameAs(@Nullable InputDataEntity other) {
		if ( other == null ) {
			return false;
		}
		return Arrays.equals(data, other.data);
	}

	@Override
	public boolean differsFrom(@Nullable InputDataEntity other) {
		return !isSameAs(other);
	}

	/**
	 * Get the primary key.
	 *
	 * @return the primary key
	 */
	@SuppressWarnings("NullAway")
	public final UserLongStringCompositePK pk() {
		return getId();
	}

	/**
	 * Get the data.
	 *
	 * @return the data
	 */
	public final byte @Nullable [] getData() {
		return data;
	}

	/**
	 * Get the user ID.
	 *
	 * @return the user ID
	 */
	public final Long getUserId() {
		return pk().getUserId();
	}

	/**
	 * Get the node ID.
	 *
	 * @return the node ID
	 */
	public final Long getNodeId() {
		return pk().getGroupId();
	}

	/**
	 * Get the source ID.
	 *
	 * @return the source ID
	 */
	public final String getSourceId() {
		return pk().getEntityId();
	}

}
