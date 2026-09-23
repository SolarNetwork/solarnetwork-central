/* ==================================================================
 * UserMetadataFilter.java - 11/11/2016 11:07:58 AM
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

package net.solarnetwork.central.domain;

import net.solarnetwork.central.common.dao.SearchFilterCriteria;
import net.solarnetwork.central.common.dao.SecurityTokenCriteria;
import net.solarnetwork.central.common.dao.TagCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Filter API for user metadata.
 *
 * <p>
 * When token criteria are provided, the metadata of each result is restricted
 * to the {@code userMetadataPaths} of that token's security policy, and results
 * whose metadata is restricted to nothing are omitted.
 * </p>
 *
 * @author matt
 * @version 1.3
 * @since 2.0
 */
public interface UserMetadataFilter extends PaginationCriteria, UserCriteria, SearchFilterCriteria,
		TagCriteria, SecurityTokenCriteria {

}
