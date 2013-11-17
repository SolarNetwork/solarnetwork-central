/* ==================================================================
 * IbatisSolarLocationDao.java - Sep 9, 2011 2:28:03 PM
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

package net.solarnetwork.central.dao.ibatis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;

/**
 * Ibatis implementation of {@link SolarLocationDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class IbatisSolarLocationDao extends
		IbatisFilterableDaoSupport<SolarLocation, LocationMatch, Location> implements SolarLocationDao {

	/** The query name used for {@link #getSolarLocationForName(String)}. */
	public static final String QUERY_FOR_NAME = "find-SolarLocation-for-name";

	/**
	 * The query name used for
	 * {@link #getSolarLocationForTimeZone(String, String)}.
	 */
	public static final String QUERY_FOR_COUNTRY_TIME_ZONE = "find-SolarLocation-for-country-timezone";

	/**
	 * Default constructor.
	 */
	public IbatisSolarLocationDao() {
		super(SolarLocation.class, LocationMatch.class);
	}

	@Override
	public SolarLocation getSolarLocationForName(String locationName) {
		@SuppressWarnings("unchecked")
		List<SolarLocation> results = getSqlMapClientTemplate().queryForList(QUERY_FOR_NAME,
				locationName, 0, 1);
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

	@Override
	public SolarLocation getSolarLocationForTimeZone(String country, String timeZoneId) {
		Map<String, String> params = new HashMap<String, String>(2);
		params.put("country", country);
		params.put("timeZoneId", timeZoneId);
		@SuppressWarnings("unchecked")
		List<SolarLocation> results = getSqlMapClientTemplate().queryForList(
				QUERY_FOR_COUNTRY_TIME_ZONE, params, 0, 1);
		if ( results.size() > 0 ) {
			return results.get(0);
		}
		return null;
	}

}
