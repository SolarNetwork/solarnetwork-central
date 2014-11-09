/* ==================================================================
 * MyBatisWeatherLocationDao.java - Nov 10, 2014 9:57:19 AM
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
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.WeatherLocationDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisFilterableDaoSupport;
import net.solarnetwork.central.domain.Location;
import net.solarnetwork.central.domain.SourceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;
import org.apache.ibatis.session.RowBounds;

/**
 * MyBatis implementation of {@link WeatherLocationDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisWeatherLocationDao extends
		BaseMyBatisFilterableDaoSupport<WeatherLocation, SourceLocationMatch, SourceLocation, Long>
		implements WeatherLocationDao {

	/**
	 * The query name used for
	 * {@link #getWeatherLocationForName(String,Location)}.
	 */
	public static final String QUERY_FOR_NAME = "get-WeatherLocation-for-name";

	/**
	 * Default constructor.
	 */
	public MyBatisWeatherLocationDao() {
		super(WeatherLocation.class, Long.class, SourceLocationMatch.class);
	}

	@Override
	public WeatherLocation getWeatherLocationForName(String sourceName, Location locationFilter) {
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("sourceName", sourceName);
		params.put("filter", locationFilter);

		List<WeatherLocation> results = getSqlSession().selectList(QUERY_FOR_NAME, params,
				new RowBounds(0, 1));
		if ( results == null || results.size() == 0 ) {
			return null;
		}
		return results.get(0);
	}

	@Override
	protected void postProcessFilterProperties(SourceLocation filter, Map<String, Object> sqlProps) {
		if ( filter.getLocation() != null ) {
			Location loc = filter.getLocation();
			StringBuilder fts = new StringBuilder();
			spaceAppend(loc.getName(), fts);
			spaceAppend(loc.getCountry(), fts);
			spaceAppend(loc.getRegion(), fts);
			spaceAppend(loc.getStateOrProvince(), fts);
			spaceAppend(loc.getLocality(), fts);
			spaceAppend(loc.getPostalCode(), fts);
			if ( fts.length() > 0 ) {
				sqlProps.put("fts", fts.toString());
			}
		}
	}

}
