/* ==================================================================
 * ObjectDatumStreamAliasEntityDao.java - 28/03/2026 6:27:41 am
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

import java.util.UUID;
import net.solarnetwork.central.common.dao.FilterableDeleteDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link ObjectDatumStreamAliasEntity} entities.
 *
 * @author matt
 * @version 1.1
 */
public interface ObjectDatumStreamAliasEntityDao extends GenericDao<ObjectDatumStreamAliasEntity, UUID>,
		FilterableDao<ObjectDatumStreamAliasEntity, UUID, ObjectDatumStreamAliasFilter>,
		FilterableDeleteDao<ObjectDatumStreamAliasFilter> {

	/**
	 * Test if an alias of a stream may resolve to that stream.
	 *
	 * <p>
	 * The original stream does not have to exist, so this reveals nothing about
	 * it.
	 * </p>
	 *
	 * @param originalNodeId
	 *        the original stream node ID
	 * @param originalSourceId
	 *        the original stream source ID
	 * @param aliasNodeId
	 *        the alias node ID
	 * @return {@code true} if an alias on {@code aliasNodeId} may resolve to
	 *         the original stream
	 * @since 1.1
	 */
	boolean aliasAllowed(Long originalNodeId, String originalSourceId, Long aliasNodeId);

}
