/* ==================================================================
 * LocationSearchFilter.java - 7/02/2025 12:52:58 pm
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

package net.solarnetwork.central.query.domain;

import net.solarnetwork.central.domain.PaginationFilter;
import net.solarnetwork.central.domain.SourceLocation;

/**
 * Search filter for locations.
 * 
 * <p>
 * This class is used for API documentation only.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface LocationSearchFilter extends SourceLocation, PaginationFilter {

}
