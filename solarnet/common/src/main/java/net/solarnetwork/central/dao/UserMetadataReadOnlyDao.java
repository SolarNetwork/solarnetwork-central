/* ==================================================================
 * UserMetadataReadonlyDao.java - 27/11/2025 2:15:05 pm
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

import net.solarnetwork.central.domain.UserMetadataEntity;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.dao.FilterableDao;

/**
 * Read-only DAO API for {@link UserMetadataEntity} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface UserMetadataReadOnlyDao
		extends FilterableDao<UserMetadataEntity, Long, UserMetadataFilter> {

	/**
	 * Get a persisted entity by its primary key.
	 *
	 * @param id
	 *        the primary key to retrieve
	 * @return the domain object, or {@code null} if not available
	 */
	UserMetadataEntity get(Long id);

	/**
	 * Extract metadata at a given path as a JSON string.
	 *
	 * <p>
	 * The {@code path} is a URL-like path, such as {@code /pm/some/thing}.
	 * </p>
	 *
	 * @param userId
	 *        the user ID to extract metadata for
	 * @param path
	 *        the path to extract
	 * @return the metadata object
	 * @see net.solarnetwork.domain.datum.DatumMetadataOperations#metadataAtPath(String)
	 */
	String jsonMetadataAtPath(Long userId, String path);

}
