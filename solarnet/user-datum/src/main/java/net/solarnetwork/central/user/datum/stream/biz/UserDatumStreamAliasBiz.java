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

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.dao.FilterResults;

/**
 * Service API for user datum stream alias management.
 * 
 * @author matt
 * @version 1.1
 */
public interface UserDatumStreamAliasBiz {

	/**
	 * The maximum number of aliases that can be saved at once.
	 *
	 * @since 1.1
	 */
	int MAX_BULK_ALIASES = 1000;

	/**
	 * Get a specific alias for a given ID.
	 *
	 * @param id
	 *        the primary key of the alias to get
	 * @return the entity
	 * @throws AuthorizationException
	 *         if the entity is not available
	 */
	ObjectDatumStreamAliasEntity aliasForUser(Long userId, UUID id);

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
	 * Save an alias entity for a user.
	 *
	 * <p>
	 * An assigned {@code aliasId} updates an existing alias: it cannot be used
	 * to create an alias with a specific ID.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the entity owner
	 * @param aliasId
	 *        the ID of the entity to save
	 * @param input
	 *        the entity input to save
	 * @return the saved entity
	 * @throws AuthorizationException
	 *         if updating the entity is not allowed
	 */
	ObjectDatumStreamAliasEntity saveAlias(Long userId, UUID aliasId,
			ObjectDatumStreamAliasEntityInput input);

	/**
	 * Create a set of new alias entities for a user.
	 *
	 * <p>
	 * Either all the aliases are created, or none are.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the entity owner
	 * @param inputs
	 *        the entity inputs to save, up to {@link #MAX_BULK_ALIASES}
	 * @return the saved entities, in the same order as {@code inputs}
	 * @throws AuthorizationException
	 *         if creating any entity is not allowed
	 * @throws IllegalArgumentException
	 *         if too many inputs are provided
	 * @since 1.1
	 */
	List<ObjectDatumStreamAliasEntity> saveAliases(Long userId,
			List<ObjectDatumStreamAliasEntityInput> inputs);

	/**
	 * Get a list of all available alias entities for a given user.
	 *
	 * @param userId
	 *        the user ID to get entities for
	 * @param filter
	 *        an optional filter
	 */
	void deleteAliases(Long userId, @Nullable ObjectDatumStreamAliasFilter filter);

	/**
	 * Get a specific alias grant for a given ID.
	 *
	 * @param userId
	 *        the user ID of the grant owner
	 * @param grantId
	 *        the ID of the grant to get
	 * @return the entity
	 * @throws AuthorizationException
	 *         if the entity is not available
	 * @since 1.1
	 */
	ObjectDatumStreamAliasGrantEntity aliasGrantForUser(Long userId, Long grantId);

	/**
	 * Get a list of all available alias grants owned by a given user.
	 *
	 * @param userId
	 *        the user ID of the grant owner
	 * @param filter
	 *        an optional filter
	 * @return the available entities, never {@code null}
	 * @since 1.1
	 */
	FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> listAliasGrants(Long userId,
			@Nullable ObjectDatumStreamAliasGrantFilter filter);

	/**
	 * Get a list of the enabled alias grants made to a given user.
	 *
	 * <p>
	 * Disabled grants are never included, so a grant is not revealed until its
	 * owner enables it. Any user criteria in {@code filter} limits the results
	 * to grants owned by those users.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the grantee
	 * @param filter
	 *        an optional filter
	 * @return the available entities, never {@code null}
	 * @since 1.1
	 */
	FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> listReceivedAliasGrants(
			Long userId, @Nullable ObjectDatumStreamAliasGrantFilter filter);

	/**
	 * Get a list of the streams of other accounts that can be aliased on a
	 * node.
	 *
	 * <p>
	 * Any user criteria in {@code filter} limits the results to streams owned
	 * by those users, and any grant criteria to streams matched by those
	 * grants. For a public node the results are limited to streams matched by a
	 * grant that allows public aliases.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the grantee
	 * @param aliasNodeId
	 *        the ID of the node that would host the aliases
	 * @param filter
	 *        an optional filter
	 * @return the available streams, never {@code null}
	 * @throws AuthorizationException
	 *         if the user does not own the node
	 * @since 1.1
	 */
	FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> listAvailableAliasStreams(
			Long userId, Long aliasNodeId, @Nullable ObjectDatumStreamAliasGrantFilter filter);

	/**
	 * Save an alias grant for a user.
	 *
	 * <p>
	 * A grant only ever applies to nodes the user owns at the time of a query,
	 * so the node ownership verified here is only to report mistakes.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the grant owner
	 * @param grantId
	 *        the ID of the grant to update, or
	 *        {@link net.solarnetwork.central.domain.EntityConstants#UNASSIGNED_LONG_ID}
	 *        to create a new grant
	 * @param input
	 *        the grant input to save
	 * @return the saved entity
	 * @throws AuthorizationException
	 *         if updating the entity is not allowed
	 * @since 1.1
	 */
	ObjectDatumStreamAliasGrantEntity saveAliasGrant(Long userId, Long grantId,
			ObjectDatumStreamAliasGrantEntityInput input);

	/**
	 * Update the enabled status of an alias grant.
	 *
	 * @param userId
	 *        the user ID of the grant owner
	 * @param grantId
	 *        the ID of the grant to update
	 * @param enabled
	 *        the enabled status to set
	 * @return the updated entity
	 * @throws AuthorizationException
	 *         if the entity is not available
	 * @since 1.1
	 */
	ObjectDatumStreamAliasGrantEntity updateAliasGrantEnabled(Long userId, Long grantId,
			boolean enabled);

	/**
	 * Delete an alias grant.
	 *
	 * <p>
	 * Any aliases that depend on the grant are not deleted, but no longer
	 * resolve to the user's streams.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the grant owner
	 * @param grantId
	 *        the ID of the grant to delete
	 * @since 1.1
	 */
	void deleteAliasGrant(Long userId, Long grantId);

}
