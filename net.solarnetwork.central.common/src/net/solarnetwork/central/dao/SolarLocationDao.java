/* ==================================================================
 * SolarLocationDao.java - Sep 9, 2011 11:13:38 AM
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

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.SolarLocation;

/**
 * DAO API for Location.
 * 
 * @author matt
 * @version $Revision$
 */
public interface SolarLocationDao extends GenericDao<SolarLocation, Long> {

	/**
	 * Find the first SolarLocation for a given location name.
	 * 
	 * <p>Note that as location names are not unique, this merely returns
	 * the first name that matches exactly.</p>
	 * 
	 * @param locationName the location name
	 * @return the SolarLocation, or <em>null</em> if not found
	 */
	SolarLocation getSolarLocationForName(String locationName);

}
