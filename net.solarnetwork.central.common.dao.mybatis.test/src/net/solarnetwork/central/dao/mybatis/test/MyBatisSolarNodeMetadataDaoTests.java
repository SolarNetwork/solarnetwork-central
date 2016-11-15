/* ==================================================================
 * MyBatisSolarNodeMetadataDaoTests.java - 11/11/2016 2:05:06 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.mybatis.MyBatisSolarNodeMetadataDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadata;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisSolarNodeMetadataDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisSolarNodeMetadataDaoTests extends AbstractMyBatisDaoTestSupport {

	private MyBatisSolarNodeMetadataDao dao;

	private SolarNodeMetadata lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisSolarNodeMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		setupTestNode();
	}

	private SolarNodeMetadata getTestInstance() {
		SolarNodeMetadata datum = new SolarNodeMetadata();
		datum.setCreated(new DateTime());
		datum.setNodeId(TEST_NODE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		return datum;
	}

	@Test
	public void storeNew() {
		SolarNodeMetadata datum = getTestInstance();
		Long id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	private void validate(SolarNodeMetadata src, SolarNodeMetadata entity) {
		assertNotNull("GeneralNodeDatum should exist", entity);
		assertEquals(src.getNodeId(), entity.getNodeId());
		assertEquals(src.getCreated(), entity.getCreated());
		assertEquals(src.getMeta(), entity.getMeta());
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		SolarNodeMetadata datum = dao.get(lastDatum.getId());
		validate(lastDatum, datum);
	}

	@Test
	public void storeVeryBigValues() {
		SolarNodeMetadata datum = getTestInstance();
		datum.getMeta().getInfo().put("watt_hours", 39309570293789380L);
		datum.getMeta().getInfo().put("very_big", new BigInteger("93475092039478209375027350293523957"));
		datum.getMeta().getInfo().put("watts", 498475890235787897L);
		datum.getMeta().getInfo().put("floating",
				new BigDecimal("293487590845639845728947589237.49087"));
		dao.store(datum);

		SolarNodeMetadata entity = dao.get(datum.getId());
		validate(datum, entity);
	}

	@Test
	public void findFiltered() {
		storeNew();

		FilterSupport criteria = new FilterSupport();
		criteria.setNodeId(TEST_NODE_ID);

		FilterResults<SolarNodeMetadataFilterMatch> results = dao.findFiltered(criteria, null, null,
				null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
		SolarNodeMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Match ID", TEST_NODE_ID, match.getId());
	}

}
