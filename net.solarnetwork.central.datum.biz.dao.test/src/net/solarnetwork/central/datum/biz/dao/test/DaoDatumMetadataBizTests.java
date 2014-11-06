/* ==================================================================
 * DaoDatumMetadataBizTests.java - Oct 4, 2014 7:17:37 AM
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

package net.solarnetwork.central.datum.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.solarnetwork.central.datum.dao.GeneralLocationDatumMetadataDao;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumMetadataDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadata;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.daum.biz.dao.DaoDatumMetadataBiz;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.test.AbstractCentralTransactionalTest;
import net.solarnetwork.domain.GeneralDatumMetadata;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test cases for the {@link DaoDatumMetadataBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ContextConfiguration
public class DaoDatumMetadataBizTests extends AbstractCentralTransactionalTest {

	private final String TEST_SOURCE_ID = "test.source";
	private final String TEST_SOURCE_ID_2 = "test.source.2";

	@Autowired
	private GeneralNodeDatumMetadataDao generalNodeDatumMetadataDao;

	@Autowired
	private GeneralLocationDatumMetadataDao generalLocationDatumMetadataDao;

	private DaoDatumMetadataBiz biz;

	@Before
	public void setup() {
		biz = new DaoDatumMetadataBiz();
		biz.setGeneralLocationDatumMetadataDao(generalLocationDatumMetadataDao);
		biz.setGeneralNodeDatumMetadataDao(generalNodeDatumMetadataDao);
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);
	}

	@Test
	public void addGeneralNodeDatumMetadataNewWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);
	}

	@Test
	public void findGeneralNodeDatumMetadataSingle() {
		addGeneralNodeDatumMetadataNew();
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);

		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralNodeDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
	}

	@Test
	public void addGeneralNodeDatumMetadataMerge() {
		addGeneralNodeDatumMetadataNew();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bam"); // this should replace
		meta.putInfoValue("oof", "rab");
		meta.addTag("mab");
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralNodeDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
		assertTrue(match instanceof GeneralNodeDatumMetadata);
		meta = ((GeneralNodeDatumMetadata) match).getMeta();
		assertTrue("Has original tag", meta.hasTag("bam"));
		assertTrue("Has new tag", meta.hasTag("mab"));
		assertEquals("Replaced info value", "bam", meta.getInfoString("foo"));
		assertEquals("New info value", "rab", meta.getInfoString("oof"));
	}

	@Test
	public void addGeneralNodeDatumMetadataMergeWithPropertyMeta() {
		addGeneralNodeDatumMetadataNewWithPropertyMeta();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bam"); // this should replace
		meta.putInfoValue("oof", "rab");
		meta.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta.putInfoValue("watts", "unitType", "SI");
		meta.addTag("mab");
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralNodeDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
		assertTrue(match instanceof GeneralNodeDatumMetadata);
		meta = ((GeneralNodeDatumMetadata) match).getMeta();
		assertTrue("Has original tag", meta.hasTag("bam"));
		assertTrue("Has new tag", meta.hasTag("mab"));
		assertEquals("Replaced info value", "bam", meta.getInfoString("foo"));
		assertEquals("New info value", "rab", meta.getInfoString("oof"));
		assertEquals("Replaced info property value", "Wh", meta.getInfoString("watts", "unit"));
		assertEquals("New info property value", "SI", meta.getInfoString("watts", "unitType"));
	}

	@Test
	public void findGeneralNodeDatumMetadataMultiple() {
		addGeneralNodeDatumMetadataNew();

		// add another, for a different source
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID_2, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(2), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(2L), results.getTotalResults());

		Set<NodeSourcePK> expectedKeys = new HashSet<NodeSourcePK>(Arrays.asList(new NodeSourcePK(
				TEST_NODE_ID, TEST_SOURCE_ID), new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID_2)));
		for ( GeneralNodeDatumMetadataFilterMatch match : results.getResults() ) {
			assertTrue("Found expected result", expectedKeys.remove(match.getId()));
		}
		assertEquals("Expected count", 0, expectedKeys.size());
	}

	@Test
	public void removeNode() {
		addGeneralNodeDatumMetadataNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralNodeDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());

		biz.removeGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID);

		// now should be gone
		results = biz.findGeneralNodeDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(0), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(0L), results.getTotalResults());
	}

	@Test
	public void removeNodeNonExisting() {
		addGeneralNodeDatumMetadataNew();
		biz.removeGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID_2);

		// verify first one still there
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = biz.findGeneralNodeDatumMetadata(
				criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralNodeDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
	}

	@Test
	public void addGeneralLocationDatumMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		biz.addGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);
	}

	@Test
	public void addGeneralLocationDatumMetadataNewWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");
		biz.addGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);
	}

	@Test
	public void findGeneralLocationDatumMetadataSingle() {
		addGeneralLocationDatumMetadataNew();
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);

		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralLocationDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
	}

	@Test
	public void addGeneralLocationDatumMetadataMerge() {
		addGeneralLocationDatumMetadataNew();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bam"); // this should replace
		meta.putInfoValue("oof", "rab");
		meta.addTag("mab");
		biz.addGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralLocationDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
		assertTrue(match instanceof GeneralLocationDatumMetadata);
		meta = ((GeneralLocationDatumMetadata) match).getMeta();
		assertTrue("Has original tag", meta.hasTag("bam"));
		assertTrue("Has new tag", meta.hasTag("mab"));
		assertEquals("Replaced info value", "bam", meta.getInfoString("foo"));
		assertEquals("New info value", "rab", meta.getInfoString("oof"));
	}

	@Test
	public void addGeneralLocationDatumMetadataMergeWithPropertyMeta() {
		addGeneralLocationDatumMetadataNewWithPropertyMeta();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bam"); // this should replace
		meta.putInfoValue("oof", "rab");
		meta.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta.putInfoValue("watts", "unitType", "SI");
		meta.addTag("mab");
		biz.addGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralLocationDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
		assertTrue(match instanceof GeneralLocationDatumMetadata);
		meta = ((GeneralLocationDatumMetadata) match).getMeta();
		assertTrue("Has original tag", meta.hasTag("bam"));
		assertTrue("Has new tag", meta.hasTag("mab"));
		assertEquals("Replaced info value", "bam", meta.getInfoString("foo"));
		assertEquals("New info value", "rab", meta.getInfoString("oof"));
		assertEquals("Replaced info property value", "Wh", meta.getInfoString("watts", "unit"));
		assertEquals("New info property value", "SI", meta.getInfoString("watts", "unitType"));
	}

	@Test
	public void findGeneralLocationDatumMetadataMultiple() {
		addGeneralLocationDatumMetadataNew();

		// add another, for a different source
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		biz.addGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID_2, meta);

		DatumFilterCommand criteria = new DatumFilterCommand();
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(2), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(2L), results.getTotalResults());

		Set<LocationSourcePK> expectedKeys = new HashSet<LocationSourcePK>(Arrays.asList(
				new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), new LocationSourcePK(TEST_NODE_ID,
						TEST_SOURCE_ID_2)));
		for ( GeneralLocationDatumMetadataFilterMatch match : results.getResults() ) {
			assertTrue("Found expected result", expectedKeys.remove(match.getId()));
		}
		assertEquals("Expected count", 0, expectedKeys.size());
	}

	@Test
	public void removeLocation() {
		addGeneralLocationDatumMetadataNew();

		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralLocationDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());

		biz.removeGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID);

		// now should be gone
		results = biz.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(0), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(0L), results.getTotalResults());
	}

	@Test
	public void removeLocationNonExisting() {
		addGeneralLocationDatumMetadataNew();
		biz.removeGeneralLocationDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID_2);

		// verify first one still there
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = biz
				.findGeneralLocationDatumMetadata(criteria, null, null, null);
		assertNotNull(results);
		assertEquals("Returned results", Integer.valueOf(1), results.getReturnedResultCount());
		assertEquals("Total results", Long.valueOf(1L), results.getTotalResults());

		GeneralLocationDatumMetadataFilterMatch match = results.getResults().iterator().next();
		assertEquals("Primary key", new LocationSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), match.getId());
	}
}
