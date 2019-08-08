/* ==================================================================
 * MyBatisSolarNodeDao.java - Nov 10, 2014 1:57:58 PM
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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarNodeMetadataDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeDao;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeMetadataDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.SolarNodeFilterMatch;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.support.SimpleSortDescriptor;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisSolarNodeDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisSolarNodeDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisSolarNodeDao dao;
	private SolarNodeMetadataDao metadataDao;

	@Before
	public void setup() {
		setupTestLocation();
		dao = new MyBatisSolarNodeDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());

		MyBatisSolarNodeMetadataDao metaDao = new MyBatisSolarNodeMetadataDao();
		metaDao.setSqlSessionFactory(getSqlSessionFactory());
		this.metadataDao = metaDao;
	}

	@Test
	public void getSolarNodeById() throws Exception {
		jdbcTemplate.update("insert into solarnet.sn_node (node_id, loc_id) values (?,?)", TEST_NODE_ID,
				TEST_LOC_ID);

		SolarNode node = dao.get(TEST_NODE_ID);
		assertNotNull(node);
		assertNotNull(node.getId());
		assertEquals(TEST_NODE_ID, node.getId());
		assertEquals(TEST_LOC_ID, node.getLocationId());
		assertNotNull(node.getTimeZone());
		assertEquals(TEST_TZ, node.getTimeZone().getID());
	}

	@Test
	public void getNonExistingSolarNodeById() throws Exception {
		SolarNode node = dao.get(-99L);
		assertNull(node);
	}

	@Test
	public void insertSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);

		Long id = dao.store(node);
		assertNotNull(id);
	}

	@Test
	public void updateSolarNode() throws Exception {
		SolarNode node = new SolarNode();
		node.setLocationId(TEST_LOC_ID);

		Long id = dao.store(node);
		assertNotNull(id);
		node = dao.get(id);
		assertEquals(id, node.getId());
		node.setName("myname");
		Long id2 = dao.store(node);
		assertEquals(id, id2);
		node = dao.get(id);
		assertEquals("myname", node.getName());
	}

	@Test
	public void findFilteredNodeIds() throws Exception {
		final int nodeCount = 5;
		for ( int i = 0; i < nodeCount; i++ ) {
			setupTestNode(TEST_NODE_ID - i);
		}

		FilterSupport filter = new FilterSupport();
		filter.setNodeIds(new Long[] { TEST_NODE_ID, TEST_NODE_ID - 1, TEST_NODE_ID - 2 });

		FilterResults<SolarNodeFilterMatch> results = dao.findFiltered(filter, null, null, -1);
		assertThat("Result count", results.getReturnedResultCount(), equalTo(3));
		Iterator<SolarNodeFilterMatch> itr = results.iterator();
		for ( int i = 0; i < 3; i++ ) {
			SolarNodeFilterMatch m = itr.next();
			assertThat("Match " + i + " ID", m.getId(), equalTo(TEST_NODE_ID - 2 + i));
		}
	}

	@Test
	public void findFilteredMetadataFilter() {
		List<Long> nodeIds = new ArrayList<>();
		for ( int i = 100; i < 103; i++ ) {
			setupTestNode((long) i);

			SolarNodeMetadata datum = new SolarNodeMetadata();
			datum.setCreated(new DateTime());
			datum.setNodeId((long) i);
			nodeIds.add(datum.getNodeId());

			GeneralDatumMetadata samples = new GeneralDatumMetadata();
			datum.setMeta(samples);

			Map<String, Object> msgs = new HashMap<String, Object>(2);
			msgs.put("foo", i);
			samples.setInfo(msgs);

			metadataDao.store(datum);

		}

		FilterSupport criteria = new FilterSupport();
		criteria.setNodeIds(nodeIds.toArray(new Long[nodeIds.size()]));
		criteria.setMetadataFilter("(&(/m/foo>100)(/m/foo<102))");

		List<SortDescriptor> sorts = asList(new SimpleSortDescriptor("node", false));
		FilterResults<SolarNodeFilterMatch> results = dao.findFiltered(criteria, sorts, null, null);
		assertThat("Result available", results, notNullValue());
		assertThat("Result count", results.getReturnedResultCount(), equalTo(1));
		assertThat("Result node IDs",
				stream(results.getResults().spliterator(), false).map(m -> m.getId()).collect(toList()),
				hasItems(101L));
	}

}
