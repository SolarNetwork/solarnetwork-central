/* ==================================================================
 * DaoInstructionInputEndpointBizTests.java - 31/03/2024 6:33:41 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.inin.biz.impl.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.dao.UserMetadataDao;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.inin.biz.RequestTransformService;
import net.solarnetwork.central.inin.biz.ResponseTransformService;
import net.solarnetwork.central.inin.biz.TransformConstants;
import net.solarnetwork.central.inin.biz.impl.DaoInstructionInputEndpointBiz;
import net.solarnetwork.central.inin.dao.EndpointConfigurationDao;
import net.solarnetwork.central.inin.dao.TransformConfigurationDao;
import net.solarnetwork.central.inin.domain.CentralInstructionInputUserEvents;
import net.solarnetwork.central.inin.domain.EndpointConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.RequestTransformConfiguration;
import net.solarnetwork.central.inin.domain.TransformConfiguration.ResponseTransformConfiguration;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link DaoInstructionInputEndpointBiz} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoInstructionInputEndpointBizTests implements CentralInstructionInputUserEvents {

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private EndpointConfigurationDao endpointDao;

	@Mock
	private TransformConfigurationDao<RequestTransformConfiguration> requestTransformDao;

	@Mock
	private TransformConfigurationDao<ResponseTransformConfiguration> responseTransformDao;

	@Mock
	private UserMetadataDao userMetadataDao;

	@Mock
	private RequestTransformService requestXformService;

	@Mock
	private ResponseTransformService responseXformService;

	@Mock
	private UserEventAppenderBiz userEventAppender;

	@Mock
	private InstructorBiz instructor;

	@Captor
	private ArgumentCaptor<Map<String, ?>> paramsCaptor;

	@Captor
	private ArgumentCaptor<byte[]> dataCaptor;

	@Captor
	private ArgumentCaptor<InputStream> inputCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	@Captor
	private ArgumentCaptor<Iterable<NodeInstruction>> instructionsCaptor;

	private String requestXformServiceId;
	private String responseXformServiceId;
	private DaoInstructionInputEndpointBiz service;

	@BeforeEach
	public void setup() {
		requestXformServiceId = randomString();
		given(requestXformService.getId()).willReturn(requestXformServiceId);
		responseXformServiceId = randomString();
		given(responseXformService.getId()).willReturn(responseXformServiceId);
		service = new DaoInstructionInputEndpointBiz(new SimpleAsyncTaskExecutor(), instructor,
				nodeOwnershipDao, endpointDao, requestTransformDao, responseTransformDao,
				userMetadataDao, singleton(requestXformService), singleton(responseXformService));
		service.setUserEventAppenderBiz(userEventAppender);
	}

	@Test
	public void inputInstruction() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new RequestTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(requestXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(Collections.singleton(nodeId));
		endpoint.setRequestTransformId(transform.getTransformId());

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(requestTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(requestXformService.supportsInput(in, type)).willReturn(true);
		final NodeInstruction xformOutput = new NodeInstruction(randomString(), Instant.now(), nodeId);
		given(requestXformService.transformInput(eq(in), eq(type), eq(transform), any()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// enqueue instruction
		final NodeInstruction queuedInstruction = xformOutput.clone();
		queuedInstruction.setId(randomLong());
		queuedInstruction.setState(InstructionState.Queuing);
		given(instructor.queueInstruction(eq(nodeId), same(xformOutput))).willReturn(queuedInstruction);

		// WHEN
		Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		Collection<NodeInstruction> result = service.importInstructions(userId, endpoint.getEndpointId(),
				type, in, params);

		// THEN
		// @formatter:off
		then(requestXformService).should().transformInput(eq(in),  eq(type), eq(transform), paramsCaptor.capture());
		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(REQ_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(REQ_TRANSFORM_SERVICE_ID_DATA_KEY, requestXformServiceId)
					.as("Event data contains content type")
					.containsEntry(CONTENT_TYPE_DATA_KEY, type.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", queuedInstruction.getId())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_IMPORTED_TAG)
					;
			})
			;

		and.then(result)
			.as("Single result returned for queued instruction")
			.hasSize(1)
			.element(0)
			.isSameAs(queuedInstruction)
			;
		// @formatter:on
	}

	@Test
	public void inputInstruction_withUserMetadataPath() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new RequestTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(requestXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(Collections.singleton(nodeId));
		endpoint.setRequestTransformId(transform.getTransformId());
		endpoint.setUserMetadataPath("/pm/foo");

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(requestTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// get metadata
		final String userMetaJson = """
				{"meta":"data"}
				""";
		given(userMetadataDao.jsonMetadataAtPath(userId, endpoint.getUserMetadataPath()))
				.willReturn(userMetaJson);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(requestXformService.supportsInput(in, type)).willReturn(true);
		final NodeInstruction xformOutput = new NodeInstruction(randomString(), Instant.now(), nodeId);
		given(requestXformService.transformInput(eq(in), eq(type), eq(transform), any()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// enqueue instruction
		final NodeInstruction queuedInstruction = xformOutput.clone();
		queuedInstruction.setId(randomLong());
		queuedInstruction.setState(InstructionState.Queuing);
		given(instructor.queueInstruction(eq(nodeId), same(xformOutput))).willReturn(queuedInstruction);

		// WHEN
		Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		Collection<NodeInstruction> result = service.importInstructions(userId, endpoint.getEndpointId(),
				type, in, params);

		// THEN
		// @formatter:off
		then(requestXformService).should().transformInput(eq(in),  eq(type), eq(transform), paramsCaptor.capture());
		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("User metadata provided")
			.containsEntry(TransformConstants.PARAM_USER_METADATA_JSON, userMetaJson)
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(REQ_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(REQ_TRANSFORM_SERVICE_ID_DATA_KEY, requestXformServiceId)
					.as("Event data contains content type")
					.containsEntry(CONTENT_TYPE_DATA_KEY, type.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", queuedInstruction.getId())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_IMPORTED_TAG)
					;
			})
			;

		and.then(result)
			.as("Single result returned for queued instruction")
			.hasSize(1)
			.element(0)
			.isSameAs(queuedInstruction)
			;
		// @formatter:on
	}

	@Test
	public void outputResult() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new ResponseTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(responseXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(singleton(nodeId));
		endpoint.setResponseTransformId(transform.getTransformId());

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(responseTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform instruction
		final MimeType type = MediaType.APPLICATION_JSON;
		given(responseXformService.supportsOutputType(type)).willReturn(true);

		final var instruction = new NodeInstruction(randomString(), Instant.now(), nodeId);
		instruction.setId(randomLong());
		instruction.setState(InstructionState.Queuing);

		// lookup instruction result
		final var finishedInstruction = instruction.clone();
		finishedInstruction.setState(InstructionState.Completed);
		finishedInstruction.setResultParameters(Map.of("all", "done"));
		given(instructor.getInstruction(instruction.getId())).willReturn(finishedInstruction);

		// WHEN
		final Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		final List<NodeInstruction> instructions = asList(instruction);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		service.generateResponse(userId, endpoint.getEndpointId(), instructions, type, out, params);

		// THEN
		// @formatter:off
		final String response = "Hello, world.";
		then(responseXformService).should()
			.transformOutput(
					instructionsCaptor.capture(),
					eq(type),
					same(transform),
					paramsCaptor.capture(),
					assertArg((OutputStream o) -> {
						if (out.size() < 1 ) {
							out.write(response.getBytes(StandardCharsets.UTF_8));
						}
					}));


		and.then(instructionsCaptor.getValue())
			.as("Transform passed finished instruction")
			.hasSize(1)
			.element(0)
			.as("Finished instruction instance passed to transform")
			.isSameAs(finishedInstruction)
			;

		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(RES_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(RES_TRANSFORM_SERVICE_ID_DATA_KEY, responseXformServiceId)
					.as("Event data contains content type")
					.containsEntry(OUTPUT_TYPE_DATA_KEY, type.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", finishedInstruction.getId())
							.as("Event data instruction state from finished instance")
							.containsEntry("state", finishedInstruction.getState().toString())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_EXECUTED_TAG)
					;
			})
			;

		and.then(out.toString(StandardCharsets.UTF_8))
			.as("Response generated")
			.isEqualTo(response)
			;
		// @formatter:on
	}

	@Test
	public void outputResult_withUserMetadataPath() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new ResponseTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(responseXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(singleton(nodeId));
		endpoint.setResponseTransformId(transform.getTransformId());
		endpoint.setUserMetadataPath("/pm/foo");

		// get metadata
		final String userMetaJson = """
				{"meta":"data"}
				""";
		given(userMetadataDao.jsonMetadataAtPath(userId, endpoint.getUserMetadataPath()))
				.willReturn(userMetaJson);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(responseTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform instruction
		final MimeType type = MediaType.APPLICATION_JSON;
		given(responseXformService.supportsOutputType(type)).willReturn(true);

		final var instruction = new NodeInstruction(randomString(), Instant.now(), nodeId);
		instruction.setId(randomLong());
		instruction.setState(InstructionState.Queuing);

		// lookup instruction result
		final var finishedInstruction = instruction.clone();
		finishedInstruction.setState(InstructionState.Completed);
		finishedInstruction.setResultParameters(Map.of("all", "done"));
		given(instructor.getInstruction(instruction.getId())).willReturn(finishedInstruction);

		// WHEN
		final Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		final List<NodeInstruction> instructions = asList(instruction);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		service.generateResponse(userId, endpoint.getEndpointId(), instructions, type, out, params);

		// THEN
		// @formatter:off
		final String response = "Hello, world.";
		then(responseXformService).should()
			.transformOutput(
					instructionsCaptor.capture(),
					eq(type),
					same(transform),
					paramsCaptor.capture(),
					assertArg((OutputStream o) -> {
						if (out.size() < 1 ) {
							out.write(response.getBytes(StandardCharsets.UTF_8));
						}
					}));


		and.then(instructionsCaptor.getValue())
			.as("Transform passed finished instruction")
			.hasSize(1)
			.element(0)
			.as("Finished instruction instance passed to transform")
			.isSameAs(finishedInstruction)
			;

		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("User metadata provided")
			.containsEntry(TransformConstants.PARAM_USER_METADATA_JSON, userMetaJson)
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(RES_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(RES_TRANSFORM_SERVICE_ID_DATA_KEY, responseXformServiceId)
					.as("Event data contains content type")
					.containsEntry(OUTPUT_TYPE_DATA_KEY, type.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", finishedInstruction.getId())
							.as("Event data instruction state from finished instance")
							.containsEntry("state", finishedInstruction.getState().toString())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_EXECUTED_TAG)
					;
			})
			;

		and.then(out.toString(StandardCharsets.UTF_8))
			.as("Response generated")
			.isEqualTo(response)
			;
		// @formatter:on
	}

	@Test
	public void inputInstruction_withContentType() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new RequestTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(requestXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(Collections.singleton(nodeId));
		endpoint.setRequestTransformId(transform.getTransformId());
		endpoint.setRequestContentType("foo/bar");

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(requestTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType reqType = MimeType.valueOf(endpoint.getRequestContentType());
		given(requestXformService.supportsInput(in, reqType)).willReturn(true);
		final NodeInstruction xformOutput = new NodeInstruction(randomString(), Instant.now(), nodeId);
		given(requestXformService.transformInput(eq(in), eq(reqType), eq(transform), any()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// enqueue instruction
		final NodeInstruction queuedInstruction = xformOutput.clone();
		queuedInstruction.setId(randomLong());
		queuedInstruction.setState(InstructionState.Queuing);
		given(instructor.queueInstruction(eq(nodeId), same(xformOutput))).willReturn(queuedInstruction);

		// WHEN
		// post application/json type but use endpoint foo/bar type
		Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		Collection<NodeInstruction> result = service.importInstructions(userId, endpoint.getEndpointId(),
				MediaType.APPLICATION_JSON, in, params);

		// THEN
		// @formatter:off
		then(requestXformService).should().transformInput(eq(in),  eq(reqType), eq(transform), paramsCaptor.capture());
		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(REQ_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(REQ_TRANSFORM_SERVICE_ID_DATA_KEY, requestXformServiceId)
					.as("Event data contains content type")
					.containsEntry(CONTENT_TYPE_DATA_KEY, reqType.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", queuedInstruction.getId())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_IMPORTED_TAG)
					;
			})
			;

		and.then(result)
			.as("Single result returned for queued instruction")
			.hasSize(1)
			.element(0)
			.isSameAs(queuedInstruction)
			;
		// @formatter:on
	}

	@Test
	public void outputResult_withContentType() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();

		final var transform = new ResponseTransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(responseXformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeIds(singleton(nodeId));
		endpoint.setResponseTransformId(transform.getTransformId());
		endpoint.setResponseContentType("bim/bam");

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(responseTransformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform instruction
		final MimeType type = MimeType.valueOf(endpoint.getResponseContentType());
		given(responseXformService.supportsOutputType(type)).willReturn(true);

		final var instruction = new NodeInstruction(randomString(), Instant.now(), nodeId);
		instruction.setId(randomLong());
		instruction.setState(InstructionState.Queuing);

		// lookup instruction result
		final var finishedInstruction = instruction.clone();
		finishedInstruction.setState(InstructionState.Completed);
		finishedInstruction.setResultParameters(Map.of("all", "done"));
		given(instructor.getInstruction(instruction.getId())).willReturn(finishedInstruction);

		// WHEN
		final Map<String, String> params = Map.of("foo", "bar", "bim", "bam");
		final List<NodeInstruction> instructions = asList(instruction);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		// post application/json type but use endpoint bim/bam type
		service.generateResponse(userId, endpoint.getEndpointId(), instructions,
				MediaType.APPLICATION_JSON, out, params);

		// THEN
		// @formatter:off
		final String response = "Hello, world.";
		then(responseXformService).should()
			.transformOutput(
					instructionsCaptor.capture(),
					eq(type),
					same(transform),
					paramsCaptor.capture(),
					assertArg((OutputStream o) -> {
						if (out.size() < 1 ) {
							out.write(response.getBytes(StandardCharsets.UTF_8));
						}
					}));


		and.then(instructionsCaptor.getValue())
			.as("Transform passed finished instruction")
			.hasSize(1)
			.element(0)
			.as("Finished instruction instance passed to transform")
			.isSameAs(finishedInstruction)
			;

		and.then(paramsCaptor.getValue())
			.asInstanceOf(map(String.class, Object.class))
			.as("User ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_USER_ID, userId)
			.as("Endpoint ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_ENDPOINT_ID, endpoint.getEndpointId().toString())
			.as("Transform ID transform parameter provided")
			.containsEntry(TransformConstants.PARAM_TRANSFORM_ID, transform.getTransformId())
			.as("Transform cache key provided")
			.containsEntry(TransformConstants.PARAM_CONFIGURATION_CACHE_KEY, transform.ident())
			.as("Service parameters passed as transform parameters")
			.containsAllEntriesOf(params)
			;

		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.as("Event published for instruction input")
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(RES_TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(RES_TRANSFORM_SERVICE_ID_DATA_KEY, responseXformServiceId)
					.as("Event data contains content type")
					.containsEntry(OUTPUT_TYPE_DATA_KEY, type.toString())
					.containsEntry(PARAMETERS_DATA_KEY, params)
					.hasEntrySatisfying(INSTRUCTION_DATA_KEY, o -> {
						and.then(o)
							.asInstanceOf(map(String.class, Object.class))
							.as("Event data instruction has ID")
							.containsEntry("id", finishedInstruction.getId())
							.as("Event data instruction state from finished instance")
							.containsEntry("state", finishedInstruction.getState().toString())
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, for input")
					.containsExactly(INSTRUCTION_TAG, ININ_TAG, INSTRUCTION_EXECUTED_TAG)
					;
			})
			;

		and.then(out.toString(StandardCharsets.UTF_8))
			.as("Response generated")
			.isEqualTo(response)
			;
		// @formatter:on
	}

}
