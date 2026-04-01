/* ==================================================================
 * DaoUserDatumStreamAliasBizTests.java - 30/03/2026 10:09:18 am
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

package net.solarnetwork.central.user.datum.stream.biz.test;

import static java.time.Instant.EPOCH;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType.AliasOnly;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.user.datum.stream.biz.impl.DaoUserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoUserDatumStreamAliasBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserDatumStreamAliasBizTests {

	@Mock
	private ObjectDatumStreamAliasEntityDao aliasDao;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasFilter> filterCaptor;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasEntity> aliasCaptor;

	private DaoUserDatumStreamAliasBiz service;

	@BeforeEach
	public void setup() {
		service = new DaoUserDatumStreamAliasBiz(aliasDao);
	}

	@Test
	public void aliasForId() {
		// GIVEN
		final Long userId = randomLong();
		final UUID aliasId = randomUUID();

		final var daoResult = new ObjectDatumStreamAliasEntity(aliasId, EPOCH, EPOCH, Node, randomLong(),
				randomSourceId(), randomLong(), randomSourceId());
		given(aliasDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(daoResult)));

		// WHEN
		final ObjectDatumStreamAliasEntity result = service.aliasForUser(userId, aliasId);

		// THEN
		// @formatter:off
		then(aliasDao).should().findFiltered(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("DAO queried")
			.isNotNull()
			.as("Alias-only search performed")
			.returns(AliasOnly, from(ObjectDatumStreamAliasFilter::getStreamAliasMatchType))
			.as("User ID from argument used as filter property")
			.returns(userId, from(ObjectDatumStreamAliasFilter::getUserId))
			.as("Alias ID from argument used as filter property")
			.returns(aliasId, from(ObjectDatumStreamAliasFilter::getStreamId))
			;
		
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void findFiltered() {
		// GIVEN
		final Long userId = randomLong();
		final UUID aliasId = randomUUID();

		final var daoResult = new BasicFilterResults<>(List.of(new ObjectDatumStreamAliasEntity(aliasId,
				EPOCH, EPOCH, Node, randomLong(), randomSourceId(), randomLong(), randomSourceId())));
		given(aliasDao.findFiltered(any())).willReturn(daoResult);

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.OriginalOnly);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomSourceId());
		final FilterResults<ObjectDatumStreamAliasEntity, UUID> result = service.listAliases(userId,
				filter);

		// THEN
		// @formatter:off
		then(aliasDao).should().findFiltered(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("DAO queried")
			.isNotNull()
			.as("Is not same filter intance as input argument")
			.isNotSameAs(filter)
			.as("Match type from argument filter used in search filter")
			.returns(filter.getStreamAliasMatchType(), from(ObjectDatumStreamAliasFilter::getStreamAliasMatchType))
			.as("User ID from argument used as filter property")
			.returns(userId, from(ObjectDatumStreamAliasFilter::getUserId))
			.as("Node IDs from argument filter used in search filter")
			.returns(filter.getNodeIds(), from(ObjectDatumStreamAliasFilter::getNodeIds))
			.as("Source IDs from argument filter used in search filter")
			.returns(filter.getSourceIds(), from(ObjectDatumStreamAliasFilter::getSourceIds))
			;
		
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void saveAlias() {
		// GIVEN
		final Long userId = randomLong();
		final UUID aliasId = randomUUID();
		final Long nodeId = randomLong();

		// persist alias
		final var daoResult = new ObjectDatumStreamAliasEntity(aliasId, EPOCH, EPOCH, Node, randomLong(),
				randomSourceId(), randomLong(), randomSourceId());
		given(aliasDao.save(any())).willReturn(aliasId);
		given(aliasDao.get(aliasId)).willReturn(daoResult);

		// WHEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());
		input.setOriginalObjectId(nodeId);
		input.setOriginalSourceId(randomSourceId());
		final var saveId = randomUUID();
		final ObjectDatumStreamAliasEntity result = service.saveAlias(userId, saveId, input);

		// THEN
		// @formatter:off
		then(aliasDao).should().save(aliasCaptor.capture());
		and.then(aliasCaptor.getValue())
			.as("Entity persisted to DAO")
			.isNotNull()
			.as("Alias ID argument passed to DAO")
			.returns(saveId, from(ObjectDatumStreamAliasEntity::getId))
			.as("Object ID from input argument passed to DAO")
			.returns(input.getObjectId(), from(ObjectDatumStreamAliasEntity::getObjectId))
			.as("Source ID from input argument passed to DAO")
			.returns(input.getSourceId(), from(ObjectDatumStreamAliasEntity::getSourceId))
			.as("Original object ID from input argument passed to DAO")
			.returns(input.getOriginalObjectId(), from(ObjectDatumStreamAliasEntity::getOriginalObjectId))
			.as("Original source ID from input argument passed to DAO")
			.returns(input.getOriginalSourceId(), from(ObjectDatumStreamAliasEntity::getOriginalSourceId))
			;
		
		and.then(result)
			.as("Result from DAO get() returned")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void deleteFiltered() {
		// GIVEN
		final Long userId = randomLong();

		given(aliasDao.delete(any(ObjectDatumStreamAliasFilter.class))).willReturn(1);

		// WHEN
		final var filter = new BasicDatumCriteria();
		filter.setStreamAliasMatchType(ObjectDatumStreamAliasMatchType.AliasOnly);
		filter.setNodeId(randomLong());
		filter.setSourceId(randomSourceId());
		service.deleteAliases(userId, filter);

		// THEN
		// @formatter:off
		then(aliasDao).should().delete(filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("DAO queried")
			.isNotNull()
			.as("Is not same filter intance as input argument")
			.isNotSameAs(filter)
			.as("Match type from argument filter used in search filter")
			.returns(filter.getStreamAliasMatchType(), from(ObjectDatumStreamAliasFilter::getStreamAliasMatchType))
			.as("User ID from argument used as filter property")
			.returns(userId, from(ObjectDatumStreamAliasFilter::getUserId))
			.as("Node IDs from argument filter used in search filter")
			.returns(filter.getNodeIds(), from(ObjectDatumStreamAliasFilter::getNodeIds))
			.as("Source IDs from argument filter used in search filter")
			.returns(filter.getSourceIds(), from(ObjectDatumStreamAliasFilter::getSourceIds))
			;
		// @formatter:on
	}

}
