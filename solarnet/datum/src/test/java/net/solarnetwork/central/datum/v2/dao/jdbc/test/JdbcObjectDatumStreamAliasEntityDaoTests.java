/* ==================================================================
 * JdbcObjectDatumStreamAliasEntityDaoTests.java - 28/03/2026 3:18:56 pm
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

import static java.util.Comparator.comparing;
import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumJdbcTestUtils.allObjectDatumStreamAliasData;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.domain.EntityConstants.isAssigned;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.BDDMockito.given;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcDatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.util.UuidGenerator;

/**
 * Test cases for the {@link JdbcObjectDatumStreamAliasEntityDao} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class JdbcObjectDatumStreamAliasEntityDaoTests extends BaseDatumJdbcTestSupport {

	@Mock
	private UuidGenerator uuidGenerator;

	private JdbcDatumEntityDao datumDao;

	private JdbcObjectDatumStreamAliasEntityDao dao;
	private Long userId;
	private Long locId;

	private ObjectDatumStreamAliasEntity last;

	@BeforeEach
	public void setup() {
		datumDao = new JdbcDatumEntityDao(jdbcTemplate);

		dao = new JdbcObjectDatumStreamAliasEntityDao(uuidGenerator, jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		locId = randomLong();
		setupTestLocation(locId);
	}

	@Test
	public void entityKey() {
		final UUID id = UUID.randomUUID();
		ObjectDatumStreamAliasEntity result = dao.entityKey(id);

		// @formatter:off
		then(result)
			.as("Entity for key returned")
			.isNotNull()
			.as("ID of entity from provided value")
			.returns(id, Entity::getId)
			.as("Object ID is not assigned")
			.returns(false, e -> isAssigned(e.getObjectId()))
			.as("Source ID is not assigned")
			.returns(false, e -> isAssigned(e.getSourceId()))
			.as("Original object ID is not assigned")
			.returns(false, e -> isAssigned(e.getOriginalObjectId()))
			.as("Original source ID is not assigned")
			.returns(false, e -> isAssigned(e.getOriginalSourceId()))
			;
		// @formatter:on
	}

	@Test
	public void insert() {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestNode(nodeId, locId);
		setupUserNodeEntity(nodeId, userId);

		final Instant now = Instant.now();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(randomUUID(),
				now.minusSeconds(1), now, Node, randomLong(), randomSourceId(), meta.getObjectId(),
				meta.getSourceId());

		// WHEN
		final UUID result = dao.save(alias);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned primary key is same as given ID")
			.isEqualTo(alias.id())
			;

		List<Map<String, Object>> data = allObjectDatumStreamAliasData(jdbcTemplate);
		then(data)
			.as("Table has 1 row")
			.hasSize(1)
			.asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.containsOnly(
				entry("stream_id", alias.id().toString()),
				entry("created", Timestamp.from(alias.getCreated())),
				entry("modified", Timestamp.from(alias.getModified())),
				entry("node_id", alias.getOriginalObjectId()),
				entry("source_id", alias.getOriginalSourceId()),
				entry("alias_node_id", alias.getObjectId()),
				entry("alias_source_id", alias.getSourceId())
			)
			;
		// @formatter:on
		last = alias;
	}

	@Test
	public void insert_generatePk() {
		// GIVEN
		final Long nodeId = randomLong();
		setupTestNode(nodeId, locId);
		setupUserNodeEntity(nodeId, userId);

		final Instant now = Instant.now();
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomSourceId());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(
				EntityConstants.UNASSIGNED_UUID_ID, now.minusSeconds(1), now, Node, randomLong(),
				randomSourceId(), meta.getObjectId(), meta.getSourceId());

		final UUID generatedPk = randomUUID();
		given(uuidGenerator.generate()).willReturn(generatedPk);

		// WHEN
		final UUID result = dao.save(alias);

		// THEN
		// @formatter:off
		then(result)
			.as("Returned primary key is generated")
			.isSameAs(generatedPk)
			;

		final List<Map<String, Object>> data = allObjectDatumStreamAliasData(jdbcTemplate);
		then(data)
			.as("Table has 1 row")
			.hasSize(1)
			.asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.containsOnly(
				entry("stream_id", generatedPk.toString()),
				entry("created", Timestamp.from(alias.getCreated())),
				entry("modified", Timestamp.from(alias.getModified())),
				entry("node_id", alias.getOriginalObjectId()),
				entry("source_id", alias.getOriginalSourceId()),
				entry("alias_node_id", alias.getObjectId()),
				entry("alias_source_id", alias.getSourceId())
			)
			;
		// @formatter:on
		last = alias.copyWithId(generatedPk);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		final ObjectDatumStreamAliasEntity result = dao.get(last.id());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
		then(result.isSameAs(last)).as("Retrieved properties match source").isTrue();
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		final Instant modified = Instant.now().plusSeconds(1);
		final ObjectDatumStreamAliasEntity entity = new ObjectDatumStreamAliasEntity(last.id(),
				last.created(), modified, Node, randomLong(), randomSourceId(),
				last.getOriginalObjectId(), last.getOriginalSourceId());

		final UUID result = dao.save(entity);
		final ObjectDatumStreamAliasEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allObjectDatumStreamAliasData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(entity)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(entity));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allObjectDatumStreamAliasData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	private SortedMap<Long, List<ObjectDatumStreamAliasEntity>> setupRandomAliases() {
		final int userCount = 3;
		final int nodeCount = 3;
		final int sourceCount = 3;
		final int aliasCount = 3;
		final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		final SortedMap<Long, List<ObjectDatumStreamAliasEntity>> entitiesByUser = new TreeMap<>();

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int n = 0; n < nodeCount; n++ ) {
				final Long nodeId = randomInt().longValue();
				setupTestNode(nodeId, locId);
				setupUserNodeEntity(nodeId, userId);
				for ( int s = 0; s < sourceCount; s++ ) {
					final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomSourceId());
					DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));
					for ( int a = 0; a < aliasCount; a++ ) {
						final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(
								randomUUID(), now, now, Node, randomInt().longValue(), randomSourceId(),
								meta.getObjectId(), meta.getSourceId());
						dao.save(alias);
						entitiesByUser
								.computeIfAbsent(userId,
										_ -> new ArrayList<>(aliasCount * nodeCount * sourceCount))
								.add(alias);
					}
				}
			}
		}
		allObjectDatumStreamAliasData(jdbcTemplate);
		return entitiesByUser;
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final SortedMap<Long, List<ObjectDatumStreamAliasEntity>> entitiesByUser = setupRandomAliases();
		final Long randomUserId = List.copyOf(entitiesByUser.keySet())
				.get(RNG.nextInt(entitiesByUser.size()));

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setUserId(randomUserId);
		FilterResults<ObjectDatumStreamAliasEntity, UUID> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		ObjectDatumStreamAliasEntity[] expected = entitiesByUser.get(randomUserId)
				.stream()
				.sorted(comparing(ObjectDatumStreamAliasEntity::getOriginalObjectId)
					.thenComparing(ObjectDatumStreamAliasEntity::getOriginalSourceId)
					.thenComparing(ObjectDatumStreamAliasEntity::getObjectId)
					.thenComparing(ObjectDatumStreamAliasEntity::getSourceId)
				)
				.toArray(ObjectDatumStreamAliasEntity[]::new);
		then(results).as("Results for single user sorted in default order").containsExactly(expected);
		// @formatter:on
	}

	@Test
	public void findFiltered_forNodesAndSources() throws Exception {
		// GIVEN
		final SortedMap<Long, List<ObjectDatumStreamAliasEntity>> entitiesByUser = setupRandomAliases();
		final Long randomUserId = List.copyOf(entitiesByUser.keySet())
				.get(RNG.nextInt(entitiesByUser.size()));
		// @formatter:off
		final List<ObjectDatumStreamAliasEntity> userEntities = entitiesByUser.get(randomUserId)
				.stream()
				.sorted(comparing(ObjectDatumStreamAliasEntity::getOriginalObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getOriginalSourceId)
				.thenComparing(ObjectDatumStreamAliasEntity::getObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getSourceId)
			).toList();
		// @formatter:on
		final ObjectDatumStreamAliasEntity randomAlias1 = userEntities
				.get(RNG.nextInt(userEntities.size()));
		final ObjectDatumStreamAliasEntity randomAlias2 = userEntities
				.get(RNG.nextInt(userEntities.size()));

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setUserId(randomUserId);
		filter.setNodeIds(new Long[] { randomAlias1.getOriginalObjectId(), randomAlias2.getObjectId() });
		filter.setSourceIds(
				new String[] { randomAlias1.getOriginalSourceId(), randomAlias2.getSourceId() });
		FilterResults<ObjectDatumStreamAliasEntity, UUID> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		ObjectDatumStreamAliasEntity[] expected = userEntities.stream()
				.filter(a -> (a.getOriginalObjectId().equals(randomAlias1.getOriginalObjectId())
						|| a.getObjectId().equals(randomAlias2.getObjectId()))
						&& (a.getOriginalSourceId().equals(randomAlias1.getOriginalSourceId())
						|| a.getSourceId().equals(randomAlias2.getSourceId())))
				.toArray(ObjectDatumStreamAliasEntity[]::new);
		then(results)
			.as("Results for single user and nodes and sources sorted in default order")
			.containsExactly(expected)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forNodesAndSources_aliasOnly() throws Exception {
		// GIVEN
		final SortedMap<Long, List<ObjectDatumStreamAliasEntity>> entitiesByUser = setupRandomAliases();
		final Long randomUserId = List.copyOf(entitiesByUser.keySet())
				.get(RNG.nextInt(entitiesByUser.size()));
		// @formatter:off
		final List<ObjectDatumStreamAliasEntity> userEntities = entitiesByUser.get(randomUserId)
				.stream()
				.sorted(comparing(ObjectDatumStreamAliasEntity::getOriginalObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getOriginalSourceId)
				.thenComparing(ObjectDatumStreamAliasEntity::getObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getSourceId)
			).toList();
		// @formatter:on
		final ObjectDatumStreamAliasEntity randomAlias1 = userEntities
				.get(RNG.nextInt(userEntities.size()));
		final ObjectDatumStreamAliasEntity randomAlias2 = userEntities
				.get(RNG.nextInt(userEntities.size()));

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.AliasOnly);
		filter.setUserId(randomUserId);
		filter.setNodeIds(new Long[] { randomAlias1.getOriginalObjectId(), randomAlias2.getObjectId() });
		filter.setSourceIds(
				new String[] { randomAlias1.getOriginalSourceId(), randomAlias2.getSourceId() });
		FilterResults<ObjectDatumStreamAliasEntity, UUID> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		ObjectDatumStreamAliasEntity[] expected = userEntities.stream()
				.filter(a -> a.getObjectId().equals(randomAlias2.getObjectId())
						&& a.getSourceId().equals(randomAlias2.getSourceId()))
				.toArray(ObjectDatumStreamAliasEntity[]::new);
		then(results)
			.as("Results for single user and alias nodes and sources sorted in default order")
			.containsExactly(expected)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forNodesAndSources_originalOnly() throws Exception {
		// GIVEN
		final SortedMap<Long, List<ObjectDatumStreamAliasEntity>> entitiesByUser = setupRandomAliases();
		final Long randomUserId = List.copyOf(entitiesByUser.keySet())
				.get(RNG.nextInt(entitiesByUser.size()));
		// @formatter:off
		final List<ObjectDatumStreamAliasEntity> userEntities = entitiesByUser.get(randomUserId)
				.stream()
				.sorted(comparing(ObjectDatumStreamAliasEntity::getOriginalObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getOriginalSourceId)
				.thenComparing(ObjectDatumStreamAliasEntity::getObjectId)
				.thenComparing(ObjectDatumStreamAliasEntity::getSourceId)
			).toList();
		// @formatter:on
		final ObjectDatumStreamAliasEntity randomAlias1 = userEntities
				.get(RNG.nextInt(userEntities.size()));
		final ObjectDatumStreamAliasEntity randomAlias2 = userEntities
				.get(RNG.nextInt(userEntities.size()));

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.OriginalOnly);
		filter.setUserId(randomUserId);
		filter.setNodeIds(new Long[] { randomAlias1.getOriginalObjectId(), randomAlias2.getObjectId() });
		filter.setSourceIds(
				new String[] { randomAlias1.getOriginalSourceId(), randomAlias2.getSourceId() });
		FilterResults<ObjectDatumStreamAliasEntity, UUID> results = dao.findFiltered(filter);

		// THEN
		// @formatter:off
		ObjectDatumStreamAliasEntity[] expected = userEntities.stream()
				.filter(a -> a.getOriginalObjectId().equals(randomAlias1.getOriginalObjectId())
						&& a.getOriginalSourceId().equals(randomAlias1.getOriginalSourceId()))
				.toArray(ObjectDatumStreamAliasEntity[]::new);
		then(results)
			.as("Results for single user and original nodes and sources sorted in default order")
			.containsExactly(expected)
			;
		// @formatter:on
	}

	@Test
	public void updateOriginalNodeSourceUpdatesAlias() {
		// GIVEN
		insert();

		final ObjectDatumStreamAliasEntity origAlias = this.last;

		final var metaFilter = new BasicDatumCriteria();
		metaFilter.setObjectKind(Node);
		metaFilter.setNodeId(origAlias.getOriginalObjectId());
		metaFilter.setSourceId(origAlias.getOriginalSourceId());
		final ObjectDatumStreamMetadataId origStreamId = datumDao.findDatumStreamMetadataIds(metaFilter)
				.iterator().next();

		final Long newNodeId = randomInt().longValue();
		final String newSourceId = randomSourceId();

		// WHEN
		// update the original stream node/source details: then alias should be updated also in the DB
		datumDao.updateAttributes(origAlias.getKind(), origStreamId.getStreamId(), newNodeId,
				newSourceId, null, null, null);

		// now reload alias
		final ObjectDatumStreamAliasEntity updatedAlias = dao.get(origAlias.id());

		// THEN
		// @formatter:off
		then(updatedAlias)
			.isNotNull()
			.as("Alias stream ID unchanged")
			.isEqualTo(origAlias)
			.as("Orig stream attributes have been updated")
			.returns(true, a -> a.isSameAs(new ObjectDatumStreamAliasEntity(
					origAlias.id(),
					origAlias.created(),
					null,
					origAlias.getKind(),
					origAlias.getObjectId(),
					origAlias.getSourceId(),
					newNodeId,
					newSourceId))
			)
			;
		// @formatter:on
	}

	@Test
	public void deleteOriginalNodeSourceDeletesAlias() {
		// GIVEN
		insert();

		// WHEN
		// delete original stream meta
		final int deleteCount = jdbcTemplate.update("""
				DELETE FROM solardatm.da_datm_meta
				WHERE node_id = ? AND source_id = ?
				""", last.getOriginalObjectId(), last.getOriginalSourceId());

		// THEN
		// @formatter:off
		then(deleteCount)
			.as("Deleted original stream meta row")
			.isOne()
			;

		List<Map<String, Object>> data = allObjectDatumStreamAliasData(jdbcTemplate);
		then(data)
			.as("Alias row cascade deleted from DB")
			.isEmpty()
			;
		// @formatter:on

	}

}
