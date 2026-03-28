/* ==================================================================
 * JdbcObjectDatumStreamAliasDaoTests.java - 28/03/2026 3:18:56 pm
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

import static java.util.Map.entry;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.dao.jdbc.test.DatumJdbcTestUtils.allObjectDatumStreamAliasData;
import static net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata.emptyMeta;
import static net.solarnetwork.central.domain.EntityConstants.isAssigned;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.BDDMockito.given;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.dao.jdbc.test.BaseDatumJdbcTestSupport;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.dao.jdbc.JdbcObjectDatumStreamAliasDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.domain.EntityConstants;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.util.UuidGenerator;

/**
 * Test cases for the {@link JdbcObjectDatumStreamAliasDao} class.
 *
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class JdbcObjectDatumStreamAliasDaoTests extends BaseDatumJdbcTestSupport {

	@Mock
	private UuidGenerator uuidGenerator;

	private JdbcObjectDatumStreamAliasDao dao;
	private Long userId;
	private Long locId;

	private ObjectDatumStreamAliasEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcObjectDatumStreamAliasDao(uuidGenerator, jdbcTemplate);
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
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomString());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(randomUUID(),
				now.minusSeconds(1), now, Node, randomLong(), randomString(), meta.getObjectId(),
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
		final var meta = emptyMeta(randomUUID(), TEST_TZ, Node, nodeId, randomString());
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, List.of(meta));

		final ObjectDatumStreamAliasEntity alias = new ObjectDatumStreamAliasEntity(
				EntityConstants.UNASSIGNED_UUID_ID, now.minusSeconds(1), now, Node, randomLong(),
				randomString(), meta.getObjectId(), meta.getSourceId());

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
				last.created(), modified, Node, randomLong(), randomString(), last.getOriginalObjectId(),
				last.getOriginalSourceId());

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

	// FIXME: more tests
}
