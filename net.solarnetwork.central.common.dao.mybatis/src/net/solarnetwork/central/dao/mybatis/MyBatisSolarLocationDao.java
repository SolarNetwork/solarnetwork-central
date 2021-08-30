/* ==================================================================
 * MyBatisSolarLocationDao.java - Nov 10, 2014 8:57:24 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao.mybatis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDao;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;

/**
 * MyBatis implementation of {@link SolarLocationDao}.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisSolarLocationDao
		extends BaseMyBatisFilterableDao<SolarLocation, LocationMatch, Location, Long>
		implements SolarLocationDao {

	/**
	 * The query name used for
	 * {@link #getSolarLocationForTimeZone(String, String)}.
	 */
	public static final String QUERY_FOR_COUNTRY_TIME_ZONE = "find-SolarLocation-for-country-timezone";

	/**
	 * The query name used for {@link #getSolarLocationForLocation(Location)}.
	 */
	public static final String QUERY_FOR_EXACT_LOCATION = "find-SolarLocation-for-location";

	/**
	 * The query name used for {@link #getSolarLocationForNode(Long)}.
	 * 
	 * @since 1.1
	 */
	public static final String QUERY_FOR_NODE = "find-SolarLocation-for-node";

	/**
	 * Default constructor.
	 */
	public MyBatisSolarLocationDao() {
		super(SolarLocation.class, Long.class, LocationMatch.class);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public SolarLocation getSolarLocationForTimeZone(String country, String timeZoneId) {
		Map<String, String> params = new HashMap<String, String>(2);
		params.put("country", country);
		params.put("timeZoneId", timeZoneId);
		return selectFirst(QUERY_FOR_COUNTRY_TIME_ZONE, params);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public SolarLocation getSolarLocationForLocation(Location criteria) {
		return selectFirst(QUERY_FOR_EXACT_LOCATION, criteria);
	}

	@Override
	protected void postProcessFilterProperties(Location filter, Map<String, Object> sqlProps) {
		StringBuilder fts = new StringBuilder();
		spaceAppend(filter.getName(), fts);
		spaceAppend(filter.getCountry(), fts);
		spaceAppend(filter.getRegion(), fts);
		spaceAppend(filter.getStateOrProvince(), fts);
		spaceAppend(filter.getLocality(), fts);
		spaceAppend(filter.getPostalCode(), fts);
		if ( fts.length() > 0 ) {
			sqlProps.put("fts", fts.toString());
		}
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public SolarLocation getSolarLocationForNode(Long nodeId) {
		return selectFirst(QUERY_FOR_NODE, nodeId);
	}

}
