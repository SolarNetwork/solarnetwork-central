/* ==================================================================
 * LocationAdminBiz.java - Jun 13, 2011 8:29:48 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.biz;

import net.solarnetwork.central.dras.domain.Location;

/**
 * Location administrator API.
 * 
 * @author matt
 * @version $Revision$
 */
public interface LocationAdminBiz {

	/**
	 * Create or update a Location.
	 * 
	 * <p>If the {@code template} {@link Location#getId()} value is present,
	 * that persisted Location will be updated. Otherwise a new Location will be
	 * persisted.</p>
	 * 
	 * <p>This method does <b>not</b> modify an existing group's member set.</p>
	 * 
	 * @param template the location data
	 * @return the persisted Location
	 */
	Location storeLocation(Location template);

}
