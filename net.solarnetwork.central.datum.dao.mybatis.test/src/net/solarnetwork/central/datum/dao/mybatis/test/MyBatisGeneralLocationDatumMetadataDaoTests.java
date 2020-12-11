/* ==================================================================
 * MyBatisGeneralLocationDatumMetadataDaoTests.java - Nov 13, 2014 9:08:02 PM
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.mybatis.MyBatisGeneralLocationDatumMetadataDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test cases for the {@link MyBatisGeneralLocationDatumMetadataDao}.
 * 
 * @author matt
 * @version 1.2
 */
public class MyBatisGeneralLocationDatumMetadataDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";
	private static final String TEST_SOURCE_ID_2 = "test.source.2";

	private MyBatisGeneralLocationDatumMetadataDao dao;

	private GeneralLocationDatumMetadata lastDatum;

	@Before
	public void setup() {
		dao = new MyBatisGeneralLocationDatumMetadataDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private GeneralLocationDatumMetadata getTestInstance() {
		GeneralLocationDatumMetadata datum = new GeneralLocationDatumMetadata();
		datum.setCreated(new DateTime());
		datum.setLocationId(TEST_LOC_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralDatumMetadata samples = new GeneralDatumMetadata();
		datum.setMeta(samples);

		Map<String, Object> msgs = new HashMap<String, Object>(2);
		msgs.put("foo", "bar");
		samples.setInfo(msgs);

		samples.addTag("foo");

		return datum;
	}

	@Test
	public void storeNew() {
		GeneralLocationDatumMetadata datum = getTestInstance();
		LocationSourcePK id = dao.store(datum);
		assertNotNull(id);
		lastDatum = datum;
	}

	@Test
	public void storeUpdated() {
		storeNew();
		GeneralLocationDatumMetadata datum = lastDatum;
		datum.getMeta().putInfoValue("bim", "bam");
		LocationSourcePK id = dao.store(datum);
		assertEquals(datum.getId(), id);
	}

	private void validate(GeneralLocationDatumMetadata src, GeneralLocationDatumMetadata entity) {
		assertNotNull("GeneralLocationDatum should exist", entity);
		assertEquals(src.getLocationId(), entity.getLocationId());
		assertEquals(src.getSourceId(), entity.getSourceId());
		assertEquals(src.getCreated(), entity.getCreated());
		assertEquals(src.getMeta(), entity.getMeta());
	}

	@Test
	public void findFilteredWithLocationSearch() {
		storeNew();

		SolarLocation loc = new SolarLocation();
		loc.setRegion("NZ");
		DatumFilterCommand criteria = new DatumFilterCommand(loc);

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria, null,
				null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
	}

	@Test
	public void findFilteredWithTagSearch() {
		storeNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setTags(new String[] { "foo", "bar" });

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria, null,
				null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
	}

	@Test
	public void findFilteredWithTagAndRegionQuery() {
		storeNew();

		SolarLocation loc = new SolarLocation();
		loc.setRegion("nz");
		DatumFilterCommand criteria = new DatumFilterCommand(loc);
		criteria.setTags(new String[] { "foo" });

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dao.findFiltered(criteria, null,
				null, null);
		assertNotNull(results);
		assertEquals(1L, (long) results.getTotalResults());
		assertEquals(1, (int) results.getReturnedResultCount());
	}

	@Test
	public void findSourcesForMetadataFilter() {
		GeneralLocationDatumMetadata meta1 = getTestInstance();
		meta1.getMeta().addTag("super");
		dao.store(meta1);

		GeneralLocationDatumMetadata meta2 = getTestInstance();
		meta2.setSourceId(TEST_SOURCE_ID_2);
		dao.store(meta2);

		Set<LocationSourcePK> results = dao.getFilteredSources(new Long[] { TEST_LOC_ID },
				"(&(/**/foo=bar)(t=super))");
		assertNotNull(results);
		assertEquals("Returned results", 1L, results.size());
		assertEquals(new LocationSourcePK(TEST_LOC_ID, TEST_SOURCE_ID), results.iterator().next());

		results = dao.getFilteredSources(new Long[] { TEST_LOC_ID }, "(/**/foo=bar)");
		assertNotNull(results);
		assertEquals("Returned results", 2L, results.size());
		assertTrue(results.contains(new LocationSourcePK(TEST_LOC_ID, TEST_SOURCE_ID)));
		assertTrue(results.contains(new LocationSourcePK(TEST_LOC_ID, TEST_SOURCE_ID_2)));
	}
}
