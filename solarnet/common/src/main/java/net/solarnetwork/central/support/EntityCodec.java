/* ==================================================================
 * EntityCodec.java - 20/03/2026 6:28:41 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.support;

/**
 * API for a service that can map an entity to/from some external form, and
 * provide ID values for them.
 * 
 * @author matt
 * @version 1.0
 */
public interface EntityCodec<T, K, S> {

	/**
	 * Serialize an entity to external form.
	 *
	 * @param entity
	 *        the entity to serialize
	 * @return the JSON form of {@code entity}
	 */
	S serialize(T entity);

	/**
	 * Deserialize an entity from external form.
	 *
	 * @param json
	 *        the JSON to deserialize
	 * @return the entity
	 */
	T deserialize(S json);

	/**
	 * Get the ID for an entity.
	 *
	 * @param entity
	 *        the entity to get the ID for
	 * @return the entity's ID
	 */
	K entityId(T entity);

}
