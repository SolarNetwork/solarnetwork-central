/* ==================================================================
 * MyBatisWeatherSourceDao.java - Nov 10, 2014 10:02:42 AM
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

import net.solarnetwork.central.dao.WeatherSourceDao;
import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDao;
import net.solarnetwork.central.domain.WeatherSource;

/**
 * MyBatis implementation of {@link WeatherSourceDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisWeatherSourceDao extends BaseMyBatisGenericDao<WeatherSource, Long> implements
		WeatherSourceDao {

	/** The query name used for {@link #getWeatherSourceForName(String)}. */
	public static final String QUERY_FOR_NAME = "get-WeatherSource-for-name";

	/**
	 * Default constructor.
	 */
	public MyBatisWeatherSourceDao() {
		super(WeatherSource.class, Long.class);
	}

	@Override
	public WeatherSource getWeatherSourceForName(String sourceName) {
		return selectFirst(QUERY_FOR_NAME, sourceName);
	}

}
