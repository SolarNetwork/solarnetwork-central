/* ==================================================================
 * LazyDatumMetadataOperationsTests.java - 13/03/2025 10:26:21 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.support.LazyDatumMetadataOperations;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Test cases for the {@link LazyDatumMetadataOperations} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class LazyDatumMetadataOperationsTests {

	@Mock
	private DatumStreamMetadataDao dao;

	@Mock
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> cache;

	@Captor
	private ArgumentCaptor<ObjectStreamCriteria> filterCaptor;

	@Captor
	private ArgumentCaptor<GeneralDatumMetadata> metaCaptor;

	@Test
	public void access_resolveFromDao() {
		// GIVEN
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var id = new ObjectDatumStreamMetadataId(Node, nodeId, sourceId);
		final var foo = randomLong();
		final var metaJson = """
				{"m":{"foo":%d}}
				""".formatted(foo);

		final var daoMeta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(), "UTC", id.getKind(),
				id.getObjectId(), id.getSourceId(), null, null, null, metaJson);
		given(dao.findDatumStreamMetadata(any())).willReturn(List.of(daoMeta));

		// WHEN
		var ops = new LazyDatumMetadataOperations(id, dao, cache);
		var result = ops.getInfoLong("foo");

		// THEN
		// @formatter:off
		then(cache).should().get(same(id));

		then(dao).should().findDatumStreamMetadata(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("Search DAO with object kind given in constructor")
			.returns(id.getKind(), from(ObjectStreamCriteria::getObjectKind))
			.as("Search DAO with object ID given in constructor")
			.returns(nodeId, from(ObjectStreamCriteria::getNodeId))
			.as("Search DAO with source ID given in constructor")
			.returns(sourceId, from(ObjectStreamCriteria::getSourceId))
			;

		then(cache).should().put(same(id), metaCaptor.capture());
		and.then(metaCaptor.getValue())
			.as("Cached metadata parsed from meta JSON from DAO")
			.isEqualTo(new GeneralDatumMetadata(Map.of("foo", foo)))
			;

		and.then(result).as("Resolved from DAO").isEqualTo(foo);
		// @formatter:on
	}

	@Test
	public void access_resolveFromCache() {
		// GIVEN
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var id = new ObjectDatumStreamMetadataId(Node, nodeId, sourceId);
		final var foo = randomLong();

		final var cachedMeta = new GeneralDatumMetadata(Map.of("foo", foo));
		given(cache.get(id)).willReturn(cachedMeta);

		// WHEN
		var ops = new LazyDatumMetadataOperations(id, dao, cache);
		var result = ops.getInfoLong("foo");

		// THEN
		// @formatter:off
		then(cache).shouldHaveNoMoreInteractions();

		then(dao).shouldHaveNoInteractions();

		and.then(result).as("Resolved from cache").isEqualTo(foo);
		// @formatter:on
	}

	@Test
	public void access_resolveNone() {
		// GIVEN
		final var nodeId = randomLong();
		final var sourceId = randomString();
		final var id = new ObjectDatumStreamMetadataId(Node, nodeId, sourceId);

		// WHEN
		var ops = new LazyDatumMetadataOperations(id, dao, cache);
		var result = ops.getInfoLong("foo");

		// THEN
		// @formatter:off
		then(cache).should().get(same(id));
		then(cache).shouldHaveNoMoreInteractions();

		then(dao).should().findDatumStreamMetadata(filterCaptor.capture());
		then(dao).shouldHaveNoMoreInteractions();
		and.then(filterCaptor.getValue())
			.as("Search DAO with object kind given in constructor")
			.returns(id.getKind(), from(ObjectStreamCriteria::getObjectKind))
			.as("Search DAO with object ID given in constructor")
			.returns(nodeId, from(ObjectStreamCriteria::getNodeId))
			.as("Search DAO with source ID given in constructor")
			.returns(sourceId, from(ObjectStreamCriteria::getSourceId))
			;

		and.then(result).as("Resolved nothing").isNull();
		// @formatter:on
	}

}
