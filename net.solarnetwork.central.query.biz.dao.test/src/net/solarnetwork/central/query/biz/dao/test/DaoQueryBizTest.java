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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.query.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Set;
import javax.annotation.Resource;
import net.solarnetwork.central.datum.dao.ConsumptionDatumDao;
import net.solarnetwork.central.datum.dao.PowerDatumDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Unit test for the {@link DaoQueryBiz} class.
 * 
 * @author matt
 * @version $Revision$
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

}
