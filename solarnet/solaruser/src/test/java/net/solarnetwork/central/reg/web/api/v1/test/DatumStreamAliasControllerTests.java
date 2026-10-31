/* ==================================================================
 * DatumStreamAliasControllerTests.java - 1/04/2026 4:06:09 pm
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

package net.solarnetwork.central.reg.web.api.v1.test;

import static java.time.Instant.EPOCH;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasMatchType.AliasOnly;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_LONG_ID;
import static net.solarnetwork.central.domain.EntityConstants.UNASSIGNED_UUID_ID;
import static net.solarnetwork.central.security.SecurityUtils.becomeUser;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomSourceId;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.ObjectDatumKind.Node;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.web.context.request.RequestContextHolder.setRequestAttributes;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.BasicObjectDatumStreamAliasGrantFilter;
import net.solarnetwork.central.datum.v2.dao.ObjectDatumStreamAliasFilter;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantEntity;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamAliasGrantStream;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.reg.web.api.v1.DatumStreamAliasController;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasEntityInput;
import net.solarnetwork.central.user.datum.stream.domain.ObjectDatumStreamAliasGrantEntityInput;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Test cases for the {@link DatumStreamAliasController} class.
 *
 * @author matt
 * @version 1.1
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DatumStreamAliasControllerTests {

	@Mock
	private UserDatumStreamAliasBiz userAliasBiz;

	@Mock
	private HttpServletRequest request;

	@Captor
	private ArgumentCaptor<ObjectDatumStreamAliasFilter> filterCaptor;

	private DatumStreamAliasController controller;

	@BeforeEach
	public void setup() {
		controller = new DatumStreamAliasController(userAliasBiz);
	}

	@AfterEach
	public void teardown() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void listAliases() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicDatumCriteria();

		final var bizResult = new BasicFilterResults<ObjectDatumStreamAliasEntity, UUID>(List.of());
		given(userAliasBiz.listAliases(eq(userId), same(filter))).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<FilterResults<ObjectDatumStreamAliasEntity, UUID>> result = controller
				.listAliases(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void createAlias() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new ObjectDatumStreamAliasEntityInput();

		final var bizResult = new ObjectDatumStreamAliasEntity(randomUUID(), EPOCH, EPOCH, Node,
				randomLong(), randomSourceId(), randomLong(), randomSourceId());
		given(userAliasBiz.saveAlias(eq(userId), eq(UNASSIGNED_UUID_ID), same(input)))
				.willReturn(bizResult);

		given(request.getContextPath()).willReturn("");
		given(request.getRequestURI()).willReturn("/create");
		given(request.getServletPath()).willReturn("/");

		final ServletRequestAttributes reqAttr = new ServletRequestAttributes(request);
		setRequestAttributes(reqAttr);

		// WHEN
		becomeUser(randomString(), null, userId);

		final ResponseEntity<Result<ObjectDatumStreamAliasEntity>> result = controller
				.createAlias(input);

		// THEN
		// @formatter:off
		and.then(result)
			.isNotNull()
			.as("Returns HTTP 201 Created")
			.returns(HttpStatus.CREATED, from(ResponseEntity::getStatusCode))
			.as("Location to entity returned")
			.returns(URI.create("/api/v1/sec/datum/stream/alias/" + bizResult.id()),
					from(r -> r.getHeaders().getLocation()))
			.extracting(ResponseEntity::getBody, type(Result.class))
			.as("Result is success")
			.returns(true, from(Result<?>::getSuccess))
			.extracting(Result<?>::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void deleteAliases() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicDatumCriteria();

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<Void> result = controller.deleteAliases(filter);

		// THEN
		// @formatter:off
		then(userAliasBiz).should().deleteAliases(eq(userId), same(filter));
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result<Void>::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void getAlias() {
		// GIVEN
		final Long userId = randomLong();
		final var aliasId = randomUUID();

		final var bizResult = new ObjectDatumStreamAliasEntity(aliasId, EPOCH, EPOCH, Node, randomLong(),
				randomSourceId(), randomLong(), randomSourceId());
		given(userAliasBiz.aliasForUser(userId, bizResult.id())).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<ObjectDatumStreamAliasEntity> result = controller.getAlias(aliasId);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result<ObjectDatumStreamAliasEntity>::getSuccess))
			.extracting(Result<ObjectDatumStreamAliasEntity>::getData, type(ObjectDatumStreamAliasEntity.class))
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void updateAlias() {
		// GIVEN
		final Long userId = randomLong();
		final var aliasId = randomUUID();
		final var input = new ObjectDatumStreamAliasEntityInput();

		final var bizResult = new ObjectDatumStreamAliasEntity(aliasId, EPOCH, EPOCH, Node, randomLong(),
				randomSourceId(), randomLong(), randomSourceId());
		given(userAliasBiz.saveAlias(eq(userId), eq(aliasId), same(input))).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<ObjectDatumStreamAliasEntity> result = controller.updateAlias(aliasId, input);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result<ObjectDatumStreamAliasEntity>::getSuccess))
			.extracting(Result<ObjectDatumStreamAliasEntity>::getData, type(ObjectDatumStreamAliasEntity.class))
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void deleteAlias() {
		// GIVEN
		final Long userId = randomLong();
		final var aliasId = randomUUID();

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<Void> result = controller.deleteAlias(aliasId);

		// THEN
		// @formatter:off
		then(userAliasBiz).should().deleteAliases(eq(userId), filterCaptor.capture());
		and.then(filterCaptor.getValue())
			.as("Filter passed to biz")
			.isNotNull()
			.as("Filter has stream ID populated")
			.returns(new UUID[] { aliasId }, from(ObjectDatumStreamAliasFilter::getStreamIds))
			.as("Alias-only match configured")
			.returns(AliasOnly, from(ObjectDatumStreamAliasFilter::getStreamAliasMatchType))
			;

		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result<Void>::getSuccess))
			.extracting(Result<Void>::getData)
			.as("No data provided")
			.isNull()
			;
		// @formatter:on
	}

	private static ObjectDatumStreamAliasGrantEntity grant(Long userId) {
		return new ObjectDatumStreamAliasGrantEntity(userId, randomLong(), EPOCH, randomLong());
	}

	@Test
	public void listAliasGrants() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		final FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> bizResult = new BasicFilterResults<>(
				List.of(grant(userId)));
		given(userAliasBiz.listAliasGrants(eq(userId), same(filter))).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final var result = controller.listAliasGrants(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result returned")
			.isNotNull()
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void createAliasGrant() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new ObjectDatumStreamAliasGrantEntityInput();

		final var bizResult = grant(userId);
		given(userAliasBiz.saveAliasGrant(eq(userId), eq(UNASSIGNED_LONG_ID), same(input)))
				.willReturn(bizResult);

		given(request.getContextPath()).willReturn("");
		given(request.getRequestURI()).willReturn("/create");
		given(request.getServletPath()).willReturn("/");

		final ServletRequestAttributes reqAttr = new ServletRequestAttributes(request);
		setRequestAttributes(reqAttr);

		// WHEN
		becomeUser(randomString(), null, userId);

		final ResponseEntity<Result<ObjectDatumStreamAliasGrantEntity>> result = controller
				.createAliasGrant(input);

		// THEN
		// @formatter:off
		and.then(result)
			.isNotNull()
			.as("Returns HTTP 201 Created")
			.returns(HttpStatus.CREATED, from(ResponseEntity::getStatusCode))
			.as("Location to entity returned")
			.returns(URI.create("/api/v1/sec/datum/stream/alias/grants/" + bizResult.getGrantId()),
					from(r -> r.getHeaders().getLocation()))
			.extracting(ResponseEntity::getBody, type(Result.class))
			.as("Result is success")
			.returns(true, from(Result<?>::getSuccess))
			.extracting(Result<?>::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void getAliasGrant() {
		// GIVEN
		final Long userId = randomLong();
		final var bizResult = grant(userId);
		given(userAliasBiz.aliasGrantForUser(userId, bizResult.getGrantId())).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<ObjectDatumStreamAliasGrantEntity> result = controller
				.getAliasGrant(bizResult.getGrantId());

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result<ObjectDatumStreamAliasGrantEntity>::getSuccess))
			.as("Biz result returned")
			.returns(bizResult, from(Result<ObjectDatumStreamAliasGrantEntity>::getData))
			;
		// @formatter:on
	}

	@Test
	public void updateAliasGrant() {
		// GIVEN
		final Long userId = randomLong();
		final var input = new ObjectDatumStreamAliasGrantEntityInput();
		final var bizResult = grant(userId);
		given(userAliasBiz.saveAliasGrant(eq(userId), eq(bizResult.getGrantId()), same(input)))
				.willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<ObjectDatumStreamAliasGrantEntity> result = controller
				.updateAliasGrant(bizResult.getGrantId(), input);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result<ObjectDatumStreamAliasGrantEntity>::getSuccess))
			.as("Biz result returned")
			.returns(bizResult, from(Result<ObjectDatumStreamAliasGrantEntity>::getData))
			;
		// @formatter:on
	}

	@Test
	public void enableAliasGrant() {
		// GIVEN
		final Long userId = randomLong();
		final var bizResult = grant(userId);
		given(userAliasBiz.updateAliasGrantEnabled(userId, bizResult.getGrantId(), true))
				.willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<ObjectDatumStreamAliasGrantEntity> result = controller
				.enableAliasGrant(bizResult.getGrantId(), true);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result<ObjectDatumStreamAliasGrantEntity>::getSuccess))
			.as("Biz result returned")
			.returns(bizResult, from(Result<ObjectDatumStreamAliasGrantEntity>::getData))
			;
		// @formatter:on
	}

	@Test
	public void deleteAliasGrant() {
		// GIVEN
		final Long userId = randomLong();
		final Long grantId = randomLong();

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<Void> result = controller.deleteAliasGrant(grantId);

		// THEN
		// @formatter:off
		then(userAliasBiz).should().deleteAliasGrant(userId, grantId);
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result<Void>::getSuccess))
			;
		// @formatter:on
	}

	@Test
	public void createAliases() {
		// GIVEN
		final Long userId = randomLong();
		final List<ObjectDatumStreamAliasEntityInput> inputs = List
				.of(new ObjectDatumStreamAliasEntityInput());

		final List<ObjectDatumStreamAliasEntity> bizResult = List
				.of(new ObjectDatumStreamAliasEntity(randomUUID(), EPOCH, EPOCH, Node, randomLong(),
						randomSourceId(), randomLong(), randomSourceId()));
		given(userAliasBiz.saveAliases(eq(userId), same(inputs))).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final Result<List<ObjectDatumStreamAliasEntity>> result = controller.createAliases(inputs);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result<List<ObjectDatumStreamAliasEntity>>::getSuccess))
			.as("Biz result returned")
			.returns(bizResult, from(Result<List<ObjectDatumStreamAliasEntity>>::getData))
			;
		// @formatter:on
	}

	@Test
	public void listReceivedAliasGrants() {
		// GIVEN
		final Long userId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		final FilterResults<ObjectDatumStreamAliasGrantEntity, UserLongCompositePK> bizResult = new BasicFilterResults<>(
				List.of(grant(randomLong())));
		given(userAliasBiz.listReceivedAliasGrants(eq(userId), same(filter))).willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final var result = controller.listReceivedAliasGrants(filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

	@Test
	public void listAvailableAliasStreams() {
		// GIVEN
		final Long userId = randomLong();
		final Long objectId = randomLong();
		final var filter = new BasicObjectDatumStreamAliasGrantFilter();

		final FilterResults<ObjectDatumStreamAliasGrantStream, UserLongStringCompositePK> bizResult = new BasicFilterResults<>(
				List.of(new ObjectDatumStreamAliasGrantStream(randomLong(), randomLong(),
						randomSourceId())));
		given(userAliasBiz.listAvailableAliasStreams(eq(userId), eq(objectId), same(filter)))
				.willReturn(bizResult);

		// WHEN
		becomeUser(randomString(), null, userId);

		final var result = controller.listAvailableAliasStreams(objectId, filter);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result is success")
			.returns(true, from(Result::getSuccess))
			.extracting(Result::getData)
			.as("Biz result returned")
			.isSameAs(bizResult)
			;
		// @formatter:on
	}

}
