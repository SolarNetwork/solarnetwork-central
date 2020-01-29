/* ==================================================================
 * DaoDataCollectorBizTest.java - Oct 23, 2011 2:49:59 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.biz.dao.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.domain.GeneralDatumMetadata;

/**
 * Test case for the {@link DaoDataCollectorBiz} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDataCollectorBizTest extends AbstractInBizDaoTestSupport {

	private static final String TEST_SOURCE_ID = "test.source";

	@Autowired
	private DaoDataCollectorBiz biz;

	private DatumMetadataBiz datumMetadataBiz;

	@Before
	public void setup() {
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		biz.setDatumMetadataBiz(datumMetadataBiz);
		setupTestNode();
		setAuthenticatedNode(TEST_NODE_ID);
	}

	@Test
	public void findLocation() {
		SolarLocation filter = new SolarLocation();
		filter.setCountry(TEST_LOC_COUNTRY);
		filter.setPostalCode(TEST_LOC_POSTAL_CODE);
		List<LocationMatch> results = biz.findLocations(filter);
		assertNotNull(results);
		assertEquals(1, results.size());

		LocationMatch loc = results.get(0);
		assertNotNull(loc);
		assertEquals(TEST_LOC_ID, loc.getId());
		assertEquals(TEST_LOC_COUNTRY, loc.getCountry());
		assertEquals(TEST_LOC_POSTAL_CODE, loc.getPostalCode());
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		datumMetadataBiz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.replay(datumMetadataBiz);

		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		EasyMock.verify(datumMetadataBiz);
	}

}
