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
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import java.util.Arrays;
import java.util.Collections;
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
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantEntityDao;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.biz.impl.DaoUserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link DaoUserDatumStreamAliasBiz} class.
 * 
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoUserDatumStreamAliasBizTests {

	@Mock
	private ObjectDatumStreamAliasEntityDao aliasDao;

	@Mock
	private ObjectDatumStreamAliasGrantEntityDao grantDao;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasFilter> filterCaptor;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasEntity> aliasCaptor;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasGrantFilter> grantFilterCaptor;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasGrantEntity> grantCaptor;

	private DaoUserDatumStreamAliasBiz service;

	@BeforeEach
	public void setup() {
		service = new DaoUserDatumStreamAliasBiz(aliasDao, grantDao);
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
		final var saveId = UNASSIGNED_UUID_ID;
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
	public void saveAlias_update() {
		// GIVEN
		final Long userId = randomLong();
		final UUID aliasId = randomUUID();

		final var existing = new ObjectDatumStreamAliasEntity(aliasId, EPOCH, EPOCH, Node, randomLong(),
				randomSourceId(), randomLong(), randomSourceId());
		given(aliasDao.get(aliasId)).willReturn(existing);
		given(aliasDao.save(any())).willReturn(aliasId);

		// WHEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		final ObjectDatumStreamAliasEntity result = service.saveAlias(userId, aliasId, input);

		// THEN
		// @formatter:off
		then(aliasDao).should().save(aliasCaptor.capture());
		and.then(aliasCaptor.getValue())
			.as("Existing alias ID passed to DAO")
			.returns(aliasId, from(ObjectDatumStreamAliasEntity::getId))
			;
		and.then(result)
			.as("Result from DAO get() returned")
			.isSameAs(existing)
			;
		// @formatter:on
	}

	@Test
	public void saveAlias_assignedIdNotFound() {
		// GIVEN
		final Long userId = randomLong();
		final UUID aliasId = randomUUID();

		given(aliasDao.get(aliasId)).willReturn(null);

		// WHEN
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());

		// @formatter:off
		and.thenThrownBy(() -> service.saveAlias(userId, aliasId, input))
			.as("Saving with an ID that is not an existing alias is not allowed")
			.isInstanceOfSatisfying(AuthorizationException.class, e -> {
				and.then(e.getReason())
					.as("Unknown object reason given")
					.isEqualTo(AuthorizationException.Reason.UNKNOWN_OBJECT)
					;
			})
			;

		// THEN
		then(aliasDao).should(never()).save(any());
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

	private static ObjectDatumStreamAliasGrantEntity grant(UserLongCompositePK pk) {
		return new ObjectDatumStreamAliasGrantEntity(pk, EPOCH, randomLong());
	}

	@Test
	public void aliasGrantForUser() {
		// GIVEN
		final var pk = new UserLongCompositePK(randomLong(), randomLong());
		final var daoResult = grant(pk);
		given(grantDao.get(pk)).willReturn(daoResult);

		// WHEN
		final ObjectDatumStreamAliasGrantEntity result = service.aliasGrantForUser(pk.getUserId(),
				pk.getEntityId());

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void aliasGrantForUser_notFound() {
		// GIVEN
		final var pk = new UserLongCompositePK(randomLong(), randomLong());
		given(grantDao.get(pk)).willReturn(null);

		// WHEN
		// @formatter:off
		and.thenExceptionOfType(AuthorizationException.class)
			.as("Grant owned by another user is not available")
			.isThrownBy(() -> service.aliasGrantForUser(pk.getUserId(), pk.getEntityId()))
			;
		// @formatter:on
	}

	@Test
	public void listAliasGrants() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGranteeId(randomLong());

		final FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> daoResult = new BasicFilterResults<>(
				List.of());
		given(grantDao.findFiltered(any())).willReturn(daoResult);

		// WHEN
		final var result = service.listAliasGrants(userId, filter);

		// THEN
		// @formatter:off
		then(grantDao).should().findFiltered(grantFilterCaptor.capture());
		and.then(grantFilterCaptor.getValue())
			.as("User ID from argument replaces any filter user ID")
			.returns(userId, from(ObjectDatumStreamAliasGrantFilter::getUserId))
			.as("Filter criteria copied")
			.returns(filter.getGranteeId(), from(ObjectDatumStreamAliasGrantFilter::getGranteeId))
			;
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void saveAliasGrant_create() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new ObjectDatumStreamAliasGrantEntityInput();
		input.setGranteeId(randomLong());
		input.setNodeIds(new Long[] { randomLong() });
		input.setSourceIds(List.of(randomSourceId()));

		final var pk = new UserLongCompositePK(userId, randomLong());
		final var saved = grant(pk);
		given(grantDao.save(any())).willReturn(pk);
		given(grantDao.get(pk)).willReturn(saved);

		// WHEN
		final ObjectDatumStreamAliasGrantEntity result = service.saveAliasGrant(userId,
				UNASSIGNED_LONG_ID, input);

		// THEN
		// @formatter:off
		then(grantDao).should().save(grantCaptor.capture());
		and.then(grantCaptor.getValue())
			.as("Grant owned by argument user")
			.returns(userId, from(ObjectDatumStreamAliasGrantEntity::getUserId))
			.as("Grant ID not assigned")
			.returns(UNASSIGNED_LONG_ID, from(ObjectDatumStreamAliasGrantEntity::getGrantId))
			.as("Grantee from input")
			.returns(input.getGranteeId(), from(ObjectDatumStreamAliasGrantEntity::getGranteeId))
			.as("Nodes from input")
			.returns(input.getNodeIds(), from(ObjectDatumStreamAliasGrantEntity::getNodeIds))
			.as("Sources from input")
			.returns(input.getSourceIds().toArray(String[]::new), from(ObjectDatumStreamAliasGrantEntity::getSourceIds))
			;
		and.then(result)
			.as("Saved grant provided")
			.isSameAs(saved)
			;
		// @formatter:on
	}

	@Test
	public void saveAliasGrant_update_notFound() {
		// GIVEN
		final var pk = new UserLongCompositePK(randomLong(), randomLong());
		final var input = new ObjectDatumStreamAliasGrantEntityInput();
		input.setGranteeId(randomLong());
		input.setNodeIds(new Long[] { randomLong() });
		given(grantDao.get(pk)).willReturn(null);

		// WHEN
		// @formatter:off
		and.thenExceptionOfType(AuthorizationException.class)
			.as("Cannot assign the ID of a grant that does not exist, or is owned by another user")
			.isThrownBy(() -> service.saveAliasGrant(pk.getUserId(), pk.getEntityId(), input))
			;

		// THEN
		then(grantDao).should(never()).save(any());
		// @formatter:on
	}

	@Test
	public void updateAliasGrantEnabled() {
		// GIVEN
		final var pk = new UserLongCompositePK(randomLong(), randomLong());
		final var updated = grant(pk);
		given(grantDao.get(pk)).willReturn(updated);

		// WHEN
		final ObjectDatumStreamAliasGrantEntity result = service.updateAliasGrantEnabled(pk.getUserId(),
				pk.getEntityId(), true);

		// THEN
		// @formatter:off
		then(grantDao).should().updateEnabledStatus(eq(pk.getUserId()), grantFilterCaptor.capture(),
				eq(true));
		and.then(grantFilterCaptor.getValue())
			.as("Update limited to given grant")
			.returns(pk.getEntityId(), from(ObjectDatumStreamAliasGrantFilter::getGrantId))
			;
		and.then(result)
			.as("Updated grant provided")
			.isSameAs(updated)
			;
		// @formatter:on
	}

	@Test
	public void deleteAliasGrant() {
		// GIVEN
		final var pk = new UserLongCompositePK(randomLong(), randomLong());
		final var key = grant(pk);
		given(grantDao.entityKey(pk)).willReturn(key);

		// WHEN
		service.deleteAliasGrant(pk.getUserId(), pk.getEntityId());

		// THEN
		then(grantDao).should().delete(key);
	}

	private static ObjectDatumStreamAliasEntityInput aliasInput() {
		final var input = new ObjectDatumStreamAliasEntityInput();
		input.setObjectId(randomLong());
		input.setSourceId(randomSourceId());
		input.setOriginalObjectId(randomLong());
		input.setOriginalSourceId(randomSourceId());
		return input;
	}

	@Test
	public void saveAliases() {
		// GIVEN
		final Long userId = randomLong();
		final var input1 = aliasInput();
		final var input2 = aliasInput();

		final UUID id1 = randomUUID();
		final UUID id2 = randomUUID();
		final var saved1 = input1.toEntity(id1, EPOCH);
		final var saved2 = input2.toEntity(id2, EPOCH);
		given(aliasDao.save(any())).willReturn(id1, id2);
		given(aliasDao.get(id1)).willReturn(saved1);
		given(aliasDao.get(id2)).willReturn(saved2);

		// WHEN
		final List<ObjectDatumStreamAliasEntity> result = service.saveAliases(userId,
				List.of(input1, input2));

		// THEN
		// @formatter:off
		then(aliasDao).should(times(2)).save(aliasCaptor.capture());
		and.then(aliasCaptor.getAllValues())
			.as("New aliases created, with no ID assigned")
			.extracting(ObjectDatumStreamAliasEntity::getId)
			.containsExactly(UNASSIGNED_UUID_ID, UNASSIGNED_UUID_ID)
			;
		and.then(result)
			.as("Saved aliases returned in input order")
			.containsExactly(saved1, saved2)
			;
		// @formatter:on
	}

	@Test
	public void saveAliases_tooMany() {
		// GIVEN
		final var inputs = Collections.nCopies(UserDatumStreamAliasBiz.MAX_BULK_ALIASES + 1,
				aliasInput());

		// WHEN
		// @formatter:off
		and.thenIllegalArgumentException()
			.isThrownBy(() -> service.saveAliases(randomLong(), inputs))
			;

		// THEN
		then(aliasDao).should(never()).save(any());
		// @formatter:on
	}

	@Test
	public void saveAliases_nullInput() {
		// GIVEN
		final var inputs = Arrays.asList(aliasInput(), null);

		// WHEN
		// @formatter:off
		and.thenIllegalArgumentException()
			.isThrownBy(() -> service.saveAliases(randomLong(), inputs))
			;

		// THEN
		then(aliasDao).should(never()).save(any());
		// @formatter:on
	}

	@Test
	public void listReceivedAliasGrants() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();
		filter.setUserId(randomLong());
		filter.setGranteeId(randomLong());
		filter.setEnabled(false);

		final FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> daoResult = new BasicFilterResults<>(
				List.of());
		given(grantDao.findFiltered(any())).willReturn(daoResult);

		// WHEN
		final var result = service.listReceivedAliasGrants(userId, filter);

		// THEN
		// @formatter:off
		then(grantDao).should().findFiltered(grantFilterCaptor.capture());
		and.then(grantFilterCaptor.getValue())
			.as("Grantee is always the user from the argument")
			.returns(userId, from(ObjectDatumStreamAliasGrantFilter::getGranteeId))
			.as("Disabled grants never included")
			.returns(true, from(ObjectDatumStreamAliasGrantFilter::getEnabled))
			.as("Owner criteria kept")
			.returns(filter.getUserId(), from(ObjectDatumStreamAliasGrantFilter::getUserId))
			;
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

	@Test
	public void listReceivedAliasGrants_noFilter() {
		// GIVEN
		final Long userId = randomLong();
		given(grantDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of()));

		// WHEN
		service.listReceivedAliasGrants(userId, null);

		// THEN
		// @formatter:off
		then(grantDao).should().findFiltered(grantFilterCaptor.capture());
		and.then(grantFilterCaptor.getValue())
			.as("Grantee is the user from the argument")
			.returns(userId, from(ObjectDatumStreamAliasGrantFilter::getGranteeId))
			.as("No owner criteria")
			.returns(null, from(ObjectDatumStreamAliasGrantFilter::getUserId))
			;
		// @formatter:on
	}

	@Test
	public void listAvailableAliasStreams() {
		// GIVEN
		final Long userId = randomLong();
		final Long aliasNodeId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		final FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> daoResult = new BasicFilterResults<>(
				List.of());
		given(grantDao.findAvailableStreams(userId, aliasNodeId, filter)).willReturn(daoResult);

		// WHEN
		final var result = service.listAvailableAliasStreams(userId, aliasNodeId, filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result from DAO provided")
			.isSameAs(daoResult)
			;
		// @formatter:on
	}

}
