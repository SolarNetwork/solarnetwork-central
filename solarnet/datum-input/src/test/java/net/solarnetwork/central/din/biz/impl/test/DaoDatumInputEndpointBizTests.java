/* ==================================================================
 * DaoDatumInputEndpointBizTests.java - 24/02/2024 10:55:10 am
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

package net.solarnetwork.central.din.biz.impl.test;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.datum.DatumId.nodeId;
import static net.solarnetwork.domain.datum.GeneralDatum.nodeDatum;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.catchThrowableOfType;
import static org.assertj.core.api.BDDAssertions.from;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.central.din.biz.DatumInputEndpointBiz;
import net.solarnetwork.central.din.biz.TransformService;
import net.solarnetwork.central.din.biz.impl.DaoDatumInputEndpointBiz;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;
import net.solarnetwork.central.din.dao.InputDataEntityDao;
import net.solarnetwork.central.din.dao.TransformConfigurationDao;
import net.solarnetwork.central.din.domain.CentralDinUserEvents;
import net.solarnetwork.central.din.domain.EndpointConfiguration;
import net.solarnetwork.central.din.domain.TransformConfiguration;
import net.solarnetwork.central.domain.BasicSolarNodeOwnership;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.domain.UserUuidPK;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test cases for the {@link DaoDatumInputEndpointBiz} class.
 *
 * @author matt
 * @version 1.2
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class DaoDatumInputEndpointBizTests implements CentralDinUserEvents {

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private EndpointConfigurationDao endpointDao;

	@Mock
	private TransformConfigurationDao transformDao;

	@Mock
	private DatumWriteOnlyDao datumDao;

	@Mock
	private TransformService xformService;

	@Mock
	private DatumProcessor fluxProcessor;

	@Mock
	private InputDataEntityDao inputDataDao;

	@Mock
	private UserEventAppenderBiz userEventAppender;

	@Captor
	private ArgumentCaptor<Map<String, ?>> paramsCaptor;

	@Captor
	private ArgumentCaptor<GeneralNodeDatum> datumCaptor;

	@Captor
	private ArgumentCaptor<Identity<GeneralNodeDatumPK>> fluxDatumCaptor;

	@Captor
	private ArgumentCaptor<byte[]> dataCaptor;

	@Captor
	private ArgumentCaptor<InputStream> inputCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> logEventCaptor;

	private String xformServiceId;
	private DaoDatumInputEndpointBiz service;

	@BeforeEach
	public void setup() {
		xformServiceId = randomString();
		given(xformService.getId()).willReturn(xformServiceId);
		service = new DaoDatumInputEndpointBiz(nodeOwnershipDao, endpointDao, transformDao, datumDao,
				inputDataDao, singleton(xformService));
		service.setFluxPublisher(fluxProcessor);
		service.setUserEventAppenderBiz(userEventAppender);
	}

	@Test
	public void inputDatum() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeId(nodeId);
		endpoint.setSourceId(sourceId);
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(false);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);
		final GeneralDatum xformOutput = nodeDatum(nodeId, sourceId, null, new DatumSamples());
		xformOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", randomLong());
		given(xformService.transform(eq(in), eq(type), eq(transform), paramsCaptor.capture()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// persist datum
		final DatumPK datumPk = new DatumPK(UUID.randomUUID(), now().plusSeconds(100));
		given(datumDao.persist(any(GeneralNodeDatum.class))).willReturn(datumPk);

		// WHEN
		Collection<DatumId> result = service.importDatum(userId, endpoint.getEndpointId(), type, in,
				null);

		// THEN
		// @formatter:off
		then(datumDao).should().persist(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Persisted datum")
			.isNotNull()
			.as("Persisted node ID")
			.returns(nodeId, GeneralNodeDatum::getNodeId)
			.as("Persisted source ID")
			.returns(sourceId, GeneralNodeDatum::getSourceId)
			.as("Timestamp not provided to DAO")
			.returns(null, GeneralNodeDatum::getCreated)
			.as("Persisted samples")
			.returns(xformOutput.getSamples(), GeneralNodeDatum::getSamples)
			;

		then(fluxProcessor).shouldHaveNoInteractions();

		and.then(result)
			.as("Single result returned for persisted datum")
			.contains(nodeId(nodeId, sourceId, datumPk.getTimestamp()))
			;
		// @formatter:on
	}

	@Test
	public void inputDatum_solarFlux() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setNodeId(nodeId);
		endpoint.setSourceId(sourceId);
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(true);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);
		final GeneralDatum xformOutput = nodeDatum(nodeId, sourceId, null, new DatumSamples());
		xformOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", randomLong());
		given(xformService.transform(eq(in), eq(type), eq(transform), paramsCaptor.capture()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// persist datum
		final DatumPK datumPk = new DatumPK(UUID.randomUUID(), now().plusSeconds(100));
		given(datumDao.persist(any(GeneralNodeDatum.class))).willReturn(datumPk);

		// publish to SolarFlux
		given(fluxProcessor.isConfigured()).willReturn(true);

		// WHEN
		Collection<DatumId> result = service.importDatum(userId, endpoint.getEndpointId(), type, in,
				null);

		// THEN
		// @formatter:off
		then(datumDao).should().persist(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Persisted datum")
			.isNotNull()
			.as("Persisted node ID")
			.returns(nodeId, GeneralNodeDatum::getNodeId)
			.as("Persisted source ID")
			.returns(sourceId, GeneralNodeDatum::getSourceId)
			.as("Timestamp not provided to DAO")
			.returns(null, GeneralNodeDatum::getCreated)
			.as("Persisted samples")
			.returns(xformOutput.getSamples(), GeneralNodeDatum::getSamples)
			;

		then(fluxProcessor).should().processDatum(fluxDatumCaptor.capture());
		and.then(fluxDatumCaptor.getValue())
			.as("Published datum to SolarFlux")
			.isNotNull()
			.as("Published same datum to SolarFlux as SolarIn")
			.isSameAs(datumCaptor.getValue())
			;

		and.then(result)
			.as("Single result returned for persisted datum")
			.contains(nodeId(nodeId, sourceId, datumPk.getTimestamp()))
			;
		// @formatter:on
	}

	@Test
	public void nodeSourceInputParameters() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(false);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);
		final GeneralDatum xformOutput = nodeDatum(nodeId, sourceId, null, new DatumSamples());
		xformOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", randomLong());
		given(xformService.transform(eq(in), eq(type), eq(transform), paramsCaptor.capture()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// persist datum
		final DatumPK datumPk = new DatumPK(UUID.randomUUID(), now().plusSeconds(100));
		given(datumDao.persist(any(GeneralNodeDatum.class))).willReturn(datumPk);

		// WHEN
		var parameters = Map.of(DatumInputEndpointBiz.PARAM_NODE_ID, nodeId.toString(),
				DatumInputEndpointBiz.PARAM_SOURCE_ID, sourceId);
		Collection<DatumId> result = service.importDatum(userId, endpoint.getEndpointId(), type, in,
				parameters);

		// THEN
		// @formatter:off
		then(datumDao).should().persist(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Persisted datum")
			.isNotNull()
			.as("Persisted node ID")
			.returns(nodeId, GeneralNodeDatum::getNodeId)
			.as("Persisted source ID")
			.returns(sourceId, GeneralNodeDatum::getSourceId)
			.as("Timestamp not provided to DAO")
			.returns(null, GeneralNodeDatum::getCreated)
			.as("Persisted samples")
			.returns(xformOutput.getSamples(), GeneralNodeDatum::getSamples)
			;

		then(fluxProcessor).shouldHaveNoInteractions();

		and.then(result)
			.as("Single result returned for persisted datum")
			.contains(nodeId(nodeId, sourceId, datumPk.getTimestamp()))
			;
		// @formatter:on
	}

	@Test
	public void nodeSourceInputParameters_notAuthorized() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(false);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[0]);
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);
		final GeneralDatum xformOutput = nodeDatum(nodeId, sourceId, null, new DatumSamples());
		xformOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", randomLong());
		given(xformService.transform(eq(in), eq(type), eq(transform), paramsCaptor.capture()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId + 1, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// WHEN
		var parameters = Map.of(DatumInputEndpointBiz.PARAM_NODE_ID, nodeId.toString(),
				DatumInputEndpointBiz.PARAM_SOURCE_ID, sourceId);
		AuthorizationException error = catchThrowableOfType(() -> {
			service.importDatum(userId, endpoint.getEndpointId(), type, in, parameters);
		}, AuthorizationException.class);

		// THEN
		// @formatter:off
		then(userEventAppender).should().addEvent(eq(userId), logEventCaptor.capture());
		and.then(logEventCaptor.getValue())
			.satisfies(event -> {
				Map<String, Object> data = JsonUtils.getStringMap(event.getData());
				and.then(data)
					.as("Event data contains endpoint ID")
					.containsEntry(ENDPOINT_ID_DATA_KEY, endpoint.getEndpointId().toString())
					.as("Event data contains transform ID")
					.containsEntry(TRANSFORM_ID_DATA_KEY, transform.getTransformId())
					.as("Event data contains transform service ID")
					.containsEntry(TRANSFORM_SERVICE_ID_DATA_KEY, xformServiceId)
					.as("Event data contains content type")
					.containsEntry(CONTENT_TYPE_DATA_KEY, type.toString())
					.hasEntrySatisfying(PARAMETERS_DATA_KEY, (p) -> {
						and.then(p)
							.asInstanceOf(InstanceOfAssertFactories.map(String.class, String.class))
							.as("Extra parameters has node ID")
							.containsEntry(DatumInputEndpointBiz.PARAM_NODE_ID, nodeId.toString())
							.as("Extra parameters has source ID")
							.containsEntry(DatumInputEndpointBiz.PARAM_SOURCE_ID, sourceId)
							;
					})
					;

				String[] tags = event.getTags();
				and.then(tags)
					.as("Event tags as expected, with error")
					.contains(DIN_TAG, DATUM_TAG, ERROR_TAG)
					;
			})
			;

		then(fluxProcessor).shouldHaveNoInteractions();

		and.then(error)
			.as("Error reason is denied for node not owned by user")
			.returns(AuthorizationException.Reason.ACCESS_DENIED, from(AuthorizationException::getReason))
			.as("Object ID is node ID")
			.returns(nodeId, from(AuthorizationException::getId))
			;
		// @formatter:on
	}

	@Test
	public void trackPrevious_first() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(false);
		endpoint.setPreviousInputTracking(true);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);

		// track previous data in cache (not found)
		final UserLongStringCompositePK inputCacheKey = new UserLongStringCompositePK(userId, nodeId,
				sourceId);
		given(inputDataDao.getAndPut(eq(inputCacheKey), any(byte[].class))).willReturn(null);

		// WHEN
		var parameters = Map.of(DatumInputEndpointBiz.PARAM_NODE_ID, nodeId.toString(),
				DatumInputEndpointBiz.PARAM_SOURCE_ID, sourceId);
		Collection<DatumId> result = service.importDatum(userId, endpoint.getEndpointId(), type, in,
				parameters);

		// THEN
		// @formatter:off
		then(inputDataDao).should().getAndPut(eq(inputCacheKey), dataCaptor.capture());
		and.then(dataCaptor.getValue())
			.as("Put input data into tracking cache")
			.containsExactly(1, 2, 3)
			;

		then(datumDao).shouldHaveNoInteractions();
		then(fluxProcessor).shouldHaveNoInteractions();

		and.then(result)
			.as("Empty result returned for tracked first datum")
			.isEmpty();
			;
		// @formatter:on
	}

	@Test
	public void trackPrevious_second() throws IOException {
		// GIVEN
		final Long userId = randomLong();
		final Long nodeId = randomLong();
		final String sourceId = randomString();

		final var transform = new TransformConfiguration(userId, randomLong(), now());
		transform.setServiceIdentifier(xformServiceId);

		final var endpoint = new EndpointConfiguration(userId, UUID.randomUUID(), now());
		endpoint.setTransformId(transform.getTransformId());
		endpoint.setPublishToSolarFlux(false);
		endpoint.setPreviousInputTracking(true);

		// load transform configuration
		given(endpointDao.get(new UserUuidPK(userId, endpoint.getEndpointId()))).willReturn(endpoint);
		given(transformDao.get(new UserLongCompositePK(userId, transform.getTransformId())))
				.willReturn(transform);

		// transform input
		final var in = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
		final MimeType type = MediaType.APPLICATION_JSON;
		given(xformService.supportsInput(in, type)).willReturn(true);

		// track previous data in cache (found)
		final UserLongStringCompositePK inputCacheKey = new UserLongStringCompositePK(userId, nodeId,
				sourceId);
		given(inputDataDao.getAndPut(eq(inputCacheKey), any(byte[].class)))
				.willReturn(new byte[] { 2, 3, 4 });

		final GeneralDatum xformOutput = nodeDatum(nodeId, sourceId, null, new DatumSamples());
		xformOutput.putSampleValue(DatumSamplesType.Instantaneous, "foo", randomLong());
		given(xformService.transform(any(), eq(type), eq(transform), any()))
				.willReturn(asList(xformOutput));

		// verify datum ownership
		final var owner = new BasicSolarNodeOwnership(nodeId, userId, "NZ", ZoneOffset.UTC, false,
				false);
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(owner);

		// persist datum
		final DatumPK datumPk = new DatumPK(UUID.randomUUID(), now().plusSeconds(100));
		given(datumDao.persist(any(GeneralNodeDatum.class))).willReturn(datumPk);

		// WHEN
		var parameters = Map.of(DatumInputEndpointBiz.PARAM_NODE_ID, nodeId.toString(),
				DatumInputEndpointBiz.PARAM_SOURCE_ID, sourceId);
		Collection<DatumId> result = service.importDatum(userId, endpoint.getEndpointId(), type, in,
				parameters);

		// THEN
		// @formatter:off
		then(inputDataDao).should().getAndPut(eq(inputCacheKey), dataCaptor.capture());
		and.then(dataCaptor.getValue())
			.as("Put input data into tracking cache")
			.containsExactly(1, 2, 3)
			;

		then(xformService).should().transform(inputCaptor.capture(),  eq(type), eq(transform), paramsCaptor.capture());
		and.then(inputCaptor.getValue())
			.as("Current input supplied as ByteArrayInputStream")
			.satisfies(curr -> {
				byte[] currData = ((ByteArrayInputStream)curr).readAllBytes();
				and.then(currData)
					.as("Current input data provided")
					.containsExactly(1, 2, 3)
					;
			})
			;

		and.then(paramsCaptor.getValue())
			.as("Previous input provided as ByteArrayInputStream")
			.hasEntrySatisfying(TransformService.PARAM_PREVIOUS_INPUT, prev -> {
				byte[] prevData = ((ByteArrayInputStream)prev).readAllBytes();
				and.then(prevData)
					.as("Previous input data provided")
					.containsExactly(2, 3, 4)
					;
			})
			;

		then(datumDao).should().persist(datumCaptor.capture());
		and.then(datumCaptor.getValue())
			.as("Persisted datum")
			.isNotNull()
			.as("Persisted node ID")
			.returns(nodeId, GeneralNodeDatum::getNodeId)
			.as("Persisted source ID")
			.returns(sourceId, GeneralNodeDatum::getSourceId)
			.as("Timestamp not provided to DAO")
			.returns(null, GeneralNodeDatum::getCreated)
			.as("Persisted samples")
			.returns(xformOutput.getSamples(), GeneralNodeDatum::getSamples)
			;


		then(fluxProcessor).shouldHaveNoInteractions();

		and.then(result)
			.as("Single result returned for persisted datum")
			.contains(nodeId(nodeId, sourceId, datumPk.getTimestamp()))
			;
		// @formatter:on
	}

}
