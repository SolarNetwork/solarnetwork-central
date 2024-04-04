/* ==================================================================
 * UserMetadataDao.java - 11/11/2016 11:10:41 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.domain.UserMetadataFilterMatch;

/**
 * DAO API for {@link UserMetadataEntity}.
 * 
 * @author matt
 * @version 1.1
 * @since 1.23
 */
public interface UserMetadataDao extends GenericDao<UserMetadataEntity, Long>,
		FilterableDao<UserMetadataFilterMatch, Long, UserMetadataFilter> {

	/**
	 * Extract metadata at a given path as a JSON string.
	 * 
	 * <p>
	 * The {@code path} is a URL-like path, such as {@literal /pm/some/thing}.
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
