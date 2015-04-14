/* ==================================================================
 * DaoDataCollectorBizTest.java - Oct 23, 2011 2:49:59 PM
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

package net.solarnetwork.central.in.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.HardwareControlDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.datum.domain.PriceDatum;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test case for the {@link DaoDataCollectorBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDataCollectorBizTest extends AbstractInBizDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";

	@Autowired
	private DaoDataCollectorBiz biz;

	private DatumMetadataBiz datumMetadataBiz;

	private Datum lastDatum;

	@Before
	public void setup() {
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		setupTestNode();
		setupTestPriceLocation();
		setAuthenticatedNode(TEST_NODE_ID);
	}

	private DayDatum newDayDatumInstance() {
		DayDatum d = new DayDatum();
		d.setSkyConditions("Sunny");
		d.setDay(new LocalDate(2011, 10, 21));
		d.setNodeId(TEST_NODE_ID);
		d.setSunrise(new LocalTime(6, 40));
		d.setSunset(new LocalTime(18, 56));
		return d;
	}

	@Test
	public void collectDay() {
		DayDatum d = newDayDatumInstance();
		@SuppressWarnings("deprecation")
		DayDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getDay(), result.getDay());
		assertNotNull(d.getLocationId());
		lastDatum = d;

		// verify created as GeneralLocationDatum
		GeneralLocationDatumPK pk = new GeneralLocationDatumPK();
		pk.setCreated(d.getDay().toDateTimeAtStartOfDay(DateTimeZone.forID(TEST_TZ)));
		pk.setLocationId(TEST_LOC_ID);
		pk.setSourceId(TEST_WEATHER_SOURCE_NAME + " Day");
		GeneralLocationDatum entity = biz.getGeneralLocationDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Sunrise", "06:40", entity.getSamples().getStatusSampleString("sunrise"));
		assertEquals("Sunset", "18:56", entity.getSamples().getStatusSampleString("sunset"));
	}

	@Test
	public void collectSameDay() {
		collectDay();
		DayDatum d = newDayDatumInstance();
		@SuppressWarnings("deprecation")
		DayDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertEquals(lastDatum.getId(), result.getId());
	}

	private WeatherDatum newWeatherDatumInstance() {
		WeatherDatum d = new WeatherDatum();
		d.setNodeId(TEST_NODE_ID);
		d.setSkyConditions("Sunny");
		d.setInfoDate(new DateTime(2015, 4, 14, 10, 30, DateTimeZone.forID(TEST_TZ)));
		d.setTemperatureCelsius(18f);
		d.setBarometricPressure(32f);
		d.setHumidity(70f);
		d.setVisibility(3.2f);
		return d;
	}

	@Test
	public void collectWeather() {
		WeatherDatum d = newWeatherDatumInstance();
		@SuppressWarnings("deprecation")
		WeatherDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getInfoDate(), result.getInfoDate());
		assertNotNull(d.getLocationId());
		lastDatum = d;

		// verify created as GeneralLocationDatum
		GeneralLocationDatumPK pk = new GeneralLocationDatumPK();
		pk.setCreated(d.getInfoDate());
		pk.setLocationId(TEST_LOC_ID);
		pk.setSourceId(TEST_WEATHER_SOURCE_NAME);
		GeneralLocationDatum entity = biz.getGeneralLocationDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Temp", d.getTemperatureCelsius().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("temp").doubleValue(), 0.001);
		assertEquals("Humidity", d.getHumidity().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("humidity").doubleValue(), 0.001);
		assertEquals("ATM", d.getBarometricPressure().doubleValue() * 100, entity.getSamples()
				.getInstantaneousSampleDouble("atm").doubleValue(), 0.001);
		assertEquals("Sky", d.getSkyConditions(), entity.getSamples().getStatusSampleString("sky"));
		assertEquals("Visibility", d.getVisibility().doubleValue() * 1000, entity.getSamples()
				.getInstantaneousSampleDouble("visibility"), 0.001);
	}

	@Test
	public void collectWeatherSameDate() {
		collectWeather();
		WeatherDatum d = newWeatherDatumInstance();
		@SuppressWarnings("deprecation")
		WeatherDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertEquals(lastDatum.getId(), result.getId());
	}

	private PriceDatum newPriceDatumInstance() {
		PriceDatum d = new PriceDatum();
		d.setNodeId(TEST_NODE_ID);
		d.setLocationId(TEST_LOC_ID);
		d.setCreated(new DateTime(2015, 4, 14, 10, 30, DateTimeZone.forID(TEST_TZ)));
		d.setPrice(23.50f);
		return d;
	}

	@Test
	public void collectPrice() {
		PriceDatum d = newPriceDatumInstance();
		@SuppressWarnings("deprecation")
		PriceDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getCreated(), result.getCreated());
		assertNotNull(d.getLocationId());
		lastDatum = d;

		// verify created as GeneralLocationDatum
		GeneralLocationDatumPK pk = new GeneralLocationDatumPK();
		pk.setCreated(d.getCreated());
		pk.setLocationId(TEST_LOC_ID);
		pk.setSourceId(TEST_PRICE_SOURCE_NAME);
		GeneralLocationDatum entity = biz.getGeneralLocationDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Price", d.getPrice().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("price").doubleValue(), 0.001);
	}

	private PowerDatum newPowerDatumInstance() {
		PowerDatum d = new PowerDatum();
		d.setNodeId(TEST_NODE_ID);
		d.setSourceId(TEST_SOURCE_ID);
		d.setCreated(new DateTime(2015, 4, 14, 10, 30, DateTimeZone.forID(TEST_TZ)));
		d.setWattHourReading(12345L);
		d.setWatts(350);
		return d;
	}

	@Test
	public void collectPower() {
		PowerDatum d = newPowerDatumInstance();
		@SuppressWarnings("deprecation")
		PowerDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getCreated(), result.getCreated());
		lastDatum = d;

		// verify created as GeneralNodeDatum
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK();
		pk.setCreated(d.getCreated());
		pk.setNodeId(TEST_NODE_ID);
		pk.setSourceId(TEST_SOURCE_ID);
		GeneralNodeDatum entity = biz.getGeneralNodeDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Watts", d.getWatts().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("watts").doubleValue(), 0.001);
		assertEquals("Wh", d.getWattHourReading().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("wattHours").doubleValue(), 0.001);
	}

	private ConsumptionDatum newConsumptionDatumInstance() {
		ConsumptionDatum d = new ConsumptionDatum();
		d.setNodeId(TEST_NODE_ID);
		d.setSourceId(TEST_SOURCE_ID);
		d.setCreated(new DateTime(2015, 4, 14, 10, 30, DateTimeZone.forID(TEST_TZ)));
		d.setWattHourReading(12345L);
		d.setWatts(350);
		return d;
	}

	@Test
	public void collectConsumption() {
		ConsumptionDatum d = newConsumptionDatumInstance();
		@SuppressWarnings("deprecation")
		ConsumptionDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getCreated(), result.getCreated());
		lastDatum = d;

		// verify created as GeneralNodeDatum
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK();
		pk.setCreated(d.getCreated());
		pk.setNodeId(TEST_NODE_ID);
		pk.setSourceId(TEST_SOURCE_ID);
		GeneralNodeDatum entity = biz.getGeneralNodeDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Watts", d.getWatts().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("watts").doubleValue(), 0.001);
		assertEquals("Wh", d.getWattHourReading().doubleValue(), entity.getSamples()
				.getInstantaneousSampleDouble("wattHours").doubleValue(), 0.001);
	}

	private HardwareControlDatum newHardwareControlDatumInstance() {
		HardwareControlDatum d = new HardwareControlDatum();
		d.setNodeId(TEST_NODE_ID);
		d.setSourceId("/power/switch/1");
		d.setCreated(new DateTime(2015, 4, 14, 10, 30, DateTimeZone.forID(TEST_TZ)));
		d.setIntegerValue(1);
		return d;
	}

	@Test
	public void collectHardwareControl() {
		HardwareControlDatum d = newHardwareControlDatumInstance();
		@SuppressWarnings("deprecation")
		HardwareControlDatum result = biz.postDatum(d);
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(d.getCreated(), result.getCreated());
		lastDatum = d;

		// verify created as GeneralNodeDatum
		GeneralNodeDatumPK pk = new GeneralNodeDatumPK();
		pk.setCreated(d.getCreated());
		pk.setNodeId(TEST_NODE_ID);
		pk.setSourceId(d.getSourceId());
		GeneralNodeDatum entity = biz.getGeneralNodeDatumDao().get(pk);
		assertNotNull(entity);
		assertEquals("Value", d.getIntegerValue().intValue(), entity.getSamples()
				.getStatusSampleInteger("val").intValue());
	}

	@Test
	public void findPriceLocation() {
		SourceLocationFilter filter = new SourceLocationFilter(TEST_PRICE_SOURCE_NAME, TEST_LOC_NAME);
		List<SourceLocationMatch> results = biz.findPriceLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		SourceLocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_PRICE_SOURCE_ID, loc.getId());
		assertEquals(TEST_LOC_ID, loc.getLocationId());
		assertEquals(TEST_LOC_NAME, loc.getLocationName());
		assertEquals(TEST_PRICE_SOURCE_NAME, loc.getSourceName());
	}

	@Test
	public void findWeatherLocation() {
		SourceLocationFilter filter = new SourceLocationFilter(TEST_WEATHER_SOURCE_NAME, null);
		filter.getLocation().setRegion(TEST_LOC_REGION);
		List<SourceLocationMatch> results = biz.findWeatherLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		SourceLocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_WEATHER_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_ID, loc.getLocationId());
		assertEquals(TEST_LOC_REGION + ", " + TEST_LOC_COUNTRY, loc.getLocationName());
		assertEquals(TEST_WEATHER_SOURCE_NAME, loc.getSourceName());
	}

	@Test
	public void findLocation() {
		SolarLocation filter = new SolarLocation();
		filter.setCountry(TEST_LOC_COUNTRY);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);
		List<LocationMatch> results = biz.findLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		LocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_COUNTRY, loc.getCountry());
		assertEquals(TEST_LOC_POSTAL_CODE, loc.getPostalCode());
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		datumMetadataBiz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.replay(datumMetadataBiz);

		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.verify(datumMetadataBiz);
	}

}
