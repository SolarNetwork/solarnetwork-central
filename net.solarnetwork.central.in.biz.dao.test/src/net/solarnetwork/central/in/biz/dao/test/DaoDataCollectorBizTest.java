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
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.dao.SolarLocationDao;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.StreamMetadataCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamKind;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.in.biz.dao.DaoDataCollectorBiz;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.domain.GeneralDatumMetadata;
import net.solarnetwork.domain.GeneralNodeDatumSamples;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
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
	private DatumStreamMetadataDao metaDao;
	private SolarLocationDao locationDao;
	private DatumMetadataBiz datumMetadataBiz;
	private Cache<Serializable, Serializable> datumCache;
	private Cache<UUID, ObjectDatumStreamKind> kindCache;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		datumDao = EasyMock.createMock(DatumEntityDao.class);
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		datumMetadataBiz = EasyMock.createMock(DatumMetadataBiz.class);
		locationDao = EasyMock.createMock(SolarLocationDao.class);
		datumCache = EasyMock.createMock(Cache.class);
		kindCache = EasyMock.createMock(Cache.class);
		biz = new DaoDataCollectorBiz();
		biz.setDatumMetadataBiz(datumMetadataBiz);
		biz.setDatumDao(datumDao);
		biz.setMetaDao(metaDao);
		biz.setSolarLocationDao(locationDao);
		biz.setStreamKindCache(kindCache);
	}

	@After
	public void teardown() {
		EasyMock.verify(datumDao, metaDao, datumMetadataBiz, locationDao, datumCache, kindCache);
	}

	private void replayAll() {
		EasyMock.replay(datumDao, metaDao, datumMetadataBiz, locationDao, datumCache, kindCache);
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

	@Test
	public void postStreamDatum_kindNotCached() {
		// GIVEN
		DatumProperties p = DatumProperties.propertiesOf(decimalArray("1.23"), decimalArray("2.34"),
				new String[] { "a" }, new String[] { "b" });
		BasicStreamDatum d = new BasicStreamDatum(UUID.randomUUID(), Instant.now(), p);
		BasicObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(d.getStreamId(),
				"Etc/UTC", ObjectDatumKind.Node, 1L, "test");
		DatumPK datumPk = new DatumPK(d.getStreamId(), d.getTimestamp());

		// look for stream kind in cache
		expect(kindCache.get(d.getStreamId())).andReturn(null);

		// stream kind not found in cache, so look in DB
		Capture<StreamMetadataCriteria> metaCriteriaCaptor = new Capture<>();
		expect(metaDao.findStreamMetadata(capture(metaCriteriaCaptor))).andReturn(meta);

		// cache stream kind result
		Capture<ObjectDatumStreamKind> kindCaptor = new Capture<>();
		kindCache.put(eq(d.getStreamId()), capture(kindCaptor));

		// save datum
		Capture<DatumEntity> datumCaptor = new Capture<>();
		expect(datumDao.save(capture(datumCaptor))).andReturn(datumPk);

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(meta.getObjectId());
		biz.postStreamDatum(singleton(d));

		// THEN
		StreamMetadataCriteria metaCriteria = metaCriteriaCaptor.getValue();
		assertThat("Meta criteria stream ID", metaCriteria.getStreamId(), is(equalTo(d.getStreamId())));

		ObjectDatumStreamKind streamKind = kindCaptor.getValue();
		assertThat("Cached stream kind", streamKind,
				equalTo(new ObjectDatumStreamKind(meta.getObjectId(), meta.getKind())));

		DatumEntity entity = datumCaptor.getValue();
		assertThat("Datum ID copied", entity.getId(), is(equalTo(datumPk)));
		assertThat("Datum timestamp copied", entity.getTimestamp(), is(equalTo(d.getTimestamp())));
		assertThat("Datum i copied", entity.getProperties().getInstantaneous(),
				is(arrayContaining(p.getInstantaneous())));
		assertThat("Datum a copied", entity.getProperties().getAccumulating(),
				is(arrayContaining(p.getAccumulating())));
		assertThat("Datum s copied", entity.getProperties().getStatus(),
				is(arrayContaining(p.getStatus())));
		assertThat("Datum t copied", entity.getProperties().getTags(), is(arrayContaining(p.getTags())));
	}

	@Test
	public void postStreamDatum_kindNotCached_datumCache() {
		// GIVEN
		biz.setDatumCache(datumCache);

		DatumProperties p = DatumProperties.propertiesOf(decimalArray("1.23"), decimalArray("2.34"),
				new String[] { "a" }, new String[] { "b" });
		BasicStreamDatum d = new BasicStreamDatum(UUID.randomUUID(), Instant.now(), p);
		BasicObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(d.getStreamId(),
				"Etc/UTC", ObjectDatumKind.Node, 1L, "test");
		DatumPK datumPk = new DatumPK(d.getStreamId(), d.getTimestamp());

		// look for stream kind in cache
		expect(kindCache.get(d.getStreamId())).andReturn(null);

		// stream kind not found in cache, so look in DB
		Capture<StreamMetadataCriteria> metaCriteriaCaptor = new Capture<>();
		expect(metaDao.findStreamMetadata(capture(metaCriteriaCaptor))).andReturn(meta);

		// cache stream kind result
		Capture<ObjectDatumStreamKind> kindCaptor = new Capture<>();
		kindCache.put(eq(d.getStreamId()), capture(kindCaptor));

		// add datum to cache
		Capture<DatumEntity> datumCaptor = new Capture<>();
		datumCache.put(eq(datumPk), capture(datumCaptor));

		// WHEN
		replayAll();
		SecurityUtils.becomeNode(meta.getObjectId());
		biz.postStreamDatum(singleton(d));

		// THEN
		StreamMetadataCriteria metaCriteria = metaCriteriaCaptor.getValue();
		assertThat("Meta criteria stream ID", metaCriteria.getStreamId(), is(equalTo(d.getStreamId())));

		ObjectDatumStreamKind streamKind = kindCaptor.getValue();
		assertThat("Cached stream kind", streamKind,
				equalTo(new ObjectDatumStreamKind(meta.getObjectId(), meta.getKind())));

		DatumEntity entity = datumCaptor.getValue();
		assertThat("Datum ID copied", entity.getId(), is(equalTo(datumPk)));
		assertThat("Datum timestamp copied", entity.getTimestamp(), is(equalTo(d.getTimestamp())));
		assertThat("Datum i copied", entity.getProperties().getInstantaneous(),
				is(arrayContaining(p.getInstantaneous())));
		assertThat("Datum a copied", entity.getProperties().getAccumulating(),
				is(arrayContaining(p.getAccumulating())));
		assertThat("Datum s copied", entity.getProperties().getStatus(),
				is(arrayContaining(p.getStatus())));
		assertThat("Datum t copied", entity.getProperties().getTags(), is(arrayContaining(p.getTags())));
	}

}
