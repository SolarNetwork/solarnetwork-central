/* ==================================================================
 * IbatisGeneralNodeDatumDaoTest.java - Aug 22, 2014 10:17:00 AM
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

package net.solarnetwork.central.datum.dao.ibatis.test;

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
import net.solarnetwork.central.datum.dao.ibatis.IbatisGeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumSamples;
import net.solarnetwork.central.domain.FilterResults;
import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test cases for the {@link IbatisGeneralNodeDatumDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class IbatisGeneralNodeDatumDaoTest extends AbstractIbatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_2ND_SOURCE = "2nd source";

	@Autowired
	private IbatisGeneralNodeDatumDao dao;

	private GeneralNodeDatum lastDatum;

	private GeneralNodeDatum getTestInstance() {
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

		Map<String, String> msgs = new HashMap<String, String>(2);
		msgs.put("foo", "bar");
		samples.setStatus(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralNodeDatum datum = getTestInstance();
		GeneralNodeDatumPK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(GeneralNodeDatum src, GeneralNodeDatum entity) {
		assertNotNull("GeneralNodeDatum should exist", entity);
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getPosted(), entity.getPosted());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getSamples(), entity.getSamples());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		GeneralNodeDatum datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		GeneralNodeDatum datum = getTestInstance();
		datum.getSamples().getAccumulating().put("watt_hours", 39309570293789380L);
		datum.getSamples().getAccumulating()
				.put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getSamples().getInstantaneous().put("watts", 498475890235787897L);
		datum.getSamples().getInstantaneous()
				.put("floating", new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		GeneralNodeDatum entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void getFilteredDefaultSort() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<GeneralNodeDatumMatch> results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());

		GeneralNodeDatum datum2 = new GeneralNodeDatum();
		datum2.setCreated(new DateTime().plusHours(1));
		datum2.setNodeId(TEST_NODE_ID);
		datum2.setSourceId(TEST_SOURCE_ID);
		datum2.setSampleJson("{\"i\":{\"watts\":123}}");
		dao.store(datum2);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(2L, (long) results.getTotalResults());
		assertEquals(2, (int) results.getReturnedResultCount());

		GeneralNodeDatum datum3 = new GeneralNodeDatum();
		datum3.setCreated(lastDatum.getCreated());
		datum3.setNodeId(TEST_NODE_ID);
		datum3.setSourceId("/test/source/2");
		datum3.setSampleJson("{\"a\":{\"watt_hours\":789}}");
		dao.store(datum3);

		results = dao.findFiltered(criteria, null, null, null);
		assertNotNull(results);
		assertEquals(3L, (long) results.getTotalResults());
		assertEquals(3, (int) results.getReturnedResultCount());
		List<GeneralNodeDatumPK> ids = new ArrayList<GeneralNodeDatumPK>();
		for ( GeneralNodeDatum d : results ) {
			ids.add(d.getId());
		}
		// expect d3, d1, d2 because sorted by nodeId,created,sourceId
		assertEquals("Result order", Arrays.asList(datum3.getId(), lastDatum.getId(), datum2.getId()),
				ids);
	}

	@Test
	public void getAllAvailableSourcesForNode() {
		storeNew();
		Set<String> sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 0, sources.size());

		// we are querying the reporting table, which requires two rows minimum	so add 2nd datum
		// of same source to trigger data population there
		GeneralNodeDatum d2 = getTestInstance();
		d2.setCreated(d2.getCreated().plus(1000));
		dao.store(d2);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 1, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));

		// add a 2nd source (two more datum to get into reporting table).
		// we also make this on another day, to support getAllAvailableSourcesForNodeAndDateRange() test
		GeneralNodeDatum d3 = getTestInstance();
		d3.setSourceId(TEST_2ND_SOURCE);
		d3.setCreated(d2.getCreated().plusDays(1));
		dao.store(d3);

		GeneralNodeDatum d4 = getTestInstance();
		d4.setSourceId(d3.getSourceId());
		d4.setCreated(d3.getCreated().plus(1000));
		dao.store(d4);

		// immediately process reporting data
		processAggregateStaleData();

		sources = dao.getAvailableSources(lastDatum.getNodeId(), null, null);
		assertEquals("Sources set size", 2, sources.size());
		assertTrue("Source ID returned", sources.contains(d2.getSourceId()));
		assertTrue("Source ID returned", sources.contains(d3.getSourceId()));
	}

}
