/* ==================================================================
 * DatumInputConfigurationInput.java - 25/02/2024 7:29:31 am
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

package net.solarnetwork.central.user.din.domain;

import java.time.Instant;
import net.solarnetwork.central.dao.UserRelatedEntity;

/**
 * API for datum input configuration input.
 *
 * @param <T>
 *        the entity type
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 1.0
 */
public interface DatumInputConfigurationInput<T extends UserRelatedEntity<K>, K> {

	/**
	 * Create an entity from the input properties and a given primary key.
	 *
	 * @param id
	 *        the primary key to use
	 * @param date
	 *        the creation date to use
	 * @return the new entity
	 */
	T toEntity(K id, Instant date);

	/**
	 * Create an entity from the input properties and a given primary key.
	 *
	 * <p>
	 * The current date will be used.
	 * </p>
	 *
	 * @param id
	 *        the primary key to use
	 * @return the new entity
	 */
	default T toEntity(K id) {
		return toEntity(id, Instant.now());
	}

}
