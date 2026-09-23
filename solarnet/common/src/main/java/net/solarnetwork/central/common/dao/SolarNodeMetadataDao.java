/* ==================================================================
 * SolarNodeMetadataDao.java - 13/11/2024 7:40:59 am
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

package net.solarnetwork.central.common.dao;

import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link SolarNodeMetadata} entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface SolarNodeMetadataDao
		extends SolarNodeMetadataReadOnlyDao, GenericDao<SolarNodeMetadata, Long> {

	/**
	 * Get the metadata for a specific node ID.
	 * 
	 * <p>
	 * This method is declared here to resolve the otherwise ambiguous
	 * {@code get(Long)} inherited from both extended APIs.
	 * </p>
	 * 
	 * @param id
	 *        the ID of the node to get the metadata for
	 * @return the metadata, or {@code null} if none available
	 * @since 1.1
	 */
	@Override
	@Nullable
	SolarNodeMetadata get(Long id);

}
