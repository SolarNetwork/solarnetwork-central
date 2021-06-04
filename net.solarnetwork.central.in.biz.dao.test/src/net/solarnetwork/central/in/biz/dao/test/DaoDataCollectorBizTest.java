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

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import javax.cache.Cache;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.util.JodaDateUtils;

/**
 * Test case for the {@link DaoDataCollectorBiz} class.
 * 
 * @author matt
 * @version 2.2
 */
public class DaoDataCollectorBizTest {

	private static final String TEST_SOURCE_ID = "test.source";

	private DaoDataCollectorBiz biz;

	private DatumEntityDao datumDao;
	private SolarLocationDao locationDao;
	private DatumMetadataBiz datumMetadataBiz;
	private Cache<Serializable, Serializable> datumCache;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		locationDao = EasyMock.createMock(SolarLocationDao.class);
		datumCache = EasyMock.createMock(Cache.class);
		biz = new DaoDataCollectorBiz();
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setDatumDao(datumDao);
		biz.setSolarLocationDao(locationDao);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, datumMetadataBiz, locationDao, datumCache);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, datumMetadataBiz, locationDao, datumCache);
	}

	@Test
	public void findLocation() {
		// GIVEN
		SolarLocation filter = new SolarLocation();
		filter.setCountry("NZ");
		filter.setPostalCode("6011");

		SolarLocation l = new SolarLocation();
		BasicFilterResults<LocationMatch> filterResults = new BasicFilterResults<>(singleton(l), 1L, 0,
				1);
		expect(locationDao.findFiltered(eq(filter), isNull(), eq(0), anyObject()))
				.andReturn(filterResults);

		// WHEN
		replayAll();
		List<LocationMatch> results = biz.findLocations(filter);

		// THEN
		assertNotNull(results);
		assertEquals(1, results.size());

		LocationMatch loc = results.get(0);
		assertThat("Expected location returned", loc, sameInstance(l));
	}

	@Test
	public void addGeneralNodeDatumMetadataNew() {
		// GIVEN
		Long nodeId = UUID.randomUUID().getMostSignificantBits();
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");
		datumMetadataBiz.addGeneralNodeDatumMetadata(nodeId, TEST_SOURCE_ID, meta);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(nodeId);
		biz.addGeneralNodeDatumMetadata(nodeId, TEST_SOURCE_ID, meta);
	}

	@Test
	public void postGeneralNodeDatum() {
		// GIVEN
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(UUID.randomUUID().getMostSignificantBits());
		d.setSourceId(TEST_SOURCE_ID);
		d.setCreated(new DateTime());
		d.setSamples(new GeneralNodeDatumSamples());
		d.getSamples().putInstantaneousSampleValue("foo", 1);

		expect(datumDao.store(d)).andReturn(
				new DatumPK(UUID.randomUUID(), JodaDateUtils.fromJodaToInstant(d.getCreated())));

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(d.getNodeId());
		biz.postGeneralNodeDatum(singleton(d));

		// THEN
	}

	@Test
	public void postGeneralNodeDatum_datumCache() {
		// GIVEN
		biz.setDatumCache(datumCache);
		GeneralNodeDatum d = new GeneralNodeDatum();
		d.setNodeId(UUID.randomUUID().getMostSignificantBits());
		d.setSourceId(TEST_SOURCE_ID);
		d.setCreated(new DateTime());
		d.setSamples(new GeneralNodeDatumSamples());
		d.getSamples().putInstantaneousSampleValue("foo", 1);

		datumCache.put(d.getId(), d);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(d.getNodeId());
		biz.postGeneralNodeDatum(singleton(d));

		// THEN
	}

}
