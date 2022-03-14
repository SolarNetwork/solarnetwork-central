/* ==================================================================
 * JdbcDatumEntityDao_DatumStreamMetadataDaoTests.java - 19/11/2020 4:41:41 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao.jdbc.test;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertDatumStream;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.loadJsonDatumResource;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertSecurityToken;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static net.solarnetwork.domain.SimpleSortDescriptor.sorts;
import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.BasicSecurityPolicy;
import net.solarnetwork.central.security.SecurityPolicy;
import net.solarnetwork.central.security.SecurityTokenStatus;
import net.solarnetwork.central.security.SecurityTokenType;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.SimpleLocation;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link DatumStreamMetadataDao}.
 * 
 * @author matt
 * @version 2.0
 */
public class JdbcDatumEntityDao_DatumStreamMetadataDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	private Cache<UUID, ObjectDatumStreamMetadata> cache;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);

		cache = EasyMock.createMock(Cache.class);
	}

	public void replayAll() {
		EasyMock.replay(cache);
	}

	@After
	public void teardown() {
		EasyMock.verify(cache);
	}

	@Test
	public void findObjectMetadata_nodes() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					format("s%d", i), new String[] { "a", "b", "c" }, new String[] { "d", "e" },
					new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		filter.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( ObjectDatumStreamMetadata expected : data ) {
			ObjectDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadata("location meta", meta, expected);
		}
	}

	@Test
	public void findObjectMetadataIds_nodes() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					format("s%d", i), new String[] { "a", "b", "c" }, new String[] { "d", "e" },
					new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);
		final Set<ObjectDatumStreamMetadataId> ids = data.stream().map(d -> {
			return new ObjectDatumStreamMetadataId(d.getStreamId(), d.getKind(), d.getObjectId(),
					d.getSourceId());
		}).collect(Collectors.toSet());

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		filter.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadataId> results = dao.findDatumStreamMetadataIds(filter);

		assertThat("Results returned", results, notNullValue());
		Set<ObjectDatumStreamMetadataId> resultIds = StreamSupport.stream(results.spliterator(), false)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		assertThat("Stream IDs same", resultIds, equalTo(ids));
	}

	@Test
	public void findObjectMetadata_nodes_withJson() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		final String json = "{\"foo\":\"bar\"}";
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node, (long) i,
					format("s%d", i), new String[] { "a", "b", "c" }, new String[] { "d", "e" },
					new String[] { "f" }, json));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( ObjectDatumStreamMetadata expected : data ) {
			ObjectDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadata("location meta", meta, expected);
		}
	}

	@Test
	public void findObjectMetadata_nodes_noSort() {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		ObjectDatumStreamMetadata meta2 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "b", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		ObjectDatumStreamMetadata meta3 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "b", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta1, meta2, meta3));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		Set<UUID> metas = StreamSupport.stream(results.spliterator(), false)
				.map(DatumStreamMetadata::getStreamId).collect(toSet());
		assertThat("Results returned in unknown order", metas,
				containsInAnyOrder(meta1.getStreamId(), meta2.getStreamId()));
	}

	@Test
	public void findObjectMetadata_nodes_sortNodeSource() {
		// GIVEN
		ObjectDatumStreamMetadata meta1 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "z", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		ObjectDatumStreamMetadata meta2 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		ObjectDatumStreamMetadata meta3 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "d", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		ObjectDatumStreamMetadata meta4 = new BasicObjectDatumStreamMetadata(randomUUID(), "UTC",
				ObjectDatumKind.Node, 2L, "z", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, asList(meta1, meta2, meta3, meta4));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L });
		filter.setSorts(sorts("node", "source"));
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		List<UUID> metas = StreamSupport.stream(results.spliterator(), false)
				.map(DatumStreamMetadata::getStreamId).collect(toList());
		assertThat("Results returned in node/source order", metas, contains(meta2.getStreamId(),
				meta1.getStreamId(), meta3.getStreamId(), meta4.getStreamId()));
	}

	@Test
	public void findObjectMetadata_nodes_absoluteDates() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, 1L, "a", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		DatumEntity datum = new DatumEntity(streamId, now(), now(),
				propertiesOf(decimalArray("1.1", "1.2"), decimalArray("2.1"), null, null));
		DatumDbUtils.insertDatum(log, jdbcTemplate, singleton(datum));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setStartDate(datum.getCreated());
		filter.setEndDate(datum.getCreated().plusSeconds(1));
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		List<ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		assertThat("Results returned", metas, hasSize(1));
		assertThat("Result stream", metas.get(0).getStreamId(), equalTo(streamId));
	}

	@Test
	public void findObjectMetadata_nodes_absoluteDates_dateOutOfBounds() throws IOException {
		// GIVEN
		List<GeneralNodeDatum> datums = loadJsonDatumResource("test-datum-01.txt", getClass());
		insertDatumStream(log, jdbcTemplate, datums, TEST_TZ);
		Instant start = datums.get(0).getCreated();

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(1L);
		filter.setStartDate(start.minusSeconds(1));
		filter.setEndDate(start);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("Result returned", metas, hasSize(0));
	}

	@Test
	public void findObjectMetadata_nodes_token_withPolicy() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, "/test/source/102", new String[] { "a", "b" },
				new String[] { "c" }, null, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestLocation(TEST_LOC_ID, "UTC");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "test");

		String tokenId = "01234567890123456789";
		SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<>(asList(TEST_NODE_ID)))
				.withSourceIds(new HashSet<>(asList("/test/source/102", "/test/source/104"))).build();
		insertSecurityToken(jdbcTemplate, tokenId, "pass", TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(),
				JsonUtils.getJSONString(policy, null));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setTokenId(tokenId);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("Results", metas, hasSize(1));
		assertThat("Result stream", metas.get(0).getStreamId(), equalTo(streamId));
	}

	@Test
	public void findObjectMetadata_nodes_token_withPolicy_noMatchPolicySource() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, "/test/source/102", new String[] { "a", "b" },
				new String[] { "c" }, null, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestLocation(TEST_LOC_ID, "UTC");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "test");

		String tokenId = "01234567890123456789";
		SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<>(asList(TEST_NODE_ID)))
				.withSourceIds(new HashSet<>(asList("/test/source/NO"))).build();
		insertSecurityToken(jdbcTemplate, tokenId, "pass", TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(),
				JsonUtils.getJSONString(policy, null));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setTokenId(tokenId);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("Results", metas, hasSize(0));
	}

	@Test
	public void findObjectMetadata_nodes_token_withPolicy_noMatchPolicyNode() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Node, TEST_NODE_ID, "/test/source/102", new String[] { "a", "b" },
				new String[] { "c" }, null, null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		setupTestUser(TEST_USER_ID, TEST_USERNAME);
		setupTestLocation(TEST_LOC_ID, "UTC");
		setupTestNode(TEST_NODE_ID, TEST_LOC_ID);
		setupTestUserNode(TEST_USER_ID, TEST_NODE_ID, "test");

		String tokenId = "01234567890123456789";
		SecurityPolicy policy = new BasicSecurityPolicy.Builder()
				.withNodeIds(new HashSet<>(asList(TEST_NODE_ID - 1L)))
				.withSourceIds(new HashSet<>(asList("/test/source/102", "/test/source/104"))).build();
		insertSecurityToken(jdbcTemplate, tokenId, "pass", TEST_USER_ID,
				SecurityTokenStatus.Active.name(), SecurityTokenType.ReadNodeData.name(),
				JsonUtils.getJSONString(policy, null));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setTokenId(tokenId);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("Results", metas, hasSize(0));
	}

	@Test
	public void findObjectMetadata_locations() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Location,
					(long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		filter.setObjectKind(ObjectDatumKind.Location);
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(streamIds));

		for ( ObjectDatumStreamMetadata expected : data ) {
			ObjectDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadata("location meta", meta, expected);
		}
	}

	@Test
	public void findObjectMetadata_locations_withJson() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		final String json = "{\"foo\":\"bar\"}";
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Location,
					(long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" }, json));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(streamIds));

		for ( ObjectDatumStreamMetadata expected : data ) {
			ObjectDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadata("location meta", meta, expected);
		}
	}

	@Test
	public void findObjectMetadata_locations_absoluteDates() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Location, 1L, "a", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		DatumEntity datum = new DatumEntity(streamId, now(), now(),
				propertiesOf(decimalArray("1.1", "1.2"), decimalArray("2.1"), null, null));
		DatumDbUtils.insertDatum(log, jdbcTemplate, singleton(datum));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setStartDate(datum.getCreated());
		filter.setEndDate(datum.getCreated().plusSeconds(1));
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		List<ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		assertThat("Results returned", metas, hasSize(1));
		assertThat("Result stream", metas.get(0).getStreamId(), equalTo(streamId));
	}

	@Test
	public void findObjectMetadata_locations_absoluteDates_dateOutOfBounds() {
		// GIVEN
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, "UTC",
				ObjectDatumKind.Location, 1L, "a", new String[] { "a", "b" }, new String[] { "c" }, null,
				null);
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		DatumEntity datum = new DatumEntity(streamId, now(), now(),
				propertiesOf(decimalArray("1.1", "1.2"), decimalArray("2.1"), null, null));
		DatumDbUtils.insertDatum(log, jdbcTemplate, singleton(datum));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationId(1L);
		filter.setStartDate(datum.getCreated().minusSeconds(1));
		filter.setEndDate(datum.getCreated());
		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		List<ObjectDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toList());
		assertThat("Results returned", metas, hasSize(0));
	}

	@Test
	public void findObjectMetadata_locations_geo() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final List<UUID> streamIds = new ArrayList<>(3);
		final List<String> zones = Arrays.asList("Pacific/Auckland", "UTC", "America/Los_Angeles");
		for ( int i = 1; i <= 3; i++ ) {
			setupTestLocation((long) i, zones.get(i - 1));
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicObjectDatumStreamMetadata(streamId, zones.get(i - 1),
					ObjectDatumKind.Location, (long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setRegion(TEST_LOC_REGION);
		locFilter.setTimeZoneId("UTC");
		filter.setLocation(locFilter);

		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("One match for region +  time zone criteria", metas, hasSize(1));
		assertThat("Expected stream match", metas.get(0).getStreamId(), equalTo(streamIds.get(1)));
		assertLocation("Region + time zone location", metas.get(0).getLocation(),
				new BasicLocation(null, TEST_LOC_COUNTRY, TEST_LOC_REGION, null, null,
						TEST_LOC_POSTAL_CODE, null, null, null, null, "UTC"));
	}

	@Test
	public void findObjectMetadata_locations_geo_tags_and() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final List<UUID> streamIds = new ArrayList<>(3);
		final List<String> zones = Arrays.asList("Pacific/Auckland", "UTC", "America/Los_Angeles");
		for ( int i = 1; i <= 3; i++ ) {
			setupTestLocation((long) i, zones.get(i - 1));
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);

			// add tags like t1, p
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.setTags(new LinkedHashSet<>(
					asList(format("t%d", i), zones.get(i - 1).substring(0, 1).toLowerCase())));

			data.add(new BasicObjectDatumStreamMetadata(streamId, zones.get(i - 1),
					ObjectDatumKind.Location, (long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" },
					JsonUtils.getJSONString(meta, null)));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setRegion(TEST_LOC_REGION);
		filter.setLocation(locFilter);
		filter.setSearchFilter("(&(/t=t3)(/t=a))");

		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("One match for region +  tag criteria", metas, hasSize(1));
		assertThat("Expected stream match", metas.get(0).getStreamId(), equalTo(streamIds.get(2)));
		assertLocation("Region + tag location", metas.get(0).getLocation(),
				new BasicLocation(null, TEST_LOC_COUNTRY, TEST_LOC_REGION, null, null,
						TEST_LOC_POSTAL_CODE, null, null, null, null, "America/Los_Angeles"));
	}

	@Test
	public void findObjectMetadata_locations_geo_tags_or() {
		// GIVEN
		final List<ObjectDatumStreamMetadata> data = new ArrayList<>(3);
		final List<UUID> streamIds = new ArrayList<>(3);
		final List<String> zones = Arrays.asList("Pacific/Auckland", "UTC", "America/Los_Angeles");
		for ( int i = 1; i <= 3; i++ ) {
			setupTestLocation((long) i, zones.get(i - 1));
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);

			// add tags like t1, p
			GeneralDatumMetadata meta = new GeneralDatumMetadata();
			meta.setTags(new LinkedHashSet<>(
					asList(format("t%d", i), zones.get(i - 1).substring(0, 1).toLowerCase())));

			data.add(new BasicObjectDatumStreamMetadata(streamId, zones.get(i - 1),
					ObjectDatumKind.Location, (long) i, format("s%d", i), new String[] { "a", "b", "c" },
					new String[] { "d", "e" }, new String[] { "f" },
					JsonUtils.getJSONString(meta, null)));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setObjectKind(ObjectDatumKind.Location);
		filter.setSorts(sorts("loc"));
		SimpleLocation locFilter = new SimpleLocation();
		locFilter.setRegion(TEST_LOC_REGION);
		filter.setLocation(locFilter);
		filter.setSearchFilter("(|(/t=t3)(/t=p))");

		Iterable<ObjectDatumStreamMetadata> results = dao.findDatumStreamMetadata(filter);

		// THEN
		assertThat("Results returned", results, notNullValue());
		List<ObjectDatumStreamMetadata> metas = stream(results.spliterator(), false).collect(toList());
		assertThat("Two match for region +  tags criteria", metas, hasSize(2));

		assertThat("Expected stream match", metas.get(0).getStreamId(), equalTo(streamIds.get(0)));
		assertLocation("Region + tags location 1", metas.get(0).getLocation(),
				new BasicLocation(null, TEST_LOC_COUNTRY, TEST_LOC_REGION, null, null,
						TEST_LOC_POSTAL_CODE, null, null, null, null, "Pacific/Auckland"));

		assertThat("Expected stream match", metas.get(1).getStreamId(), equalTo(streamIds.get(2)));
		assertLocation("Region + tags location 1", metas.get(1).getLocation(),
				new BasicLocation(null, TEST_LOC_COUNTRY, TEST_LOC_REGION, null, null,
						TEST_LOC_POSTAL_CODE, null, null, null, null, "America/Los_Angeles"));
	}

	@Test
	public void metadataForStream_notFound() {
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(UUID.randomUUID());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		assertThat("Metadata not found", result, nullValue());
	}

	@Test
	public void metadataForStream_node() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadata("returned meta", result, meta);
	}

	@Test
	public void metadataForStream_node_withJson() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" }, "{\"foo\":\"bar\"}");
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadata("returned meta", result, meta);
	}

	@Test
	public void metadataForStream_cacheMiss() {
		// GIVEN
		dao.setStreamMetadataCache(cache);
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		expect(cache.get(meta.getStreamId())).andReturn(null);
		Capture<ObjectDatumStreamMetadata> metaCaptor = new Capture<>();
		cache.put(eq(meta.getStreamId()), capture(metaCaptor));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadata("returned meta", result, meta);
		assertDatumStreamMetadata("cached meta", metaCaptor.getValue(), meta);
	}

	@Test
	public void metadataForStream_cacheHit() {
		// GIVEN
		dao.setStreamMetadataCache(cache);
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });

		expect(cache.get(meta.getStreamId())).andReturn(meta);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertThat("Cached metadata returned", result, sameInstance(meta));
	}

	@Test
	public void metadataForStream_location() {
		// GIVEN
		setupTestLocation(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadata("returned meta", result, meta);
	}

	@Test
	public void replaceJson_node_notFound() {
		// GIVEN

		// WHEN
		replayAll();
		final String json = "{\"foo\":\"bar\"}";
		dao.replaceJsonMeta(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), json);
	}

	private ObjectDatumStreamMetadata metaForStream(UUID streamId) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		return dao.findStreamMetadata(filter);
	}

	@Test
	public void replaceJson_node_create() {
		// GIVEN
		setupTestNode(); // for TZ
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		final String json = "{\"foo\":\"bar\"}";
		dao.replaceJsonMeta(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), json);

		// THEN
		ObjectDatumStreamMetadata result = metaForStream(streamId);
		assertThat("JSON persisted", getStringMap(result.getMetaJson()), equalTo(getStringMap(json)));
	}

	@Test
	public void replaceJson_node_create_new() {
		// GIVEN

		// WHEN
		replayAll();
		final String json = "{\"foo\":\"bar\"}";
		dao.replaceJsonMeta(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), json);

		// THEN
		List<ObjectDatumStreamMetadata> metas = DatumDbUtils.listNodeMetadata(jdbcTemplate);
		assertThat("Stream created", metas, hasSize(1));
		assertThat("Stream object ID matches", metas.get(0).getObjectId(), equalTo(TEST_NODE_ID));
		assertThat("Stream source ID matches", metas.get(0).getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("JSON persisted", getStringMap(metas.get(0).getMetaJson()),
				equalTo(getStringMap(json)));
	}

	@Test
	public void replaceJson_location_create() {
		// GIVEN
		setupTestLocation(); // for TZ
		UUID streamId = UUID.randomUUID();
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(streamId, TEST_TZ,
				ObjectDatumKind.Location, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		final String json = "{\"foo\":\"bar\"}";
		dao.replaceJsonMeta(new LocationSourcePK(TEST_LOC_ID, TEST_SOURCE_ID), json);

		// THEN
		ObjectDatumStreamMetadata result = metaForStream(streamId);
		assertThat("JSON persisted", getStringMap(result.getMetaJson()), equalTo(getStringMap(json)));
	}

	@Test
	public void replaceJson_location_create_new() {
		// GIVEN

		// WHEN
		replayAll();
		final String json = "{\"foo\":\"bar\"}";
		dao.replaceJsonMeta(new LocationSourcePK(TEST_LOC_ID, TEST_SOURCE_ID), json);

		// THEN
		List<ObjectDatumStreamMetadata> metas = DatumDbUtils.listLocationMetadata(jdbcTemplate);
		assertThat("Stream created", metas, hasSize(1));
		assertThat("Stream object ID matches", metas.get(0).getObjectId(), equalTo(TEST_LOC_ID));
		assertThat("Stream source ID matches", metas.get(0).getSourceId(), equalTo(TEST_SOURCE_ID));
		assertThat("JSON persisted", getStringMap(metas.get(0).getMetaJson()),
				equalTo(getStringMap(json)));
	}

	@Test
	public void replaceJson_node_storeVeryBigValues() {
		// GIVEN
		GeneralDatumMetadata m = new GeneralDatumMetadata();
		m.putInfoValue("watt_hours", 39309570293789380L);
		m.putInfoValue("very_big", new BigInteger("93475092039478209375027350293523957"));
		m.putInfoValue("watts", 498475890235787897L);
		m.putInfoValue("floating", new BigDecimal("293487590845639845728947589237.49087"));

		// WHEN
		replayAll();
		final String json = JsonUtils.getJSONString(m, null);
		dao.replaceJsonMeta(new NodeSourcePK(TEST_NODE_ID, TEST_SOURCE_ID), json);

		// THEN
		ObjectDatumStreamMetadata meta = DatumDbUtils.listNodeMetadata(jdbcTemplate).get(0);
		assertThat("JSON persisted", getStringMap(meta.getMetaJson()), equalTo(getStringMap(json)));
	}

	@Test
	public void updateIdAttributes_node_object() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		Long newNodeId = UUID.randomUUID().getLeastSignificantBits();
		ObjectDatumStreamMetadataId id = dao.updateIdAttributes(meta.getKind(), meta.getStreamId(),
				newNodeId, null);

		// THEN
		assertThat("Final ID returned", id, is(notNullValue()));
		assertThat("Returned kind matches", id.getKind(), is(meta.getKind()));
		assertThat("Returned stream ID matches", id.getStreamId(), is(meta.getStreamId()));
		assertThat("Returned object ID new value", id.getObjectId(), is(newNodeId));
		assertThat("Returned source ID unchanged", id.getSourceId(), is(meta.getSourceId()));
	}

	@Test
	public void updateIdAttributes_node_object_clearsCache() {
		// GIVEN
		dao.setStreamMetadataCache(cache);
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		expect(cache.remove(meta.getStreamId())).andReturn(true);

		// WHEN
		replayAll();
		Long newNodeId = UUID.randomUUID().getLeastSignificantBits();
		ObjectDatumStreamMetadataId id = dao.updateIdAttributes(meta.getKind(), meta.getStreamId(),
				newNodeId, null);

		// THEN
		assertThat("Final ID returned", id, is(notNullValue()));
		assertThat("Returned kind matches", id.getKind(), is(meta.getKind()));
		assertThat("Returned stream ID matches", id.getStreamId(), is(meta.getStreamId()));
		assertThat("Returned object ID new value", id.getObjectId(), is(newNodeId));
		assertThat("Returned source ID unchanged", id.getSourceId(), is(meta.getSourceId()));
	}

	@Test
	public void updateIdAttributes_node_objectAndSource() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Node, TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		Long newNodeId = UUID.randomUUID().getLeastSignificantBits();
		String newSourceId = UUID.randomUUID().toString();
		ObjectDatumStreamMetadataId id = dao.updateIdAttributes(meta.getKind(), meta.getStreamId(),
				newNodeId, newSourceId);

		// THEN
		assertThat("Final ID returned", id, is(notNullValue()));
		assertThat("Returned kind matches", id.getKind(), is(meta.getKind()));
		assertThat("Returned stream ID matches", id.getStreamId(), is(meta.getStreamId()));
		assertThat("Returned object ID new value", id.getObjectId(), is(newNodeId));
		assertThat("Returned source ID new value", id.getSourceId(), is(newSourceId));
	}

	@Test
	public void updateIdAttributes_loc_object() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Location, TEST_PRICE_LOC_ID, TEST_SOURCE_ID,
				new String[] { "a", "b", "c" }, new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		Long newLocId = UUID.randomUUID().getLeastSignificantBits();
		ObjectDatumStreamMetadataId id = dao.updateIdAttributes(meta.getKind(), meta.getStreamId(),
				newLocId, null);

		// THEN
		assertThat("Final ID returned", id, is(notNullValue()));
		assertThat("Returned kind matches", id.getKind(), is(meta.getKind()));
		assertThat("Returned stream ID matches", id.getStreamId(), is(meta.getStreamId()));
		assertThat("Returned object ID new value", id.getObjectId(), is(newLocId));
		assertThat("Returned source ID unchanged", id.getSourceId(), is(meta.getSourceId()));
	}

	@Test
	public void updateIdAttributes_loc_objectAndSource() {
		// GIVEN
		setupTestNode(); // for TZ
		ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				ObjectDatumKind.Location, TEST_PRICE_LOC_ID, TEST_SOURCE_ID,
				new String[] { "a", "b", "c" }, new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		Long newLocId = UUID.randomUUID().getLeastSignificantBits();
		String newSourceId = UUID.randomUUID().toString();
		ObjectDatumStreamMetadataId id = dao.updateIdAttributes(meta.getKind(), meta.getStreamId(),
				newLocId, newSourceId);

		// THEN
		assertThat("Final ID returned", id, is(notNullValue()));
		assertThat("Returned kind matches", id.getKind(), is(meta.getKind()));
		assertThat("Returned stream ID matches", id.getStreamId(), is(meta.getStreamId()));
		assertThat("Returned object ID new value", id.getObjectId(), is(newLocId));
		assertThat("Returned source ID new value", id.getSourceId(), is(newSourceId));
	}

}
