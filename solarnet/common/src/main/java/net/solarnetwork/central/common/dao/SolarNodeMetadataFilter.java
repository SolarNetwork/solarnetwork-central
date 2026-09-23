/* ==================================================================
 * SolarNodeMetadataFilter.java - 12/11/2024 6:36:12 pm
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

import net.solarnetwork.dao.PaginationCriteria;

/**
 * Filter criteria for node metadata.
 * 
 * <p>
 * The {@link SearchFilterCriteria} is applied to the metadata JSON of each
 * node, using the node metadata path syntax, for example
 * {@code (/m/foo=bar)}.
 * </p>
 *
 * <p>
 * When token criteria are provided, results are limited to the nodes owned by
 * that token's user, the metadata of each result is restricted to the
 * {@code nodeMetadataPaths} of that token's security policy, and results whose
 * metadata is restricted to nothing are omitted.
 * </p>
 *
 * @author matt
 * @version 1.2
 */
public interface SolarNodeMetadataFilter
		extends NodeCriteria, SearchFilterCriteria, PaginationCriteria, SecurityTokenCriteria {

}
