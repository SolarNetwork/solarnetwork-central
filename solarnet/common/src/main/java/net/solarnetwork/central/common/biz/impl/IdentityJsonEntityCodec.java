/* ==================================================================
 * IdentityJsonEntityCodec.java - 18/03/2026 5:53:34 pm
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

package net.solarnetwork.central.common.biz.impl;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import net.solarnetwork.central.support.EntityCodec;
import net.solarnetwork.domain.Identity;
import tools.jackson.databind.json.JsonMapper;

/**
 * Service to map {@link Identity} entities to/from JSON.
 *
 * @author matt
 * @version 1.0
 */
public class IdentityJsonEntityCodec<T extends Identity<K>, K extends Comparable<K>>
		implements EntityCodec<T, K, String> {

	private final JsonMapper jsonMapper;
	private final Class<? extends T> deserializeClass;

	public IdentityJsonEntityCodec(JsonMapper jsonMapper, Class<? extends T> deserializeClass) {
		super();
		this.jsonMapper = requireNonNullArgument(jsonMapper, "jsonMapper");
		this.deserializeClass = requireNonNullArgument(deserializeClass, "deserializeClass");
	}

	@Override
	public String serialize(T entity) {
		return jsonMapper.writeValueAsString(entity);
	}

	@Override
	public T deserialize(String json) {
		return jsonMapper.readValue(json, deserializeClass);
	}

	@Override
	public K entityId(T entity) {
		return entity.id();
	}

}
