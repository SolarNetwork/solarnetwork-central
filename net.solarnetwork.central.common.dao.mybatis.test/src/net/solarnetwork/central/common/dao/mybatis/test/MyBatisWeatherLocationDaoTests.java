/* ==================================================================
 * MyBatisWeatherLocationDaoTests.java - Nov 10, 2014 10:00:49 AM
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

package net.solarnetwork.central.common.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarLocationDao;
import net.solarnetwork.central.dao.mybatis.MyBatisWeatherLocationDao;
import net.solarnetwork.central.dao.mybatis.MyBatisWeatherSourceDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;
import net.solarnetwork.central.domain.WeatherSource;
import net.solarnetwork.central.support.SourceLocationFilter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisWeatherLocationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisWeatherLocationDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisWeatherLocationDao dao;
	private MyBatisWeatherSourceDao weatherSourceDao;
	private MyBatisSolarLocationDao solarLocationDao;

	private SolarLocation location = null;
	private WeatherLocation weatherLocation = null;
	private WeatherSource weatherSource = null;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisWeatherLocationDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		weatherSourceDao = new MyBatisWeatherSourceDao();
		weatherSourceDao.setSqlSessionFactory(getSqlSessionFactory());
		solarLocationDao = new MyBatisSolarLocationDao();
		solarLocationDao.setSqlSessionFactory(getSqlSessionFactory());

		setupTestLocation();

		this.location = solarLocationDao.get(TEST_LOC_ID);
		this.weatherSource = weatherSourceDao.get(TEST_WEATHER_SOURCE_ID);
	}

	private WeatherLocation newWeatherLocationInstance() {
		WeatherLocation loc = new WeatherLocation();
		loc.setCreated(new DateTime());
		loc.setLocation(this.location);
		loc.setSource(this.weatherSource);
		loc.setSourceData("Test source data");
		return loc;
	}

	@Test
	public void storeNew() {
		WeatherLocation loc = newWeatherLocationInstance();
		Long id = dao.store(loc);
		assertNotNull(id);
		loc.setId(id);
		weatherLocation = loc;
	}

	private void validate(WeatherLocation src, WeatherLocation entity) {
		assertNotNull("UserNodeConfirmation should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getLocation(), entity.getLocation());
		assertEquals(src.getSource(), entity.getSource());
		assertEquals(src.getSourceData(), entity.getSourceData());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		WeatherLocation loc = dao.get(weatherLocation.getId());
		validate(weatherLocation, loc);
	}

	@Test
	public void update() {
		storeNew();
		WeatherLocation loc = dao.get(weatherLocation.getId());
		loc.setSourceData("new data");
		Long newId = dao.store(loc);
		assertEquals(loc.getId(), newId);
		WeatherLocation loc2 = dao.get(weatherLocation.getId());
		validate(loc, loc2);
	}

	@Test
	public void findByName() {
		WeatherSource source = new WeatherSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = weatherSourceDao.get(weatherSourceDao.store(source));

		WeatherLocation loc = newWeatherLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		WeatherLocation found = dao.getWeatherLocationForName(source.getName(), null);
		assertNotNull(found);
		validate(loc, found);
	}

	@Test
	public void findByNameAndFilter() {
		WeatherSource source = new WeatherSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = weatherSourceDao.get(weatherSourceDao.store(source));

		WeatherLocation loc = newWeatherLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		SolarLocation filter = new SolarLocation();
		filter.setCountry(TEST_LOC_COUNTRY);
		filter.setRegion(TEST_LOC_REGION);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);

		WeatherLocation found = dao.getWeatherLocationForName(source.getName(), filter);
		assertNotNull(found);
		validate(loc, found);
	}

	@Test
	public void findBySourceFilter() {
		WeatherSource source = new WeatherSource();
		source.setName(getClass().getSimpleName() + " test source");
		source = weatherSourceDao.get(weatherSourceDao.store(source));

		WeatherLocation loc = newWeatherLocationInstance();
		loc.setSource(source);
		loc = dao.get(dao.store(loc));

		SourceLocationFilter filter = new SourceLocationFilter(source.getName(), loc.getLocationName());
		FilterResults<SourceLocationMatch> results = dao.findFiltered(filter, null, null, null);
		assertNotNull(results);
		assertEquals(Integer.valueOf(1), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		SourceLocationMatch match = results.getResults().iterator().next();
		assertEquals(loc.getId(), match.getId());
		assertEquals(TEST_LOC_ID, match.getLocationId());
		assertEquals(source.getName(), match.getSourceName());
	}

}
