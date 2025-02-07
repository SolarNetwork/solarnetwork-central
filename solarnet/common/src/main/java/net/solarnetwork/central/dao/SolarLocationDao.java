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
 */

package net.solarnetwork.central.dao;

import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for Location.
 *
 * @author matt
 * @version 1.5
 */
public interface SolarLocationDao
		extends GenericDao<SolarLocation, Long>, FilterableDao<LocationMatch, Long, Location> {

	/**
	 * Find a SolarLocation for just a country and time zone.
	 *
	 * @param country
	 *        the country
	 * @param timeZoneId
	 *        the time zone ID
	 * @return the SolarLocation, or {@literal null} if none found
	 */
	SolarLocation getSolarLocationForTimeZone(String country, String timeZoneId);

	/**
	 * Find a SolarLocation that exactly matches the given criteria. By exactly
	 * matching, even empty fields must match.
	 *
	 * @param criteria
	 *        the search criteria
	 * @return the matching location, or {@literal null} if not found
	 */
	SolarLocation getSolarLocationForLocation(Location criteria);

	/**
	 * Get the location associated with a node.
	 *
	 * @param nodeId
	 *        the node ID to get the location for
	 * @return the location, or {@literal null} if not found
	 * @since 1.4
	 */
	SolarLocation getSolarLocationForNode(Long nodeId);

}
