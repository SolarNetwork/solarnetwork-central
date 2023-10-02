/* ==================================================================
 * StreamMetadataCriteria.java - 19/11/2020 4:05:59 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.common.dao.LocationCriteria;
import net.solarnetwork.central.common.dao.SearchFilterCriteria;
import net.solarnetwork.central.common.dao.SecurityTokenCriteria;
import net.solarnetwork.central.common.dao.SourceCriteria;
import net.solarnetwork.central.common.dao.UserCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Search criteria for stream metadata related data.
 * 
 * @author matt
 * @version 1.1
 * @since 3.8
 */
public interface StreamMetadataCriteria extends StreamCriteria, SourceCriteria, LocationCriteria,
		UserCriteria, SecurityTokenCriteria, PropertyNameCriteria, SortCriteria, SearchFilterCriteria {

}
