/* ==================================================================
 * OcppControllerTests.java - 31/03/2020 2:33:05 pm
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

package net.solarnetwork.central.ocpp.v16.controller.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_ACTION_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_CHARGER_IDENTIFIER_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_MESSAGE_PARAM;
import static net.solarnetwork.central.ocpp.util.OcppInstructionUtils.OCPP_V16_TOPIC;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import java.time.Instant;
import java.util.Map;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.ocpp.dao.CentralAuthorizationDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointConnectorDao;
import net.solarnetwork.central.ocpp.dao.CentralChargePointDao;
import net.solarnetwork.central.ocpp.domain.CentralChargePoint;
import net.solarnetwork.central.ocpp.v16.controller.OcppController;
import net.solarnetwork.central.test.CallingThreadExecutorService;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.ocpp.domain.ActionMessage;
import net.solarnetwork.ocpp.domain.ChargePointConnectorKey;
import net.solarnetwork.ocpp.domain.ChargePointErrorCode;
import net.solarnetwork.ocpp.domain.ChargePointIdentity;
import net.solarnetwork.ocpp.domain.ChargePointStatus;
import net.solarnetwork.ocpp.domain.StatusNotification;
import net.solarnetwork.ocpp.service.ActionMessageProcessor;
import net.solarnetwork.ocpp.service.ActionMessageResultHandler;
import net.solarnetwork.ocpp.service.ChargePointBroker;
import net.solarnetwork.ocpp.service.ChargePointRouter;
import ocpp.v16.ChargePointAction;
import ocpp.v16.cp.AvailabilityStatus;
import ocpp.v16.cp.AvailabilityType;
import ocpp.v16.cp.ChangeAvailabilityRequest;
import ocpp.v16.cp.ChangeAvailabilityResponse;
import ocpp.v16.cp.json.ChargePointActionPayloadDecoder;

/**
 * Test cases for the {@link OcppController} class.
 * 
 * @author matt
 * @version 2.0
 */
public class OcppControllerTests {

	private ChargePointRouter chargePointRouter;
	private UserNodeDao userNodeDao;
	private NodeInstructionDao instructionDao;
	private CentralAuthorizationDao authorizationDao;
	private CentralChargePointDao chargePointDao;
	private CentralChargePointConnectorDao chargePointConnectorDao;
	private ActionMessageProcessor<JsonNode, Void> instructionHandler;

	private ChargePointBroker chargePointBroker;

	private OcppController controller;

	private final Logger log = LoggerFactory.getLogger(getClass());

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		chargePointRouter = EasyMock.createMock(ChargePointRouter.class);
		userNodeDao = EasyMock.createMock(UserNodeDao.class);
		instructionDao = EasyMock.createMock(NodeInstructionDao.class);
		authorizationDao = EasyMock.createMock(CentralAuthorizationDao.class);
		chargePointDao = EasyMock.createMock(CentralChargePointDao.class);
		chargePointConnectorDao = EasyMock.createMock(CentralChargePointConnectorDao.class);
		instructionHandler = EasyMock.createMock(ActionMessageProcessor.class);

		chargePointBroker = EasyMock.createMock(ChargePointBroker.class);

		controller = new OcppController(new CallingThreadExecutorService(), chargePointRouter,
				userNodeDao, instructionDao, authorizationDao, chargePointDao, chargePointConnectorDao);
		controller.setChargePointActionPayloadDecoder(new ChargePointActionPayloadDecoder());
	}

	@After
	public void teardown() {
		EasyMock.verify(chargePointRouter, userNodeDao, instructionDao, authorizationDao, chargePointDao,
				chargePointConnectorDao, chargePointBroker, instructionHandler);
	}

	private void replayAll() {
		EasyMock.replay(chargePointRouter, userNodeDao, instructionDao, authorizationDao, chargePointDao,
				chargePointConnectorDao, chargePointBroker, instructionHandler);
	}

	@Test
	public void updateStatus() {
		// GIVEN
		Long nodeId = randomUUID().getMostSignificantBits();
		Long userId = randomUUID().getMostSignificantBits();
		ChargePointIdentity identity = new ChargePointIdentity(randomUUID().toString(), userId);

		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(), userId,
				nodeId);
		expect(chargePointDao.getForIdentity(identity)).andReturn(cp);

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(1)
				.withStatus(ChargePointStatus.Charging)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now())
				.withInfo("Hello.")
				.withVendorId("SolarNetwork")
				.build();
		// @formatter:on
		expect(chargePointConnectorDao.saveStatusInfo(cp.getId().longValue(), info))
				.andReturn(new ChargePointConnectorKey(cp.getId().longValue(), 1));

		// WHEN
		replayAll();
		controller.updateChargePointStatus(identity, info);
	}

	@Test
	public void updateStatus_conn0() {
		// GIVEN
		Long nodeId = randomUUID().getMostSignificantBits();
		Long userId = randomUUID().getMostSignificantBits();
		ChargePointIdentity identity = new ChargePointIdentity(randomUUID().toString(), userId);

		CentralChargePoint cp = new CentralChargePoint(randomUUID().getMostSignificantBits(), userId,
				nodeId);
		expect(chargePointDao.getForIdentity(identity)).andReturn(cp);

		// @formatter:off
		StatusNotification info = StatusNotification.builder()
				.withConnectorId(0)
				.withStatus(ChargePointStatus.Available)
				.withErrorCode(ChargePointErrorCode.NoError)
				.withTimestamp(Instant.now())
				.withInfo("Hello.")
				.withVendorId("SolarNetwork")
				.build();
		// @formatter:on
		expect(chargePointConnectorDao.saveStatusInfo(cp.getId().longValue(), info))
				.andReturn(new ChargePointConnectorKey(cp.getId().longValue(), 1));

		// WHEN
		replayAll();
		controller.updateChargePointStatus(identity, info);
	}

	@Test
	public void handleInstruction_toggleConnectorAvailability() {
		// GIVEN
		Long nodeId = randomUUID().getMostSignificantBits();
		NodeInstruction instruction = new NodeInstruction(OCPP_V16_TOPIC, Instant.now(), nodeId);
		String chargerIdentity = randomUUID().toString();
		instruction.addParameter(OCPP_CHARGER_IDENTIFIER_PARAM, chargerIdentity);
		instruction.addParameter(OCPP_ACTION_PARAM, ChargePointAction.ChangeAvailability.getName());
		instruction.addParameter("connectorId", "1");
		instruction.addParameter("type", AvailabilityType.INOPERATIVE.value());
		Long instructionId = randomUUID().getMostSignificantBits();

		UserNode userNode = new UserNode(
				new User(randomUUID().getMostSignificantBits(), "test@localhost"),
				new SolarNode(nodeId, randomUUID().getMostSignificantBits()));
		expect(userNodeDao.get(nodeId)).andReturn(userNode);

		CentralChargePoint cp = new CentralChargePoint(userNode.getUserId(), nodeId, Instant.now(),
				chargerIdentity, "SolarNetwork", "SolarNode");
		expect(chargePointDao.getForIdentifier(userNode.getUserId(), chargerIdentity)).andReturn(cp);

		ChargePointIdentity cpIdent = new ChargePointIdentity(chargerIdentity, userNode.getUserId());
		expect(chargePointRouter.brokerForChargePoint(cpIdent)).andReturn(chargePointBroker);

		Capture<ActionMessage<Object>> actionCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> resultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionCaptor),
				capture(resultHandlerCaptor))).andAnswer(new IAnswer<Boolean>() {

					@Override
					public Boolean answer() throws Throwable {
						// invoke result handler
						ActionMessage<Object> message = actionCaptor.getValue();
						assertThat("Message sent to charge point is ChangeAvailability",
								message.getMessage(), instanceOf(ChangeAvailabilityRequest.class));
						ActionMessageResultHandler<Object, Object> resultHandler = resultHandlerCaptor
								.getValue();
						ChangeAvailabilityResponse res = new ChangeAvailabilityResponse();
						res.setStatus(AvailabilityStatus.ACCEPTED);
						boolean handlerResult = resultHandler.handleActionMessageResult(message, res,
								null);
						assertThat("Result handled", handlerResult, equalTo(true));
						return true;
					}
				});

		Capture<Map<String, Object>> resultParamsCaptor = new Capture<>();
		expect(instructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(nodeId),
				eq(InstructionState.Executing), eq(InstructionState.Completed),
				capture(resultParamsCaptor))).andReturn(true);

		// WHEN
		replayAll();
		NodeInstruction instr = controller.willQueueNodeInstruction(instruction);
		controller.didQueueNodeInstruction(instr, instructionId);

		// THEN
		log.debug("Instruction result parameters: {}", instr.getResultParameters());
		assertThat("Instruction executing", instr.getState(), equalTo(InstructionState.Executing));
		assertThat("Result parameters has accepted result", resultParamsCaptor.getValue(),
				hasEntry("status", AvailabilityStatus.ACCEPTED.value()));
	}

	@Test
	public void handleInstruction_toggleConnectorAvailability_msgParam() {
		// GIVEN
		Long nodeId = randomUUID().getMostSignificantBits();
		NodeInstruction instruction = new NodeInstruction(OCPP_V16_TOPIC, Instant.now(), nodeId);
		String chargerIdentity = randomUUID().toString();
		instruction.addParameter(OCPP_CHARGER_IDENTIFIER_PARAM, chargerIdentity);
		instruction.addParameter(OCPP_ACTION_PARAM, ChargePointAction.ChangeAvailability.getName());
		instruction.addParameter(OCPP_MESSAGE_PARAM, String.format("{\"connectorId\":1,\"type\":\"%s\"}",
				AvailabilityType.INOPERATIVE.value()));
		Long instructionId = randomUUID().getMostSignificantBits();

		UserNode userNode = new UserNode(
				new User(randomUUID().getMostSignificantBits(), "test@localhost"),
				new SolarNode(nodeId, randomUUID().getMostSignificantBits()));
		expect(userNodeDao.get(nodeId)).andReturn(userNode);

		CentralChargePoint cp = new CentralChargePoint(userNode.getUserId(), nodeId, Instant.now(),
				chargerIdentity, "SolarNetwork", "SolarNode");
		expect(chargePointDao.getForIdentifier(userNode.getUserId(), chargerIdentity)).andReturn(cp);

		ChargePointIdentity cpIdent = new ChargePointIdentity(chargerIdentity, userNode.getUserId());
		expect(chargePointRouter.brokerForChargePoint(cpIdent)).andReturn(chargePointBroker);

		Capture<ActionMessage<Object>> actionCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<Object, Object>> resultHandlerCaptor = new Capture<>();
		expect(chargePointBroker.sendMessageToChargePoint(capture(actionCaptor),
				capture(resultHandlerCaptor))).andAnswer(new IAnswer<Boolean>() {

					@Override
					public Boolean answer() throws Throwable {
						// invoke result handler
						ActionMessage<Object> message = actionCaptor.getValue();
						assertThat("Message sent to charge point is ChangeAvailability",
								message.getMessage(), instanceOf(ChangeAvailabilityRequest.class));
						ActionMessageResultHandler<Object, Object> resultHandler = resultHandlerCaptor
								.getValue();
						ChangeAvailabilityResponse res = new ChangeAvailabilityResponse();
						res.setStatus(AvailabilityStatus.ACCEPTED);
						boolean handlerResult = resultHandler.handleActionMessageResult(message, res,
								null);
						assertThat("Result handled", handlerResult, equalTo(true));
						return true;
					}
				});

		Capture<Map<String, Object>> resultParamsCaptor = new Capture<>();
		expect(instructionDao.compareAndUpdateInstructionState(eq(instructionId), eq(nodeId),
				eq(InstructionState.Executing), eq(InstructionState.Completed),
				capture(resultParamsCaptor))).andReturn(true);

		// WHEN
		replayAll();
		NodeInstruction instr = controller.willQueueNodeInstruction(instruction);
		controller.didQueueNodeInstruction(instr, instructionId);

		// THEN
		log.debug("Instruction result parameters: {}", instr.getResultParameters());
		assertThat("Instruction executing", instr.getState(), equalTo(InstructionState.Executing));
		assertThat("Result parameters has accepted result", resultParamsCaptor.getValue(),
				hasEntry("status", AvailabilityStatus.ACCEPTED.value()));
	}

	@Test
	public void handleInstruction_toggleConnectorAvailability_withInstructionHandler() throws Exception {
		// GIVEN
		controller.setInstructionHandler(instructionHandler);
		Long nodeId = randomUUID().getMostSignificantBits();
		NodeInstruction instruction = new NodeInstruction(OCPP_V16_TOPIC, Instant.now(), nodeId);
		String chargerIdentity = randomUUID().toString();
		instruction.addParameter(OCPP_CHARGER_IDENTIFIER_PARAM, chargerIdentity);
		instruction.addParameter(OCPP_ACTION_PARAM, ChargePointAction.ChangeAvailability.getName());
		instruction.addParameter("connectorId", "1");
		instruction.addParameter("type", AvailabilityType.INOPERATIVE.value());
		Long instructionId = randomUUID().getMostSignificantBits();

		UserNode userNode = new UserNode(
				new User(randomUUID().getMostSignificantBits(), "test@localhost"),
				new SolarNode(nodeId, randomUUID().getMostSignificantBits()));
		expect(userNodeDao.get(nodeId)).andReturn(userNode);

		CentralChargePoint cp = new CentralChargePoint(userNode.getUserId(), nodeId, Instant.now(),
				chargerIdentity, "SolarNetwork", "SolarNode");
		expect(chargePointDao.getForIdentifier(userNode.getUserId(), chargerIdentity)).andReturn(cp);

		Capture<ActionMessage<JsonNode>> messageCaptor = new Capture<>();
		Capture<ActionMessageResultHandler<JsonNode, Void>> handlerCaptor = new Capture<>();
		instructionHandler.processActionMessage(capture(messageCaptor), capture(handlerCaptor));

		// WHEN
		replayAll();
		NodeInstruction instr = controller.willQueueNodeInstruction(instruction);
		instr.setId(instructionId);
		controller.didQueueNodeInstruction(instr, instructionId);
		handlerCaptor.getValue().handleActionMessageResult(messageCaptor.getValue(), null, null);

		// THEN
		log.debug("Instruction result parameters: {}", instr.getResultParameters());
		assertThat("Instruction received", instr.getState(), equalTo(InstructionState.Received));
		ActionMessage<JsonNode> message = messageCaptor.getValue();
		assertThat("Message action", message.getAction(), equalTo(ChargePointAction.ChangeAvailability));
		assertThat("Message ID is instruction ID", message.getMessageId(),
				equalTo(instructionId.toString()));
		assertThat("Message client ID", message.getClientId(), equalTo(cp.chargePointIdentity()));
		JsonNode json = message.getMessage();
		ChangeAvailabilityRequest req = controller.getObjectMapper().treeToValue(json,
				ChangeAvailabilityRequest.class);
		assertThat("Message payload connector ID", req.getConnectorId(), equalTo(1));
		assertThat("Message payload type", req.getType(), equalTo(AvailabilityType.INOPERATIVE));
	}

}
