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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.query.biz.dao.DaoQueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import net.solarnetwork.domain.GeneralNodeDatumSamples;

/**
 * Unit test for the {@link DaoQueryBiz} class.
 * 
 * @author matt
 * @version 2.1
 */
public class DaoQueryBizTest extends AbstractQueryBizDaoTestSupport {

	private final String TEST_SOURCE_ID = "test.source";
	private final String TEST_SOURCE_ID2 = "test.source.2";
	private final Long TEST_NODE_ID2 = -2L;

	@Resource
	private DaoQueryBiz daoQueryBiz;

	@Autowired
	private GeneralNodeDatumDao generalNodeDatumDao;

	@Autowired
	private GeneralLocationDatumDao generalLocationDatumDao;

	@Before
	public void setup() {
		setupTestNode();
		setupTestPriceLocation();
	}

	private GeneralNodeDatum getTestGeneralNodeDatumInstance() {
		return createGeneralNodeDatum(TEST_NODE_ID, TEST_SOURCE_ID);
	}

	private GeneralNodeDatum createGeneralNodeDatum(Long nodeId, String sourceId) {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(new DateTime());
		datum.setNodeId(nodeId);
		datum.setPosted(new DateTime());
		datum.setSourceId(sourceId);

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

	private GeneralLocationDatum createGeneralLocationDatum(Long locationId, String sourceId) {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime());
		datum.setLocationId(locationId);
		datum.setPosted(new DateTime());
		datum.setSourceId(sourceId);

		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
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
	public void getAvailableSourcesGeneralNodeDatumMultipleNodes() {
		GeneralNodeDatum d = getTestGeneralNodeDatumInstance();
		generalNodeDatumDao.store(d);

		GeneralNodeDatum d2 = getTestGeneralNodeDatumInstance();
		d2.setCreated(d2.getCreated().plusHours(1));
		generalNodeDatumDao.store(d2);

		GeneralNodeDatum d3 = getTestGeneralNodeDatumInstance();
		d3.setNodeId(TEST_NODE_ID2);
		d3.setSourceId(TEST_SOURCE_ID2);
		generalNodeDatumDao.store(d3);

		GeneralNodeDatum d4 = getTestGeneralNodeDatumInstance();
		d4.setCreated(d3.getCreated().plusHours(1));
		d4.setNodeId(TEST_NODE_ID2);
		d4.setSourceId(TEST_SOURCE_ID2);
		generalNodeDatumDao.store(d4);

		// immediately process reporting data, which the DAO relies on
		processAggregateStaleData();

		DatumFilterCommand f = new DatumFilterCommand();
		f.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID2 });
		Set<String> result = daoQueryBiz.getAvailableSources(f);
		assertThat("Source IDs set", result, contains(TEST_SOURCE_ID, TEST_SOURCE_ID2));
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

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated(),
				d.getCreated().plusDays(1));
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("1st result inclusive start date", result.contains(TEST_SOURCE_ID));

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(1),
				d.getCreated().plusDays(2));
		assertNotNull(result);
		assertEquals("No results within date range", 0, result.size());

		result = daoQueryBiz.getAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(4),
				d2.getCreated());
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("2nd result inclusive end date", result.contains(TEST_SOURCE_ID2));
	}

	@Test
	public void testIterateGeneralNodeDatum() {
		List<GeneralNodeDatumPK> ids = new ArrayList<GeneralNodeDatumPK>(10);
		// make sure created dates are different and ascending
		final int numDatum = 10;
		final long created = System.currentTimeMillis() - (1000 * numDatum);
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralNodeDatum d = createGeneralNodeDatum(TEST_NODE_ID, TEST_SOURCE_ID);
			d.setCreated(new DateTime(created + (i * 1000)));
			generalNodeDatumDao.store(d);
			ids.add(d.getId());
		}
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(TEST_NODE_ID);
		List<SortDescriptor> sorts = Collections
				.singletonList((SortDescriptor) new SimpleSortDescriptor("created", true));
		int offset = 0;
		final int maxResults = 2;
		while ( offset < 8 ) {
			FilterResults<GeneralNodeDatumFilterMatch> matches = daoQueryBiz
					.findFilteredGeneralNodeDatum(filter, sorts, offset, maxResults);
			assertNotNull(matches);
			assertEquals(Integer.valueOf(2), matches.getReturnedResultCount());
			Iterator<GeneralNodeDatumFilterMatch> itr = matches.getResults().iterator();
			for ( int i = 0; i < maxResults; i++, offset++ ) {
				GeneralNodeDatumFilterMatch match = itr.next();
				assertEquals(ReportingGeneralNodeDatum.class, match.getClass());
				assertEquals(created + ((numDatum - offset - 1) * 1000),
						((ReportingGeneralNodeDatum) match).getCreated().getMillis());
			}
		}

	}

	private GeneralLocationDatum getTestGeneralLocationDatumInstance() {
		return createGeneralLocationDatum(TEST_LOC_ID, TEST_SOURCE_ID);
	}

	@Test
	public void getReportableIntervalGeneralLocationNoData() {
		ReportableInterval result = daoQueryBiz.getReportableInterval(TEST_NODE_ID, (String) null);
		assertNull(result);
	}

	@Test
	public void getReportableIntervalSingleGeneralLocationDatum() {
		GeneralLocationDatum d = getTestGeneralLocationDatumInstance();
		generalLocationDatumDao.store(d);

		ReportableInterval result = daoQueryBiz.getLocationReportableInterval(TEST_NODE_ID,
				(String) null);
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getReportableIntervalRangeGeneralLocationDatum() {
		GeneralLocationDatum d = getTestGeneralLocationDatumInstance();
		generalLocationDatumDao.store(d);

		GeneralLocationDatum d2 = getTestGeneralLocationDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		generalLocationDatumDao.store(d2);

		ReportableInterval result = daoQueryBiz.getLocationReportableInterval(TEST_NODE_ID,
				(String) null);
		assertNotNull(result);
		assertEquals(d.getCreated(), result.getInterval().getStart());
		assertEquals(d2.getCreated(), result.getInterval().getEnd());
	}

	@Test
	public void getAvailableSourcesGeneralLocationDatum() {
		GeneralLocationDatum d = getTestGeneralLocationDatumInstance();
		generalLocationDatumDao.store(d);

		GeneralLocationDatum d2 = getTestGeneralLocationDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		d2.setSourceId(TEST_SOURCE_ID2);
		generalLocationDatumDao.store(d2);

		// immediately process reporting data, which the DAO relies on
		processAggregateStaleData();

		Set<String> result = daoQueryBiz.getLocationAvailableSources(TEST_NODE_ID, null, null);
		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.contains(TEST_SOURCE_ID));
		assertTrue(result.contains(TEST_SOURCE_ID2));
	}

	@Test
	public void getAvailableSourcesGeneralLocationDatumWithDateRange() {
		GeneralLocationDatum d = getTestGeneralLocationDatumInstance();
		generalLocationDatumDao.store(d);

		GeneralLocationDatum d2 = getTestGeneralLocationDatumInstance();
		d2.setCreated(d2.getCreated().plusDays(5));
		d2.setSourceId(TEST_SOURCE_ID2);
		generalLocationDatumDao.store(d2);

		// immediately process reporting data, which the DAO relies on
		processAggregateStaleData();

		Set<String> result;

		result = daoQueryBiz.getLocationAvailableSources(TEST_NODE_ID, d.getCreated(),
				d.getCreated().plusDays(1));
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("1st result inclusive start date", result.contains(TEST_SOURCE_ID));

		result = daoQueryBiz.getLocationAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(1),
				d.getCreated().plusDays(2));
		assertNotNull(result);
		assertEquals("No results within date range", 0, result.size());

		result = daoQueryBiz.getLocationAvailableSources(TEST_NODE_ID, d.getCreated().plusDays(4),
				d2.getCreated());
		assertNotNull(result);
		assertEquals(1, result.size());
		assertTrue("2nd result inclusive end date", result.contains(TEST_SOURCE_ID2));
	}

	@Test
	public void testIterateGeneralLocationDatum() {
		List<GeneralLocationDatumPK> ids = new ArrayList<GeneralLocationDatumPK>(10);
		// make sure created dates are different and ascending
		final int numDatum = 10;
		final long created = System.currentTimeMillis() - (1000 * numDatum);
		for ( int i = 0; i < numDatum; i++ ) {
			GeneralLocationDatum d = createGeneralLocationDatum(TEST_NODE_ID, TEST_SOURCE_ID);
			d.setCreated(new DateTime(created + (i * 1000)));
			generalLocationDatumDao.store(d);
			ids.add(d.getId());
		}
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(TEST_NODE_ID);
		List<SortDescriptor> sorts = Collections
				.singletonList((SortDescriptor) new SimpleSortDescriptor("created", true));
		int offset = 0;
		final int maxResults = 2;
		while ( offset < 8 ) {
			FilterResults<GeneralLocationDatumFilterMatch> matches = daoQueryBiz
					.findGeneralLocationDatum(filter, sorts, offset, maxResults);
			assertNotNull(matches);
			assertEquals(Integer.valueOf(2), matches.getReturnedResultCount());
			Iterator<GeneralLocationDatumFilterMatch> itr = matches.getResults().iterator();
			for ( int i = 0; i < maxResults; i++, offset++ ) {
				GeneralLocationDatumFilterMatch match = itr.next();
				assertEquals(ReportingGeneralLocationDatum.class, match.getClass());
				assertEquals(created + ((numDatum - offset - 1) * 1000),
						((ReportingGeneralLocationDatum) match).getCreated().getMillis());
			}
		}
	}

	@Test
	public void findNodesForDataToken() {
		User user = createNewUser(TEST_USER_EMAIL);
		for ( int i = 100; i < 105; i++ ) {
			setupTestNode(TEST_NODE_ID - i);
			storeNewUserNode(user, getNode(TEST_NODE_ID - i));
		}
		SecurityToken actor = becomeAuthenticatedReadNodeDataToken(user.getId(), null);
		Set<Long> results = daoQueryBiz.findAvailableNodes(actor);

		assertThat("Results", results, hasItems(TEST_NODE_ID - 104, TEST_NODE_ID - 103,
				TEST_NODE_ID - 102, TEST_NODE_ID - 101, TEST_NODE_ID - 100));
	}

	@Test
	public void findNodesForDataTokenWithPolicy() {
		User user = createNewUser(TEST_USER_EMAIL);
		for ( int i = 100; i < 105; i++ ) {
			setupTestNode(TEST_NODE_ID - i);
			storeNewUserNode(user, getNode(TEST_NODE_ID - i));
		}

		SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<Long>(Arrays.asList(TEST_NODE_ID - 100, TEST_NODE_ID - 104)))
				.build();
		SecurityToken actor = becomeAuthenticatedReadNodeDataToken(user.getId(), policy);
		Set<Long> results = daoQueryBiz.findAvailableNodes(actor);
		assertThat("Results", results, hasItems(TEST_NODE_ID - 104, TEST_NODE_ID - 100));
	}

}
