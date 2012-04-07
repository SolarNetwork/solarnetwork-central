/* ==================================================================
 * WeatherLocationDao.java - Oct 19, 2011 8:35:45 PM
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

import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;

/**
 * DAO API for WeatherLocation.
 * 
 * @author matt
 * @version $Revision$
 */
public interface WeatherLocationDao extends GenericDao<WeatherLocation, Long>,
FilterableDao<SourceLocationMatch, Long, SourceLocation> {

	/**
	 * Find a unique WeatherLocation for a given WeatherSource name and location name.
	 * 
	 * @param sourceName the WeatherSource name
	 * @param location the location filter
	 * @return the WeatherLocation, or <em>null</em> if not found
	 */
	WeatherLocation getWeatherLocationForName(String sourceName, Location locationFilter);

}
