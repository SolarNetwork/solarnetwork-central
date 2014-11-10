/* ==================================================================
 * MyBatisWeatherSourceDaoTests.java - Nov 10, 2014 10:14:52 AM
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

package net.solarnetwork.central.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import net.solarnetwork.central.dao.mybatis.MyBatisWeatherSourceDao;
import net.solarnetwork.central.domain.WeatherSource;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisWeatherSourceDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisWeatherSourceDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisWeatherSourceDao dao;

	private WeatherSource weatherSource = null;

	@Test
	public void storeNew() {
		dao = new MyBatisWeatherSourceDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		WeatherSource d = new WeatherSource();
		d.setCreated(new DateTime());
		d.setName("Test name");
		Long id = dao.store(d);
		assertNotNull(id);
		d.setId(id);
		weatherSource = d;
	}

	private void validate(WeatherSource src, WeatherSource entity) {
		assertNotNull("WeatherSource should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getId(), entity.getId());
		assertEquals(src.getName(), entity.getName());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		WeatherSource d = dao.get(weatherSource.getId());
		validate(weatherSource, d);
	}

	@Test
	public void update() {
		storeNew();
		WeatherSource d = dao.get(weatherSource.getId());
		d.setName("new name");
		Long newId = dao.store(d);
		assertEquals(d.getId(), newId);
		WeatherSource d2 = dao.get(weatherSource.getId());
		validate(d, d2);
	}

	@Test
	public void findByName() {
		storeNew();
		WeatherSource s = dao.getWeatherSourceForName("Test name");
		assertNotNull(s);
		validate(weatherSource, s);
	}

}
