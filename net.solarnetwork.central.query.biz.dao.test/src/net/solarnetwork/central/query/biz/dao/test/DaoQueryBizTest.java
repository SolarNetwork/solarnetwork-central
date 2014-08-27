/* ==================================================================
 * DaoQueryBizTest.java - Jul 12, 2012 6:22:33 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.dao.HardwareControlDatumDao;
import net.solarnetwork.central.datum.dao.PowerDatumDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.HardwareControlDatum;
import net.solarnetwork.central.datum.domain.HardwareControlDatumMatch;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Unit test for the {@link DaoQueryBiz} class.
 * 
 * @author matt
 * @version 1.1
 */
@ContextConfiguration
public class DaoQueryBizTest extends AbstractCentralTransactionalTest {

	private final String TEST_SOURCE_ID = "test.source";
	private final String TEST_SOURCE_ID2 = "test.source.2";

	@Resource
	private DaoQueryBiz daoQueryBiz;

	@Autowired
	private PowerDatumDao powerDatumDao;

	@Autowired
	private ConsumptionDatumDao consumptionDatumDao;

	@Autowired
	private HardwareControlDatumDao hardwareControlDatumDao;

	@Autowired
	private GeneralNodeDatumDao generalNodeDatumDao;

	@Before
	public void setup() {
		setupTestNode();
		setupTestPriceLocation();
	}

	private PowerDatum getTestPowerDatumInstance() {
		PowerDatum datum = new PowerDatum();
		datum.setBatteryAmpHours(1.3F);
		datum.setBatteryVolts(1.4F);
		datum.setCreated(new DateTime());
		datum.setKWattHoursToday(1.7F);
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);
		datum.setPosted(new DateTime());

		// note we are setting legacy amp/volt properties here
		datum.setPvAmps(1.8F);
		datum.setPvVolts(1.9F);

		datum.setWattHourReading(2L);
		return datum;
	}

	private ConsumptionDatum getTestConsumptionDatumInstance() {
		ConsumptionDatum datum = new ConsumptionDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(new DateTime());
		datum.setSourceId(TEST_SOURCE_ID);

		// note we are setting legacy amps/volts values here, not watts
		datum.setAmps(1.0F);
		datum.setVolts(1.0F);

		datum.setWattHourReading(2L);
		return datum;
	}

	private GeneralNodeDatum getTestGeneralNodeDatumInstance() {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(new DateTime());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralNodeDatumSamples samples = new GeneralNodeDatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("watt_hours", 4123);
		samples.setAccumulating(accum);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setStatus(msgs);
		return datum;
	}

	@Test
	public void getReportableIntervalNoData() {
		@SuppressWarnings("unchecked")
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID,
				new Class[] { PowerDatum.class });
		assertNull(result);
	}

	@Test
	public void getReportableIntervalSingleDatum() {
		PowerDatum d = getTestPowerDatumInstance();
		powerDatumDao.store(d);

		@SuppressWarnings("unchecked")
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID,
				new Class[] { PowerDatum.class });
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getReportableIntervalRangeDatum() {
		PowerDatum d = getTestPowerDatumInstance();
		powerDatumDao.store(d);

		PowerDatum d2 = getTestPowerDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		powerDatumDao.store(d2);

		@SuppressWarnings("unchecked")
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID,
				new Class[] { PowerDatum.class });
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d2.getCreated(), result.getInterval().getEnd());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getReportableIntervalRangeMultipleDatumTypes() {
		PowerDatum d = getTestPowerDatumInstance();
		powerDatumDao.store(d);

		ConsumptionDatum c = getTestConsumptionDatumInstance();
		c.setCreated(c.getCreated().plusDays(3));
		consumptionDatumDao.store(c);

		// searches on single datum types should result in only the range for that type
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID,
				new Class[] { PowerDatum.class });
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d.getCreated(), result.getInterval().getEnd());

		result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, new Class[] { ConsumptionDatum.class });
		assertNotNull(result);
		assertEquals(c.getCreated(), result.getInterval().getStart());
		assertEquals(c.getCreated(), result.getInterval().getEnd());

		// now search for multiple types, and the range should span both
		result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, new Class[] { ConsumptionDatum.class,
				PowerDatum.class });
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(c.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getAvailableSources() {
		// because we're relying on DAO implementation, source search only works with a minimum of 2 datum
		PowerDatum d = getTestPowerDatumInstance();
		d.setSourceId(TEST_SOURCE_ID);
		powerDatumDao.store(d);
		PowerDatum d2 = getTestPowerDatumInstance();
		d2.setCreated(d.getCreated().plusMinutes(1));
		d2.setSourceId(TEST_SOURCE_ID);
		powerDatumDao.store(d2);

		ConsumptionDatum c = getTestConsumptionDatumInstance();
		c.setCreated(c.getCreated().plusDays(3));
		c.setSourceId(TEST_SOURCE_ID2);
		consumptionDatumDao.store(c);
		ConsumptionDatum c2 = getTestConsumptionDatumInstance();
		c2.setCreated(c.getCreated().plusMinutes(1));
		c2.setSourceId(TEST_SOURCE_ID2);
		consumptionDatumDao.store(c2);

		// immediately process reporting data, which the DAO relies on
		processReportingStaleData();

		Set<String> result;

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, PowerDatum.class, null, null);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.contains(TEST_SOURCE_ID));

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, ConsumptionDatum.class, null, null);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue(result.contains(TEST_SOURCE_ID2));
	}

	private HardwareControlDatum createHardwareControlDatum(Long nodeId, String sourceId,
			Integer integerValue, Float floatValue) {
		HardwareControlDatum datum = new HardwareControlDatum();
		datum.setCreated(new DateTime());
		datum.setIntegerValue(integerValue);
		datum.setFloatValue(floatValue);
		datum.setNodeId(nodeId);
		datum.setSourceId(sourceId);
		return datum;
	}

	@Test
	public void testIterateHardwareControlDatum() {
		List<Long> ids = new ArrayList<Long>(10);
		// make sure created dates are different and ascending
		final int numDatum = 10;
		final long created = System.currentTimeMillis() - (1000 * numDatum);
		for ( int i = 0; i < numDatum; i++ ) {
			HardwareControlDatum d = createHardwareControlDatum(TEST_NODE_ID, TEST_SOURCE_ID,
					(int) Math.round(Math.random() * 100), (float) (Math.random() * 10));
			d.setCreated(new DateTime(created + (i * 1000)));
			ids.add(hardwareControlDatumDao.store(d));
		}
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		List<SortDescriptor> sorts = Collections
				.singletonList((SortDescriptor) new SimpleSortDescriptor("created", true));
		int offset = 0;
		final int maxResults = 2;
		while ( offset < 8 ) {
			FilterResults<? extends EntityMatch> matches = daoQueryBiz.findFilteredDatum(
					HardwareControlDatum.class, filter, sorts, offset, maxResults);
			assertNotNull(matches);
			assertEquals(Integer.valueOf(2), matches.getReturnedResultCount());
			Iterator<? extends EntityMatch> itr = matches.getResults().iterator();
			for ( int i = 0; i < maxResults; i++, offset++ ) {
				EntityMatch match = itr.next();
				assertEquals(HardwareControlDatumMatch.class, match.getClass());
				assertEquals(created + ((numDatum - offset - 1) * 1000),
						((HardwareControlDatumMatch) match).getCreated().getMillis());
			}
		}

	}

	@Test
	public void getReportableIntervalGeneralNodeNoData() {
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, (String) null);
		assertNull(result);
	}

	@Test
	public void getReportableIntervalSingleGeneralNodeDatum() {
		GeneralNodeDatum d = getTestGeneralNodeDatumInstance();
		generalNodeDatumDao.store(d);

		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, (String) null);
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getReportableIntervalRangeGeneralNodeDatum() {
		GeneralNodeDatum d = getTestGeneralNodeDatumInstance();
		generalNodeDatumDao.store(d);

		GeneralNodeDatum d2 = getTestGeneralNodeDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		generalNodeDatumDao.store(d2);

		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, (String) null);
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d2.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getAvailableSourcesGeneralNodeDatum() {
		GeneralNodeDatum d = getTestGeneralNodeDatumInstance();
		generalNodeDatumDao.store(d);

		GeneralNodeDatum d2 = getTestGeneralNodeDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		d2.setSourceId(TEST_SOURCE_ID2);
		generalNodeDatumDao.store(d2);

		// immediately process reporting data, which the DAO relies on
		processAggregateStaleData();

		Set<String> result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, null, null);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.contains(TEST_SOURCE_ID));
		assertTrue(result.contains(TEST_SOURCE_ID2));
	}

	@Test
	public void getAvailableSourcesGeneralNodeDatumWithDateRange() {
		GeneralNodeDatum d = getTestGeneralNodeDatumInstance();
		generalNodeDatumDao.store(d);

		GeneralNodeDatum d2 = getTestGeneralNodeDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		d2.setSourceId(TEST_SOURCE_ID2);
		generalNodeDatumDao.store(d2);

		// immediately process reporting data, which the DAO relies on
		processAggregateStaleData();

		Set<String> result;

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated(), d.getCreated()
				.plusDays(1));
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("1st result inclusive start date", result.contains(TEST_SOURCE_ID));

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(1), d
				.getCreated().plusDays(2));
		assertNotNull(result);
		assertEquals("No results within date range", 0, result.size());

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(4),
				d2.getCreated());
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("2nd result inclusive end date", result.contains(TEST_SOURCE_ID2));
	}

}
