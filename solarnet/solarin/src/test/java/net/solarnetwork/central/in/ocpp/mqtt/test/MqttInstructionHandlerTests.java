/* ==================================================================
 * MqttInstructionHandlerTests.java - 2/04/2020 7:01:41 pm
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

package net.solarnetwork.central.in.ocpp.mqtt.test;

import static java.util.UUID.randomUUID;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import net.solarnetwork.central.in.ocpp.mqtt.MqttInstructionHandler;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.common.mqtt.BasicMqttMessage;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointInfo;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import net.solarnetwork.ocpp.v16.cp.ChargePointActionMessage;
import ocpp.v16.ChargePointAction;
import ocpp.v16.cp.AvailabilityType;
import ocpp.v16.cp.ChangeAvailabilityRequest;
import ocpp.v16.cp.ChangeAvailabilityResponse;
import ocpp.v16.cp.ChangeConfigurationRequest;
import ocpp.v16.cp.ChangeConfigurationResponse;
import ocpp.v16.cp.ConfigurationStatus;

/**
 * Test cases for the {@link MattInstructionHandler} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttInstructionHandlerTests {

	private ObjectMapper objectMapper;
	private NodeInstructionDao instructionDao;
	private CentralChargePointDao chargePointDao;
	private ChargePointRouter chargePointRouter;
	private ChargePointBroker chargePointBroker;
	private MqttConnection mqttConnection;
	private MqttInstructionHandler<ChargePointAction> handler;

	@Before
	public void setup() {
		instructionDao = EasyMock.createMock(NodeInstructionDao.class);
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		chargePointRouter = EasyMock.createMock(ChargePointRouter.class);
		chargePointBroker = EasyMock.createMock(ChargePointBroker.class);
		mqttConnection = EasyMock.createMock(MqttConnection.class);
		objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.registerModule(new JaxbAnnotationModule());
		handler = new MqttInstructionHandler<>(ChargePointAction.class, instructionDao, chargePointDao,
				objectMapper, chargePointRouter);
	}

	@After
	public void teardown() {
		EasyMock.verify(instructionDao, chargePointDao, chargePointRouter, mqttConnection,
				chargePointBroker);
	}

	private void replayAll() {
		EasyMock.replay(instructionDao, chargePointDao, chargePointRouter, mqttConnection,
				chargePointBroker);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void changeAvailability() throws Exception {
		// GIVEN
		Long instructionId = randomUUID().getMostSignificantBits();
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));
		ChargePointIdentity clientId = new ChargePointIdentity(cp.getInfo().getId(), cp.getUserId());
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(1);
		req.setType(AvailabilityType.INOPERATIVE);
		JsonNode json = objectMapper.valueToTree(req);
		ChargePointActionMessage message = new ChargePointActionMessage(clientId,
				instructionId.toString(), ChargePointAction.ChangeAvailability, json);

		handler.onMqttServerConnectionEstablished(mqttConnection, false);

		expect(mqttConnection.isEstablished()).andReturn(true).anyTimes();

		Capture<MqttMessage> mqttMessageCaptor = new Capture<>();
		CompletableFuture<?> publishFuture = CompletableFuture.completedFuture(null);
		expect(mqttConnection.publish(capture(mqttMessageCaptor)))
				.andReturn((CompletableFuture) publishFuture);

		// WHEN
		replayAll();

		CountDownLatch latch = new CountDownLatch(1);
		handler.processActionMessage(message, (msg, res, err) -> {
			assertThat("No error returned", err, nullValue());
			latch.countDown();
			return true;
		});

		// THEN
		latch.await(5, TimeUnit.SECONDS);

		MqttMessage mqttMessage = mqttMessageCaptor.getValue();
		assertThat("MQTT topic", mqttMessage.getTopic(), equalTo(handler.getMqttTopic()));
		assertThat("MQTT QoS", mqttMessage.getQosLevel(), equalTo(MqttQos.AtMostOnce));

		JsonNode pubJson = objectMapper.readTree(mqttMessage.getPayload());
		assertThat("MQTT payload", pubJson, instanceOf(ObjectNode.class));
		assertThat("Pub action", pubJson.path("action").asText(),
				equalTo(ChargePointAction.ChangeAvailability.getName()));
		assertThat("Pub charger identifier", pubJson.path("clientId").path("identifier").asText(),
				equalTo(cp.getInfo().getId()));
		assertThat("Pub charger user", pubJson.path("clientId").path("userIdentifier").asLong(),
				equalTo(cp.getUserId()));
		assertThat("Pub message ID", pubJson.path("messageId").asText(),
				equalTo(instructionId.toString()));
		assertThat("Pub payload", pubJson.path("message"), equalTo(json));
	}

	@Test
	public void handleChangeAvailability() throws Exception {
		// GIVEN
		Long instructionId = randomUUID().getMostSignificantBits();
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));
		ChargePointIdentity clientId = new ChargePointIdentity(cp.getInfo().getId(), cp.getUserId());
		ChangeAvailabilityRequest req = new ChangeAvailabilityRequest();
		req.setConnectorId(1);
		req.setType(AvailabilityType.INOPERATIVE);
		JsonNode json = objectMapper.valueToTree(req);
		ChargePointActionMessage actionMessage = new ChargePointActionMessage(clientId,
				instructionId.toString(), ChargePointAction.ChangeAvailability, json);

		BasicMqttMessage message = new BasicMqttMessage(handler.getMqttTopic(), false,
				MqttQos.AtMostOnce, objectMapper.writeValueAsBytes(actionMessage));

		// get ChargePoint entity
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get connection to charge point
		expect(chargePointRouter.brokerForChargePoint(cp.chargePointIdentity()))
				.andReturn(chargePointBroker);

		// mark instruction as executing
		expect(instructionDao.compareAndUpdateInstructionState(instructionId, cp.getNodeId(),
				InstructionState.Received, InstructionState.Executing, null)).andReturn(true);

		// send action to charger point
		Capture<ActionMessage<Object>> actionMessageCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> actionMessageResultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionMessageCaptor),
				capture(actionMessageResultHandlerCaptor))).andReturn(true);

		// mark instruction completed (without result parameters)
		expect(instructionDao.compareAndUpdateInstructionState(instructionId, cp.getNodeId(),
				InstructionState.Executing, InstructionState.Completed, null)).andReturn(true);

		// WHEN
		replayAll();
		handler.onMqttMessage(message);

		ChangeAvailabilityResponse res = new ChangeAvailabilityResponse();
		actionMessageResultHandlerCaptor.getValue()
				.handleActionMessageResult(actionMessageCaptor.getValue(), res, null);

		// THEN
		ActionMessage<Object> postedActionMessage = actionMessageCaptor.getValue();
		assertThat("Posted action", postedActionMessage.getAction(),
				equalTo(ChargePointAction.ChangeAvailability));
	}

	@Test
	public void handleChangeConfiguration() throws Exception {
		// GIVEN
		Long instructionId = randomUUID().getMostSignificantBits();
		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now(),
				new ChargePointInfo(randomUUID().toString(), "SolarNetwork", "SolarNode"));
		ChargePointIdentity clientId = new ChargePointIdentity(cp.getInfo().getId(), cp.getUserId());
		ChangeConfigurationRequest req = new ChangeConfigurationRequest();
		req.setKey("foo");
		req.setValue("bar");
		JsonNode json = objectMapper.valueToTree(req);
		ChargePointActionMessage actionMessage = new ChargePointActionMessage(clientId,
				instructionId.toString(), ChargePointAction.ChangeConfiguration, json);

		BasicMqttMessage message = new BasicMqttMessage(handler.getMqttTopic(), false,
				MqttQos.AtMostOnce, objectMapper.writeValueAsBytes(actionMessage));

		// get ChargePoint entity
		expect(chargePointDao.getForIdentity(cp.chargePointIdentity())).andReturn(cp);

		// get connection to charge point
		expect(chargePointRouter.brokerForChargePoint(cp.chargePointIdentity()))
				.andReturn(chargePointBroker);

		// mark instruction as executing
		expect(instructionDao.compareAndUpdateInstructionState(instructionId, cp.getNodeId(),
				InstructionState.Received, InstructionState.Executing, null)).andReturn(true);

		// send action to charger point
		Capture<ActionMessage<Object>> actionMessageCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> actionMessageResultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionMessageCaptor),
				capture(actionMessageResultHandlerCaptor))).andReturn(true);

		// mark instruction completed (without result parameters)
		Capture<Map<String, Object>> resultParamsCaptor = new Capture<>();
		expect(instructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(cp.getNodeId()),
				eq(InstructionState.Executing), eq(InstructionState.Completed),
				capture(resultParamsCaptor))).andReturn(true);

		// WHEN
		replayAll();
		handler.onMqttMessage(message);

		ChangeConfigurationResponse res = new ChangeConfigurationResponse();
		res.setStatus(ConfigurationStatus.ACCEPTED);
		actionMessageResultHandlerCaptor.getValue()
				.handleActionMessageResult(actionMessageCaptor.getValue(), res, null);

		// THEN
		ActionMessage<Object> postedActionMessage = actionMessageCaptor.getValue();
		assertThat("Posted action", postedActionMessage.getAction(),
				equalTo(ChargePointAction.ChangeConfiguration));

		Map<String, Object> resultParams = resultParamsCaptor.getValue();
		assertThat("Result map has status", resultParams,
				hasEntry("status", ConfigurationStatus.ACCEPTED.value()));
		assertThat("Result map has size 1", resultParams.keySet(), hasSize(1));
	}

}
