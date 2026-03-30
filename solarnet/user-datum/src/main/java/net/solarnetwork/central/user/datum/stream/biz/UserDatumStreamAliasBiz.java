/* ==================================================================
 * UserDatumStreamAliasBiz.java - 30/03/2026 9:58:15 am
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

package net.solarnetwork.central.user.datum.stream.biz;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for user datum stream alias management.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserDatumStreamAliasBiz {

	/**
	 * Get a specific alias for a given ID.
	 *
	 * @param id
	 *        the primary key of the alias to get
	 * @return the entity
	 * @throws AuthorizationException
	 *         if the entity is not available
	 */
	ObjectDatumStreamAliasEntity aliasForId(Long userId, UUID id);

	/**
	 * Get a list of all available alias entities for a given user.
	 *
	 * @param userId
	 *        the user ID to get entities for
	 * @param filter
	 *        an optional filter
	 * @return the available entities, never {@code null}
	 */
	FilterResults<ObjectDatumStreamAliasEntity, UUID> listAliases(Long userId,
			@Nullable ObjectDatumStreamAliasFilter filter);

	/**
	 * Save a cloud integration configuration for a user.
	 *
	 * @param userId
	 *        the user ID of the entity owner
	 * @param id
	 *        the ID of the entity to save
	 * @param input
	 *        the entity input to save
	 * @return the saved entity
	 * @throws AuthorizationException
	 *         if updating the entity is not allowed
	 */
	ObjectDatumStreamAliasEntity saveAlias(Long userId, UUID aliasId,
			ObjectDatumStreamAliasEntityInput input);

}
