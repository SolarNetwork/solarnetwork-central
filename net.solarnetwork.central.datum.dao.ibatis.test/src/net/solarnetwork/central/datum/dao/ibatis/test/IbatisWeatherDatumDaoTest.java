/* ==================================================================
 * IbatisConsumptionDatumDaoTest.java - Sep 11, 2011 4:11:28 PM
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

package net.solarnetwork.central.datum.dao.ibatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.Iterator;
import net.solarnetwork.central.datum.dao.ibatis.IbatisWeatherDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.SkyCondition;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.datum.domain.WeatherDatumMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link IbatisWeatherDatumDao} class.
 * 
 * @author matt
 * @version $Revision$
 */
public class IbatisWeatherDatumDaoTest extends AbstractIbatisDaoTestSupport {

	@Autowired
	private IbatisWeatherDatumDao dao;

	private WeatherDatum lastDatum;

	@Before
	public void setUp() throws Exception {
		lastDatum = null;
	}

	@Test
	public void storeNew() {
		WeatherDatum datum = new WeatherDatum();
		datum.setBarometerDelta("rising");
		datum.setBarometricPressure(1.0F);
		datum.setCondition(SkyCondition.Clear);
		datum.setCreated(new DateTime());
		datum.setDewPoint(2.0F);
		datum.setHumidity(1.0F);
		datum.setInfoDate(new DateTime());
		datum.setLocationId(TEST_WEATHER_LOC_ID);
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(new DateTime());
		datum.setSkyConditions("clear");
		datum.setSourceId("test.source");
		datum.setTemperatureCelsius(1.5F);
		datum.setUvIndex(1);
		datum.setVisibility(1.2F);
		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(WeatherDatum src, WeatherDatum entity) {
		assertNotNull("ConsumptionDatum should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getBarometerDelta(), entity.getBarometerDelta());
		assertEquals(src.getBarometricPressure(), entity.getBarometricPressure());
		//assertEquals(src.getCondition(), entity.getCondition());
		assertEquals(src.getDewPoint(), entity.getDewPoint());
		assertEquals(src.getHumidity(), entity.getHumidity());
		assertEquals(src.getInfoDate(), entity.getInfoDate());
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getSkyConditions(), entity.getSkyConditions());
		assertEquals(src.getTemperatureCelsius(), entity.getTemperatureCelsius());
		assertEquals(src.getUvIndex(), entity.getUvIndex());
		assertEquals(src.getVisibility(), entity.getVisibility());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		WeatherDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void findByPostalCode() {
		storeNew();
		SolarLocation locFilter = new SolarLocation();
		locFilter.setPostalCode(TEST_LOC_POSTAL_CODE);
		DatumFilterCommand filter = new DatumFilterCommand(locFilter);

		FilterResults<WeatherDatumMatch> results = dao.findFiltered(filter, filter.getSortDescriptors(),
				null, null);

		assertNotNull(results);
		assertEquals(Integer.valueOf(1), results.getReturnedResultCount());
		assertNotNull(results.getResults());
		Iterator<WeatherDatumMatch> itr = results.getResults().iterator();

		WeatherDatumMatch match = itr.next();
		assertEquals(lastDatum.getId(), match.getId());
	}

}
