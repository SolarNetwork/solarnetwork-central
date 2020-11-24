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
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toMap;
import static net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils.insertObjectDatumStreamMetadata;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumTestUtils.assertDatumStreamMetadat;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import javax.cache.Cache;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.BasicLocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.LocationDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link DatumStreamMetadataDao}.
 * 
 * @author matt
 * @version 1.0
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
	public void findNodeMetadata() {
		// GIVEN
		final List<NodeDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicNodeDatumStreamMetadata(streamId, "UTC", (long) i, format("s%d", i),
					new String[] { "a", "b", "c" }, new String[] { "d", "e" }, new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<NodeDatumStreamMetadata> results = dao.findNodeDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, NodeDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( NodeDatumStreamMetadata expected : data ) {
			NodeDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadat("location meta", meta, expected);
		}
	}

	@Test
	public void findLocationMetadata() {
		// GIVEN
		final List<LocationDatumStreamMetadata> data = new ArrayList<>(3);
		final Set<UUID> streamIds = new LinkedHashSet<>(3);
		for ( int i = 1; i <= 3; i++ ) {
			UUID streamId = UUID.randomUUID();
			streamIds.add(streamId);
			data.add(new BasicLocationDatumStreamMetadata(streamId, "UTC", (long) i, format("s%d", i),
					new String[] { "a", "b", "c" }, new String[] { "d", "e" }, new String[] { "f" }));

		}
		insertObjectDatumStreamMetadata(log, jdbcTemplate, data);

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<LocationDatumStreamMetadata> results = dao.findLocationDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, LocationDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(streamIds));

		for ( LocationDatumStreamMetadata expected : data ) {
			LocationDatumStreamMetadata meta = metas.get(expected.getStreamId());
			assertDatumStreamMetadat("location meta", meta, expected);
		}
	}

	@Test
	public void metadataForStream_notFound() {
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" }, new String[] { "d", "e" },
				new String[] { "f" });
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
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" }, new String[] { "d", "e" },
				new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadat("returned meta", result, meta);
	}

	@Test
	public void metadataForStream_cacheMiss() {
		// GIVEN
		dao.setStreamMetadataCache(cache);
		setupTestNode(); // for TZ
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" }, new String[] { "d", "e" },
				new String[] { "f" });
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
		assertDatumStreamMetadat("returned meta", result, meta);
		assertDatumStreamMetadat("cached meta", metaCaptor.getValue(), meta);
	}

	@Test
	public void metadataForStream_cacheHit() {
		// GIVEN
		dao.setStreamMetadataCache(cache);
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" }, new String[] { "d", "e" },
				new String[] { "f" });

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
		BasicLocationDatumStreamMetadata meta = new BasicLocationDatumStreamMetadata(UUID.randomUUID(),
				TEST_TZ, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		replayAll();
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertDatumStreamMetadat("returned meta", result, meta);
	}
}
