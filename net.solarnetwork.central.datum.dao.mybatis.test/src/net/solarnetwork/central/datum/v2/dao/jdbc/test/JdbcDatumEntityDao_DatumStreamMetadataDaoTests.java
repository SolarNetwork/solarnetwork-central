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
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for the {@link JdbcDatumEntityDao} class' implementation of
 * {@link DatumStreamMetadataDao}.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcDatumEntityDao_DatumStreamMetadataDaoTests extends BaseDatumJdbcTestSupport {

	private JdbcDatumEntityDao dao;

	@Before
	public void setup() {
		dao = new JdbcDatumEntityDao(jdbcTemplate);
	}

	@Test
	public void findNodeMetadata() {
		// GIVEN
		final List<UUID> streamIds = new ArrayList<>(3);
		jdbcTemplate.batchUpdate(
				"insert into solardatm.da_datm_meta (stream_id,node_id,source_id,names_i,names_a,names_s) "
						+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						UUID streamId = UUID.randomUUID();
						streamIds.add(streamId);
						ps.setString(1, streamId.toString());
						ps.setLong(2, i + 1);
						ps.setString(3, format("s%d", i + 1));
						ps.setString(4, "{a,b,c}");
						ps.setString(5, "{d,e}");
						ps.setString(6, "{f}");
					}

					@Override
					public int getBatchSize() {
						return 3;
					}
				});

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<NodeDatumStreamMetadata> results = dao.findNodeDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, NodeDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( int i = 0, idx = 1; i < 3; i++, idx++ ) {
			final UUID streamId = streamIds.get(i);
			NodeDatumStreamMetadata meta = metas.get(streamId);
			assertThat("Stream ID " + idx, meta.getStreamId(), equalTo(streamId));
			assertThat("Node ID " + idx, meta.getNodeId(), equalTo((long) idx));
			assertThat("Source ID" + idx, meta.getSourceId(), equalTo(format("s%d", idx)));
			assertThat("Instantaneous property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
					arrayContaining("a", "b", "c"));
			assertThat("Accumulating property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
					arrayContaining("d", "e"));
			assertThat("Status property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Status), arrayContaining("f"));
		}
	}

	@Test
	public void findLocationMetadata() {
		// GIVEN
		final List<UUID> streamIds = new ArrayList<>(3);
		jdbcTemplate.batchUpdate(
				"insert into solardatm.da_loc_datm_meta (stream_id,loc_id,source_id,names_i,names_a,names_s) "
						+ "VALUES (?::uuid,?,?,?::text[],?::text[],?::text[])",
				new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						UUID streamId = UUID.randomUUID();
						streamIds.add(streamId);
						ps.setString(1, streamId.toString());
						ps.setLong(2, i + 1);
						ps.setString(3, format("s%d", i + 1));
						ps.setString(4, "{a,b,c}");
						ps.setString(5, "{d,e}");
						ps.setString(6, "{f}");
					}

					@Override
					public int getBatchSize() {
						return 3;
					}
				});

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setLocationIds(new Long[] { 1L, 2L, 3L });
		filter.setSourceIds(new String[] { "s1", "s2", "s3" });
		Iterable<LocationDatumStreamMetadata> results = dao.findLocationDatumStreamMetadata(filter);

		assertThat("Results returned", results, notNullValue());
		Map<UUID, LocationDatumStreamMetadata> metas = StreamSupport.stream(results.spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		assertThat("Stream IDs same", metas.keySet(), equalTo(new LinkedHashSet<>(streamIds)));

		for ( int i = 0, idx = 1; i < 3; i++, idx++ ) {
			final UUID streamId = streamIds.get(i);
			LocationDatumStreamMetadata meta = metas.get(streamId);
			assertThat("Stream ID " + idx, meta.getStreamId(), equalTo(streamId));
			assertThat("Location ID " + idx, meta.getLocationId(), equalTo((long) idx));
			assertThat("Source ID" + idx, meta.getSourceId(), equalTo(format("s%d", idx)));
			assertThat("Instantaneous property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
					arrayContaining("a", "b", "c"));
			assertThat("Accumulating property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
					arrayContaining("d", "e"));
			assertThat("Status property names " + idx,
					meta.propertyNamesForType(GeneralDatumSamplesType.Status), arrayContaining("f"));
		}
	}

	@Test
	public void metadataForStream_notFound() {
		BasicNodeDatumStreamMetadata meta = new BasicNodeDatumStreamMetadata(UUID.randomUUID(), TEST_TZ,
				TEST_NODE_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" }, new String[] { "d", "e" },
				new String[] { "f" });
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
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
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertThat("Metadata found", result, notNullValue());
		assertThat("Node metadata returned", result, instanceOf(NodeDatumStreamMetadata.class));
		assertThat("Stream ID", result.getStreamId(), equalTo(meta.getStreamId()));
		assertThat("Time zone ID", result.getTimeZoneId(), equalTo(meta.getTimeZoneId()));
		assertThat("Object ID", result.getObjectId(), equalTo(meta.getNodeId()));
		assertThat("Source ID", result.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Instantaneous property names",
				meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining("a", "b", "c"));
		assertThat("Accumulating property names",
				meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining("d", "e"));
		assertThat("Status property names", meta.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining("f"));
	}

	@Test
	public void metadataForStream_location() {
		// GIVEN
		setupTestLocation(); // for TZ
		BasicLocationDatumStreamMetadata meta = new BasicLocationDatumStreamMetadata(UUID.randomUUID(),
				TEST_TZ, TEST_LOC_ID, TEST_SOURCE_ID, new String[] { "a", "b", "c" },
				new String[] { "d", "e" }, new String[] { "f" });
		DatumTestUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, singleton(meta));

		// WHEN
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(meta.getStreamId());
		ObjectDatumStreamMetadata result = dao.findStreamMetadata(filter);

		// THEN
		assertThat("Metadata found", result, notNullValue());
		assertThat("Location metadata returned", result, instanceOf(LocationDatumStreamMetadata.class));
		assertThat("Stream ID", result.getStreamId(), equalTo(meta.getStreamId()));
		assertThat("Time zone ID", result.getTimeZoneId(), equalTo(meta.getTimeZoneId()));
		assertThat("Object ID", result.getObjectId(), equalTo(meta.getLocationId()));
		assertThat("Source ID", result.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Instantaneous property names",
				meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining("a", "b", "c"));
		assertThat("Accumulating property names",
				meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining("d", "e"));
		assertThat("Status property names", meta.propertyNamesForType(GeneralDatumSamplesType.Status),
				arrayContaining("f"));
	}
}
