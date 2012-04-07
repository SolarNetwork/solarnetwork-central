/* ==================================================================
 * IbatisWeatherLocationDao.java - Oct 19, 2011 8:53:30 PM
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

package net.solarnetwork.central.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;

/**
 * iBATIS implementation of {@link WeatherLocationDao}.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisWeatherLocationDao 
extends IbatisFilterableDaoSupport<WeatherLocation, SourceLocationMatch, SourceLocation>
implements WeatherLocationDao {

	/** The query name used for {@link #getWeatherLocationForName(String,Location)}. */
	public static final String QUERY_FOR_NAME = "get-WeatherLocation-for-name";
	
	/**
	 * Default constructor.
	 */
	public IbatisWeatherLocationDao() {
		super(WeatherLocation.class, SourceLocationMatch.class);
	}

	@Override
	public WeatherLocation getWeatherLocationForName(String sourceName,
			Location locationFilter) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("sourceName", sourceName);
		params.put("filter", locationFilter);
		
		@SuppressWarnings("unchecked")
		List<WeatherLocation> results = getSqlMapClientTemplate().queryForList(
				QUERY_FOR_NAME, params, 0, 1);
		if ( results == null || results.size() == 0 ) {
			return null;
		}
		return results.get(0);
	}

}
