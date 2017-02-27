/* ==================================================================
 * MyBatisGeneralNodeDatumMetadataDaoTests.java - Nov 14, 2014 6:45:13 AM
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralNodeDatumMetadataDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisGeneralNodeDatumMetadataDao} class.
 * 
 * @author matt
 * @version 1.1
 */
public class MyBatisGeneralNodeDatumMetadataDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID_2 = "test.source.2";

	private MyBatisGeneralNodeDatumMetadataDao dao;

	private GeneralNodeDatumMetadata lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisGeneralNodeDatumMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralNodeDatumMetadata getTestInstance() {
		GeneralNodeDatumMetadata datum = new GeneralNodeDatumMetadata();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralNodeDatumMetadata datum = getTestInstance();
		NodeSourcePK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(GeneralNodeDatumMetadata src, GeneralNodeDatumMetadata entity) {
		assertNotNull("GeneralNodeDatum should exist", entity);
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getCreated(), entity.getCreated());
		assertEquals(src.getMeta(), entity.getMeta());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralNodeDatumMetadata datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		GeneralNodeDatumMetadata datum = getTestInstance();
		datum.getMeta().getInfo().put("watt_hours", 39309570293789380L);
		datum.getMeta().getInfo().put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getMeta().getInfo().put("watts", 498475890235787897L);
		datum.getMeta().getInfo().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralNodeDatumMetadata entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = dao.findFiltered(criteria, null,
				null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralNodeDatumMetadata datum2 = new GeneralNodeDatumMetadata();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID_2);
		datum2.setMetaJson("{\"m\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		GeneralNodeDatumMetadata datum3 = new GeneralNodeDatumMetadata();
		datum3.setCreated(lastDatum.getCreated());
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setMetaJson("{\"m\":{\"watt_hours\":789}}");
		dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<NodeSourcePK> ids = new ArrayList<NodeSourcePK>();
		for ( GeneralNodeDatumMetadataFilterMatch d : results ) {
			ids.add(d.getId());
		}
		// expect d3, d1, d2 because sorted by nodeId,created,sourceId
		assertEquals("Result order", Arrays.asList(datum3.getId(), lastDatum.getId(), datum2.getId()),
				ids);
	}

	@Test
	public void findFilteredWithMax() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = dao.findFiltered(criteria, null, 0,
				1);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralNodeDatumMetadata datum2 = new GeneralNodeDatumMetadata();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID_2);
		datum2.setMetaJson("{\"m\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, 0, 1);
		assertNotNull(results);
		assertEquals("Returned results", 2L, (long) results.getTotalResults());
		assertEquals("Returned result count", 1, (int) results.getReturnedResultCount());
		assertEquals("Datum ID", lastDatum.getId(), results.iterator().next().getId());
	}

	@Test
	public void findSourcesForMetadataFilter() {
		GeneralNodeDatumMetadata meta1 = getTestInstance();
		meta1.getMeta().addTag("super");
		dao.store(meta1);

		GeneralNodeDatumMetadata meta2 = getTestInstance();
		meta2.setSourceId(TEST_SOURCE_ID_2);
		dao.store(meta2);

		Set<NodeSourcePK> results = dao.getFilteredSources(new Long[] { TEST_NODE_ID },
				"(&(/**/foo=bar)(t=super))");
		assertNotNull(results);
		assertEquals("Returned results", 1L, results.size());
		assertEquals(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), results.iterator().next());

		results = dao.getFilteredSources(new Long[] { TEST_NODE_ID }, "(/**/foo=bar)");
		assertNotNull(results);
		assertEquals("Returned results", 2L, results.size());
		assertTrue(results.contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID)));
		assertTrue(results.contains(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID_2)));
	}

}
