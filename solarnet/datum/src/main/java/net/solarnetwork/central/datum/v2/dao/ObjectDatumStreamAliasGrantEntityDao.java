/* ==================================================================
 * ObjectDatumStreamAliasGrantEntityDao.java - 31/10/2026 4:18:20 pm
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

package net.solarnetwork.central.datum.v2.dao;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.common.dao.GenericCompositeKey2Dao;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;

/**
 * DAO API for {@link ObjectDatumStreamAliasGrantEntity} entities.
 *
 * @author matt
 * @version 1.0
 */
public interface ObjectDatumStreamAliasGrantEntityDao extends
		GenericCompositeKey2Dao<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK, Long, Long>,
		FilterableDao<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK, ObjectDatumStreamAliasGrantFilter>,
		UserModifiableEnabledStatusDao<ObjectDatumStreamAliasGrantFilter> {

	/**
	 * Find the streams an alias on a node would resolve to through the grants
	 * made to a user.
	 *
	 * <p>
	 * The result always agrees with
	 * {@link ObjectDatumStreamAliasEntityDao#aliasAllowed(Long, String, Long)}.
	 * Only the user, grant, and pagination criteria of {@code filter} are used,
	 * where the user criteria are the owners of the streams.
	 * </p>
	 *
	 * @param granteeId
	 *        the ID of the user granted access
	 * @param aliasNodeId
	 *        the ID of the node that would host the aliases
	 * @param filter
	 *        an optional filter
	 * @return the available streams, never {@code null}
	 */
	FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> findAvailableStreams(
			Long granteeId, Long aliasNodeId, @Nullable ObjectDatumStreamAliasGrantFilter filter);

}
