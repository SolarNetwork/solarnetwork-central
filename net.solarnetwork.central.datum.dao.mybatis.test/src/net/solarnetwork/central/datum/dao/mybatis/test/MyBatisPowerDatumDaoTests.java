/* ==================================================================
 * MyBatisPowerDatumDaoTests.java - Nov 14, 2014 6:55:10 AM
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

package net.solarnetwork.central.datum.dao.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisPowerDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.datum.domain.PowerDatumMatch;
import net.solarnetwork.central.domain.FilterResults;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link MyBatisPowerDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisPowerDatumDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_2ND_SOURCE = "2nd source";

	private MyBatisPowerDatumDao dao;

	private PowerDatum lastDatum;

	@Before
	public void setUp() {
		dao = new MyBatisPowerDatumDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		lastDatum = null;
	}

	private PowerDatum getTestInstance() {
		PowerDatum datum = new PowerDatum();
		datum.setBatteryAmpHours(1.3F);
		datum.setBatteryVolts(1.4F);
		datum.setCreated(new DateTime());
		datum.setKWattHoursToday(1.7F);
		datum.setLocationId(TEST_PRICE_LOC_ID);
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId("test.source");
		datum.setPosted(new DateTime());

		// note we are setting legacy amp/volt properties here
		datum.setPvAmps(1.8F);
		datum.setPvVolts(1.9F);

		datum.setWattHourReading(2L);
		return datum;
	}

	@Test
	public void storeNew() {
		PowerDatum datum = getTestInstance();

		Long id = dao.store(datum);
		assertNotNull(id);
		datum.setId(id);
		lastDatum = datum;
	}

	private void validate(PowerDatum src, PowerDatum entity) {
		assertNotNull("PowerDatum should exist", entity);
		assertNotNull("Created date should be set", entity.getCreated());
		assertEquals(src.getBatteryAmpHours(), entity.getBatteryAmpHours());
		assertEquals(src.getBatteryVolts(), entity.getBatteryVolts());
		assertEquals(src.getWattHourReading(), entity.getWattHourReading());
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getPosted(), entity.getPosted());
		assertEquals(src.getWatts(), entity.getWatts());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		PowerDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void getMostRecent() {
		storeNew();

		DatumQueryCommand criteria = new DatumQueryCommand();
		criteria.setNodeId(TEST_NODE_ID);

		List<PowerDatum> results = dao.getMostRecentDatum(criteria);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(lastDatum, results.get(0));

		PowerDatum datum2 = new PowerDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(lastDatum.getSourceId());
		datum2.setPvAmps(1.2F);
		Long id2 = dao.store(datum2);

		results = dao.getMostRecentDatum(criteria);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(id2, results.get(0).getId());

		PowerDatum datum3 = new PowerDatum();
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setPvAmps(1.3F);
		Long id3 = dao.store(datum3);

		results = dao.getMostRecentDatum(criteria);
		assertNotNull(results);
		assertEquals(2, results.size());
		Set<Long> ids = new LinkedHashSet<Long>();
		for ( PowerDatum d : results ) {
			ids.add(d.getId());
		}
		assertTrue(ids.contains(id2));
		assertTrue(ids.contains(id3));
	}

	@Test
	public void getAllAvailableSourcesNoneAvailable() {
		Set<String> sources = dao.getAvailableSources(1L, null, null);
		assertNotNull(sources);
	}

	@Test
	public void getAllAvailableSourcesForNode() {
		storeNew();
		Set<String> sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		PowerDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		d2.setWattHourReading(d2.getWattHourReading() + 1L);
		d2 = dao.get(dao.store(d2));

		processReportingStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));

		// add a 2nd source (two more datum to get into reporting table).
		// we also make this on another day, to support getAllAvailableSourcesForNodeAndDateRange() test
		PowerDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(1));
		d3.setWattHourReading(d2.getWattHourReading() + 1L);
		d3 = dao.get(dao.store(d3));

		PowerDatum d4 = getTestInstance();
		d4.setSourceId(d3.getSourceId());
		d4.setCreated(d3.getCreated().plus(1000));
		d4.setWattHourReading(d3.getWattHourReading() + 1L);
		d4 = dao.get(dao.store(d4));

		processReportingStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));
		assertTrue("Source ID returned", sources.contains(d3.getSourceId()));
	}

	@Test
	public void getAllAvailableSourcesForNodeAndDateRange() {
		getAllAvailableSourcesForNode();
		final LocalDate start = lastDatum.getCreated().toLocalDate();
		final LocalDate end = start.plusDays(1);

		// search for range BEFORE data
		Set<String> sources = dao.getAvailableSources(lastDatum.getNodeId(), start.minusDays(1),
				start.minusDays(1));
		assertEquals("Sources set size", 0, sources.size());

		// search for range AFTER data
		sources = dao.getAvailableSources(lastDatum.getNodeId(), start.plusDays(2), start.plusDays(2));
		assertEquals("Sources set size", 0, sources.size());

		// search for range UP TO first data
		sources = dao.getAvailableSources(lastDatum.getNodeId(), start, start);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(lastDatum.getSourceId()));

		// search for range STARTING FROM last data
		sources = dao.getAvailableSources(lastDatum.getNodeId(), end, end);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(TEST_2ND_SOURCE));

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(lastDatum.getSourceId()));
		assertTrue("Source ID returned", sources.contains(TEST_2ND_SOURCE));
	}

	@Test
	public void getFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<PowerDatumMatch> results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		PowerDatum datum2 = new PowerDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(lastDatum.getSourceId());
		datum2.setPvAmps(1.2F);
		Long id2 = dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		PowerDatum datum3 = new PowerDatum();
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setPvAmps(1.3F);
		Long id3 = dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<Long> ids = new ArrayList<Long>();
		for ( PowerDatum d : results ) {
			ids.add(d.getId());
		}
		assertEquals("Result order", Arrays.asList(lastDatum.getId(), id2, id3), ids);
	}

}
