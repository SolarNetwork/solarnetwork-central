/* ==================================================================
 * CloudControlInstructionQueueHookTests.java - 14/11/2025 3:12:36 pm
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

package net.solarnetwork.central.c2c.support.test;

import static java.time.Instant.now;
import static java.util.Map.entry;
import static net.solarnetwork.central.c2c.biz.CommonInstructionTopic.SetControlParameter;
import static net.solarnetwork.central.c2c.support.CloudControlInstructionQueueHook.ERROR_CODE_INSTRUCTION_NOT_HANDLED;
import static net.solarnetwork.central.c2c.support.CloudControlInstructionQueueHook.ERROR_CODE_INSTRUCTION_THREW_EXCEPTION;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Declined;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queued;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queuing;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudControlService;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudControlFilter;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.c2c.support.CloudControlInstructionCountStat;
import net.solarnetwork.central.c2c.support.CloudControlInstructionQueueHook;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.jackson.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.domain.BasicInstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;
import net.solarnetwork.util.StatTracker;

/**
 * Test cases for the {@link CloudControlInstructionQueueHook} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class CloudControlInstructionQueueHookTests implements CloudIntegrationsUserEvents {

	private static final Logger log = LoggerFactory
			.getLogger(CloudControlInstructionQueueHookTests.class);

	private static final String CONTROL_SERVICE_ID = randomString();

	private static final String OTHER_TOPIC = "SomethingFancy";

	@Mock
	private SolarNodeOwnershipDao nodeOwnershipDao;

	@Mock
	private CloudControlConfigurationDao controlDao;

	@Mock
	private NodeInstructionDao nodeInstructionDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private CloudControlService controlService;

	@Captor
	private ArgumentCaptor<CloudControlFilter> filterCaptor;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> stateUpdateMapCaptor;

	private StatTracker stats;
	private CloudControlInstructionQueueHook hook;

	@BeforeEach
	public void setup() {
		// set up some mock integration required by hook constructor
		when(controlService.getId()).thenReturn(CONTROL_SERVICE_ID);
		when(controlService.supportedTopics())
				.thenReturn(new LinkedHashSet<>(List.of(SetControlParameter.name(), OTHER_TOPIC)));

		stats = new StatTracker("CloudControl", null, log, 1);
		hook = new CloudControlInstructionQueueHook(stats, nodeOwnershipDao, controlDao,
				nodeInstructionDao, List.of(controlService));
		hook.setUserEventAppenderBiz(userEventAppenderBiz);
	}

	private SolarNodeOwnership randomOwnership() {
		return ownershipFor(randomLong(), randomLong());
	}

	@Test
	public void unsupportedTopic() {
		// GIVEN

		// WHEN
		final NodeInstruction instruction = new NodeInstruction("something/else", now(), randomLong());
		instruction.getInstruction().setState(Queued);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		// @formatter:off
		then(userEventAppenderBiz).shouldHaveNoInteractions();

		and.then(result)
			.as("Result is same instruction as given")
			.isSameAs(instruction)
			;

		and.then(result.getInstruction())
			.as("Instruction status unchanged for unsupported topic")
			.returns(Queued, Instruction::getState)
			;

		and.then(stats.allCounts())
			.as("No stats affected")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void unknownOwner() {
		// GIVEN

		final Long nodeId = randomLong();

		// look up owner for instruction node ID
		given(nodeOwnershipDao.ownershipForNodeId(nodeId)).willReturn(null);

		// WHEN
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				nodeId);
		instruction.getInstruction().setState(Queued);

		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		// @formatter:off
		then(userEventAppenderBiz).shouldHaveNoInteractions();

		and.then(result)
			.as("Result is same instruction as given")
			.isSameAs(instruction)
			;

		and.then(result.getInstruction())
			.as("Instruction status unchanged for unknown owner")
			.returns(Queued, Instruction::getState)
			;

		and.then(stats.allCounts())
			.as("No stats affected")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void noCloudControl() {
		// GIVEN

		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control, none found
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of()));

		final String controlId = randomString();
		final String controlValue = randomString();

		// WHEN
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(controlId, controlValue));

		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		and.then(filterCaptor.getValue())
			.as("Owner user ID passed in control filter")
			.returns(new Long[] { owner.getUserId() }, CloudControlFilter::getUserIds)
			.as("Instruction node ID passed in control filter")
			.returns(new Long[] { owner.getNodeId() }, CloudControlFilter::getNodeIds)
			.as("Control ID extracted from SetControlParameter parameter key")
			.returns(new String[] { controlId }, CloudControlFilter::getControlIds);
			;

		then(userEventAppenderBiz).shouldHaveNoInteractions();

		and.then(result)
			.as("Result is same instruction as given")
			.isSameAs(instruction)
			;

		and.then(result.getInstruction())
			.as("Instruction status unchanged for no cloud control")
			.returns(Queued, from(Instruction::getState))
			;

		and.then(stats.allCounts())
			.as("No stats affected")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void inspect_notQueued() {
		// GIVEN
		final NodeInstruction instruction = new NodeInstruction("foo", now(), randomLong());

		// WHEN
		for ( InstructionState s : EnumSet.complementOf(EnumSet.of(InstructionState.Queued)) ) {
			instruction.getInstruction().setState(s);
			NodeInstruction result = hook.willQueueNodeInstruction(instruction);

			// @formatter:off
			and.then(result)
				.as("Input state %s is ignored", s)
				.isSameAs(instruction)
				;
			// @formatter:on
		}
	}

	private CloudControlConfiguration newControl(SolarNodeOwnership owner) {
		final CloudControlConfiguration control = new CloudControlConfiguration(owner.getUserId(),
				randomLong(), now());
		control.setServiceIdentifier(CONTROL_SERVICE_ID);
		control.setIntegrationId(randomLong());
		control.setNodeId(owner.getNodeId());
		control.setControlId(randomString());
		control.setEnabled(true);
		control.setControlReference(randomString());
		return control;
	}

	@Test
	public void inspect_SetControlParameter() {
		// GIVEN

		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control
		final CloudControlConfiguration control = newControl(owner);
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(control)));

		// WHEN
		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		and.then(filterCaptor.getValue())
			.as("Owner user ID passed in control filter")
			.returns(new Long[] { owner.getUserId() }, CloudControlFilter::getUserIds)
			.as("Instruction node ID passed in control filter")
			.returns(new Long[] { owner.getNodeId() }, CloudControlFilter::getNodeIds)
			.as("Control ID extracted from SetControlParameter parameter key")
			.returns(new String[] { control.getControlId() }, CloudControlFilter::getControlIds);
			;

		then(userEventAppenderBiz).shouldHaveNoInteractions();

		and.then(result)
			.as("Result is NOT same instruction as given")
			.isNotSameAs(instruction)
			;

		and.then(result.getInstruction())
			.as("Instruction status changed to Queueing")
			.returns(Queuing, from(Instruction::getState))
			;

		and.then(stats.allCounts())
			.as("No stats affected")
			.isEmpty()
			;
		// @formatter:on
	}

	private Map<String, Long> instructionStatCounts(String topic, InstructionState resultState) {
		// @formatter:off
		return Map.ofEntries(
				  entry(CloudControlInstructionCountStat.InstructionsExecuted.name(), 1L)
				, entry(topic + "Executed", 1L)
				, entry(topic + resultState.name(), 1L)
			);
		// @formatter:on
	}

	private Map<String, Long> instructionStatErrorCounts(String topic) {
		// @formatter:off
		return Map.ofEntries(
				  entry(CloudControlInstructionCountStat.InstructionsExecuted.name(), 1L)
				, entry(CloudControlInstructionCountStat.InstructionErrors.name(), 1L)
				, entry(topic + "Executed", 1L)
				, entry(topic + "Errors", 1L)
			);
		// @formatter:on
	}

	private Map<String, Object> executionEventData(CloudControlConfiguration control, Long instructionId,
			InstructionState state) {
		return executionEventData(control, instructionId, state, null);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> executionEventData(CloudControlConfiguration control, Long instructionId,
			InstructionState state, String errorCode) {
		// @formatter:off
		List<Entry<String, Object>> entries = new ArrayList<>(List.of(
				  entry(CONFIG_ID_DATA_KEY, control.getConfigId())
				, entry(INTEGRATION_ID_DATA_KEY, control.getIntegrationId())
				, entry(INSTRUCTION_ID_DATA_KEY, instructionId)
				, entry(INSTRUCTION_STATE_DATA_KEY, state.name())
				));
		// @formatter:on

		if ( errorCode != null ) {
			entries.add(entry(ERROR_CODE_DATA_KEY, errorCode));
		}

		return Map.ofEntries(entries.toArray(Entry[]::new));
	}

	@Test
	public void execute_SetControlParameter_exception() {
		// GIVEN
		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control
		final CloudControlConfiguration control = newControl(owner);
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(control)));

		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		// execute instruction
		RuntimeException e = new RuntimeException("Boom");
		given(controlService.executeInstruction(eq(control.getId()), any())).willThrow(e);

		// update instruction state
		final Long instructionId = randomLong();
		given(nodeInstructionDao.compareAndUpdateInstructionState(eq(instructionId),
				eq(instruction.getNodeId()), eq(Queuing), eq(Declined), any())).willReturn(true);

		// WHEN
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		then(nodeInstructionDao).should().compareAndUpdateInstructionState(eq(instructionId), eq(instruction.getNodeId()),
				eq(Queuing), eq(Declined), stateUpdateMapCaptor.capture());
		and.then(stateUpdateMapCaptor.getValue())
			.as("Instruction state includes result message")
			.containsOnlyKeys("message")
			.extractingByKey("message", STRING)
			.as("Result message contains exception message")
			.contains(e.getMessage())
			;

		and.then(result.getInstruction())
			.as("Instruction state updated to Declined after exception")
			.returns(Declined, from(Instruction::getState))
			.extracting(Instruction::getResultParameters, map(String.class, Object.class))
			.as("Instruction result parameters include result message")
			.containsOnlyKeys("message")
			.extractingByKey("message", STRING)
			.as("Result message contains exception message")
			.contains(e.getMessage())
			;

		then(userEventAppenderBiz).should().addEvent(eq(owner.getUserId()), eventCaptor.capture());
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(stats.allCounts())
			.as("Stats updated with execution counts")
			.containsExactlyInAnyOrderEntriesOf(instructionStatErrorCounts(SetControlParameter.name()))
			;

		and.then(eventCaptor.getValue())
			.as("Event tags for control instructions")
			.returns(INTEGRATION_CONTROL_INSTRUCTION_ERROR_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(executionEventData(control, instructionId, Declined, ERROR_CODE_INSTRUCTION_THREW_EXCEPTION))
			;
		// @formatter:on
	}

	@Test
	public void execute_SetControlParameter_Completed() {
		// GIVEN
		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control
		final CloudControlConfiguration control = newControl(owner);
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(control)));

		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		// execute instruction
		BasicInstructionStatus status = new BasicInstructionStatus(
				instruction != null ? instruction.getId() : null, Completed, now(), null);
		given(controlService.executeInstruction(eq(control.getId()), any())).willReturn(status);

		// update instruction state
		final Long instructionId = randomLong();
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, instruction.getNodeId(),
				Queuing, status.getInstructionState(), status.getResultParameters())).willReturn(true);

		// WHEN
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		then(userEventAppenderBiz).should().addEvent(eq(owner.getUserId()), eventCaptor.capture());
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(result.getInstruction())
			.as("Instruction state updated from result status")
			.returns(status.getInstructionState(), from(Instruction::getState))
			;

		and.then(stats.allCounts())
			.as("Stats updated with execution counts")
			.containsExactlyInAnyOrderEntriesOf(instructionStatCounts(SetControlParameter.name(), Completed))
			;

		and.then(eventCaptor.getValue())
			.as("Event tags for control instructions")
			.returns(INTEGRATION_CONTROL_INSTRUCTION_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(executionEventData(control, instructionId, status.getInstructionState()))
			;
		// @formatter:on
	}

	@Test
	public void execute_SetControlParameter_Declined() {
		// GIVEN
		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control
		final CloudControlConfiguration control = newControl(owner);
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(control)));

		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		// execute instruction
		BasicInstructionStatus status = new BasicInstructionStatus(
				instruction != null ? instruction.getId() : null, Declined, now(), null);
		given(controlService.executeInstruction(eq(control.getId()), any())).willReturn(status);

		// update instruction state
		final Long instructionId = randomLong();
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, instruction.getNodeId(),
				Queuing, status.getInstructionState(), status.getResultParameters())).willReturn(true);

		// WHEN
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		then(userEventAppenderBiz).should().addEvent(eq(owner.getUserId()), eventCaptor.capture());
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(result.getInstruction())
			.as("Instruction state updated from result status")
			.returns(status.getInstructionState(), from(Instruction::getState))
			;

		and.then(stats.allCounts())
			.as("Stats updated with execution counts")
			.containsExactlyInAnyOrderEntriesOf(instructionStatCounts(SetControlParameter.name(), Declined))
			;

		and.then(eventCaptor.getValue())
			.as("Event tags for control instructions")
			.returns(INTEGRATION_CONTROL_INSTRUCTION_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(executionEventData(control, instructionId, status.getInstructionState()))
			;
		// @formatter:on
	}

	@Test
	public void execute_SetControlParameter_Queuing() {
		// GIVEN
		// look up owner for instruction node ID
		final SolarNodeOwnership owner = randomOwnership();
		given(nodeOwnershipDao.ownershipForNodeId(owner.getNodeId())).willReturn(owner);

		// look up control
		final CloudControlConfiguration control = newControl(owner);
		given(controlDao.findFiltered(any())).willReturn(new BasicFilterResults<>(List.of(control)));

		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		// execute instruction
		BasicInstructionStatus status = new BasicInstructionStatus(
				instruction != null ? instruction.getId() : null, Queuing, now(), null);
		given(controlService.executeInstruction(eq(control.getId()), any())).willReturn(status);

		// update instruction state to Declined because state did not change from Queuing
		final Long instructionId = randomLong();
		given(nodeInstructionDao.compareAndUpdateInstructionState(instructionId, instruction.getNodeId(),
				Queuing, Declined, status.getResultParameters())).willReturn(true);

		// WHEN
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);
		hook.didQueueNodeInstruction(result, instructionId);

		// THEN
		// @formatter:off
		then(controlDao).should().findFiltered(filterCaptor.capture());
		then(controlDao).shouldHaveNoMoreInteractions();

		then(userEventAppenderBiz).should().addEvent(eq(owner.getUserId()), eventCaptor.capture());
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(result.getInstruction())
			.as("Instruction state updated to Declined if unchanged from Queuing")
			.returns(Declined, from(Instruction::getState))
			;

		and.then(stats.allCounts())
			.as("Stats updated with execution counts")
			.containsExactlyInAnyOrderEntriesOf(instructionStatCounts(SetControlParameter.name(), Declined))
			;

		and.then(eventCaptor.getValue())
			.as("Event tags for control instructions")
			.returns(INTEGRATION_CONTROL_INSTRUCTION_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(executionEventData(control, instructionId, Declined, ERROR_CODE_INSTRUCTION_NOT_HANDLED))
			;
		// @formatter:on
	}

}
