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

import static java.util.Collections.singleton;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.common.dao.BasicLocationRequestCriteria;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;
import net.solarnetwork.central.common.dao.LocationRequestDao;
import net.solarnetwork.central.datum.biz.dao.DaoDatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.central.domain.LocationRequestStatus;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link DaoDatumMetadataBiz} class.
 *
 * @author matt
 * @version 2.0
 */
public class DaoDatumMetadataBizTests {

	private final Long TEST_NODE_ID = -11L;
	private final Long TEST_LOCATION_ID = -111L;
	private final String TEST_SOURCE_ID = "test.source";

	private DatumStreamMetadataDao metaDao;
	private LocationRequestDao locationRequestDao;
	private DaoDatumMetadataBiz biz;

	private void replayAll() {
		replay(metaDao, locationRequestDao);
	}

	private void verifyAll() {
		verify(metaDao, locationRequestDao);
	}

	@Before
	public void setup() {
		metaDao = EasyMock.createMock(DatumStreamMetadataDao.class);
		locationRequestDao = EasyMock.createMock(LocationRequestDao.class);
		biz = new DaoDatumMetadataBiz(metaDao, locationRequestDao, JsonUtils.newDatumObjectMapper());
	}

	@Test
	public void addGeneralNodeDatumMetadata_new() {
		// GIVEN
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.emptyList());

		NodeSourcePK pk = new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(meta, null));

		// WHEN
		replayAll();
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_NODE_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void addGeneralNodeDatumMetadata_newWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.emptyList());

		NodeSourcePK pk = new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(meta, null));

		// WHEN
		replayAll();
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_NODE_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void findGeneralNodeDatumMetadata() {
		// GIVEN
		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK> r = biz
				.findGeneralNodeDatumMetadata(filter, null, null, null);

		// THEN
		verifyAll();
		assertThat("Source ID passed to DAO", criteriaCaptor.getValue().getSourceId(),
				equalTo(filter.getSourceId()));
		assertThat("Node type passed to DAO", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
		assertThat("Result converted", r.getReturnedResultCount(), equalTo(1));
		GeneralNodeDatumMetadataFilterMatch match = r.iterator().next();
		assertThat("Result source ID", match.getId(),
				equalTo(new NodeSourcePK(meta.getObjectId(), meta.getSourceId())));
	}

	@Test
	public void addGeneralNodeDatumMetadata_merge() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.addTag("mab");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, null, null, null,
				JsonUtils.getJSONString(meta, null));
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.singleton(streamMeta));

		NodeSourcePK pk = new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralDatumMetadata merged = new GeneralDatumMetadata(meta);
		merged.merge(meta2, true);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(merged, null));

		// WHEN
		replayAll();
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta2);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_NODE_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void addGeneralNodeDatumMetadata_mergeWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta2.putInfoValue("watts", "unitType", "SI");
		meta2.addTag("mab");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, null, null, null,
				JsonUtils.getJSONString(meta, null));
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.singleton(streamMeta));

		NodeSourcePK pk = new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID);
		GeneralDatumMetadata merged = new GeneralDatumMetadata(meta);
		merged.merge(meta2, true);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(merged, null));

		// WHEN
		replayAll();
		biz.addGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID, meta2);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_NODE_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Node));
	}

	@Test
	public void remove_node() {
		// GIVEN
		NodeSourcePK pk = new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, null);

		// WHEN
		replayAll();
		biz.removeGeneralNodeDatumMetadata(TEST_NODE_ID, TEST_SOURCE_ID);

		// THEN
		verifyAll();
	}

	@Test
	public void addGeneralLocationDatumMetadata_new() {
		// GIVEN
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.emptyList());

		LocationSourcePK pk = new LocationSourcePK(TEST_LOCATION_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(meta, null));

		// WHEN
		replayAll();
		biz.addGeneralLocationDatumMetadata(TEST_LOCATION_ID, TEST_SOURCE_ID, meta);

		// THEN
		verifyAll();
		assertThat("Query loc ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_LOCATION_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void addGeneralLocationDatumMetadata_newWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.emptyList());

		LocationSourcePK pk = new LocationSourcePK(TEST_LOCATION_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(meta, null));

		// WHEN
		replayAll();
		biz.addGeneralLocationDatumMetadata(TEST_LOCATION_ID, TEST_SOURCE_ID, meta);

		// THEN
		verifyAll();
		assertThat("Query loc ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_LOCATION_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void findGeneralLocationDatumMetadata() {
		// GIVEN
		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata meta = BasicObjectDatumStreamMetadata.emptyMeta(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Location, 1L, TEST_SOURCE_ID);
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor))).andReturn(singleton(meta));

		// WHEN
		replayAll();
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setSourceId(TEST_SOURCE_ID);
		FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> r = biz
				.findGeneralLocationDatumMetadata(filter, null, null, null);

		// THEN
		verifyAll();
		assertThat("Source ID passed to DAO", criteriaCaptor.getValue().getSourceId(),
				equalTo(filter.getSourceId()));
		assertThat("Location type passed to DAO", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
		assertThat("Result converted", r.getReturnedResultCount(), equalTo(1));
		GeneralLocationDatumMetadataFilterMatch match = r.iterator().next();
		assertThat("Result source ID", match.getId(),
				equalTo(new LocationSourcePK(meta.getObjectId(), meta.getSourceId())));
	}

	@Test
	public void addGeneralLocationDatumMetadata_merge() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.addTag("bam");

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.addTag("mab");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Location, TEST_LOCATION_ID, TEST_SOURCE_ID, null, null, null,
				JsonUtils.getJSONString(meta, null));
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.singleton(streamMeta));

		LocationSourcePK pk = new LocationSourcePK(TEST_LOCATION_ID, TEST_SOURCE_ID);
		GeneralDatumMetadata merged = new GeneralDatumMetadata(meta);
		merged.merge(meta2, true);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(merged, null));

		// WHEN
		replayAll();
		biz.addGeneralLocationDatumMetadata(TEST_LOCATION_ID, TEST_SOURCE_ID, meta2);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_LOCATION_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void addGeneralLocationDatumMetadataMergeWithPropertyMeta() {
		GeneralDatumMetadata meta = new GeneralDatumMetadata();
		meta.putInfoValue("foo", "bar");
		meta.putInfoValue("watts", "unit", "W");
		meta.addTag("bam");

		GeneralDatumMetadata meta2 = new GeneralDatumMetadata();
		meta2.putInfoValue("foo", "bam"); // this should replace
		meta2.putInfoValue("oof", "rab");
		meta2.putInfoValue("watts", "unit", "Wh"); // this should replace
		meta2.putInfoValue("watts", "unitType", "SI");
		meta2.addTag("mab");

		Capture<ObjectStreamCriteria> criteriaCaptor = new Capture<>();
		ObjectDatumStreamMetadata streamMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Location, TEST_LOCATION_ID, TEST_SOURCE_ID, null, null, null,
				JsonUtils.getJSONString(meta, null));
		expect(metaDao.findDatumStreamMetadata(capture(criteriaCaptor)))
				.andReturn(Collections.singleton(streamMeta));

		LocationSourcePK pk = new LocationSourcePK(TEST_LOCATION_ID, TEST_SOURCE_ID);
		GeneralDatumMetadata merged = new GeneralDatumMetadata(meta);
		merged.merge(meta2, true);
		metaDao.replaceJsonMeta(pk, JsonUtils.getJSONString(merged, null));

		// WHEN
		replayAll();
		biz.addGeneralLocationDatumMetadata(TEST_LOCATION_ID, TEST_SOURCE_ID, meta2);

		// THEN
		verifyAll();
		assertThat("Query node ID", criteriaCaptor.getValue().getObjectId(), equalTo(TEST_LOCATION_ID));
		assertThat("Query source ID", criteriaCaptor.getValue().getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("Query object kind", criteriaCaptor.getValue().getObjectKind(),
				equalTo(ObjectDatumKind.Location));
	}

	@Test
	public void remove_location() {
		// GIVEN
		LocationSourcePK pk = new LocationSourcePK(TEST_LOCATION_ID, TEST_SOURCE_ID);
		metaDao.replaceJsonMeta(pk, null);

		// WHEN
		replayAll();
		biz.removeGeneralLocationDatumMetadata(TEST_LOCATION_ID, TEST_SOURCE_ID);

		// THEN
		verifyAll();
	}

	@Test
	public void findLocationRequests() {
		// GIVEN
		Capture<LocationRequestCriteria> criteriaCaptor = new Capture<>();
		net.solarnetwork.dao.FilterResults<LocationRequest, Long> bfr = BasicFilterResults
				.filterResults(Collections.<LocationRequest> emptyList(), null, 0L, 0);
		expect(locationRequestDao.findFiltered(capture(criteriaCaptor), EasyMock.isNull(),
				EasyMock.isNull(), EasyMock.isNull())).andReturn(bfr);

		// WHEN
		replayAll();
		BasicLocationRequestCriteria filter = new BasicLocationRequestCriteria();
		filter.setRequestStatus(LocationRequestStatus.Created);
		Long userId = UUID.randomUUID().getLeastSignificantBits();
		net.solarnetwork.dao.FilterResults<LocationRequest, Long> results = biz
				.findLocationRequests(userId, filter, null, null, null);

		// THEN
		assertThat("Results returned directly from DAO", results, is(sameInstance(bfr)));

		LocationRequestCriteria crit = criteriaCaptor.getValue();
		assertThat("Copy of filter passed to DAO", crit, is(not(sameInstance(filter))));
		assertThat("User ID added to DAO filter", crit.getUserIds(), is(arrayContaining(userId)));
	}

	@Test
	public void submitLocationRequest() {
		final Long userId = UUID.randomUUID().getLeastSignificantBits();

		Capture<LocationRequest> reqCaptor = new Capture<>();
		final Long reqId = UUID.randomUUID().getLeastSignificantBits();
		expect(locationRequestDao.save(capture(reqCaptor))).andReturn(reqId);
		expect(locationRequestDao.get(reqId)).andAnswer(new IAnswer<LocationRequest>() {

			@Override
			public LocationRequest answer() throws Throwable {
				return reqCaptor.getValue();
			}
		});

		// WHEN
		replayAll();
		LocationRequestInfo info = new LocationRequestInfo();
		info.setFeatures(new LinkedHashSet<>(Arrays.asList("weather", "forecast")));
		info.setLocation(new BasicLocation("Foo", "NZ", null, "Welly", "Welly City", null, null, null,
				null, null, "Pacific/Auckland"));
		biz.submitLocationRequest(userId, info);

		// THEN
		LocationRequest req = reqCaptor.getValue();
		assertThat("Status configured", req.getStatus(), is(equalTo(LocationRequestStatus.Submitted)));
		assertThat("Json generated", req.getJsonData(),
				is(equalTo("{\"features\":[\"weather\",\"forecast\"],\"location\":"
						+ "{\"name\":\"Foo\",\"country\":\"NZ\",\"stateOrProvince\":\"Welly\",\"locality\":\"Welly City\",\"zone\":\"Pacific/Auckland\"}"
						+ "}")));
	}

}
