/* ==================================================================
 * LocationRequestDao.java - 19/05/2022 1:58:50 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import java.util.List;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for location requests.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface LocationRequestDao extends GenericDao<LocationRequest, Long>,
		FilterableDao<LocationRequest, Long, LocationRequestCriteria> {

	/**
	 * Get a persisted entity by its primary key.
	 * 
	 * @param id
	 *        the primary key to retrieve
	 * @param filter
	 *        the optional filter
	 * @return the domain object, or {@literal null} if not available
	 */
	List<LocationRequest> find(Long id, LocationRequestCriteria filter);

	/**
	 * Remove persisted entities.
	 * 
	 * @param id
	 *        optional ID to delete
	 * @param filter
	 *        optional filter to delete
	 * @return the number of entities deleted
	 */
	int delete(Long id, LocationRequestCriteria filter);

}
