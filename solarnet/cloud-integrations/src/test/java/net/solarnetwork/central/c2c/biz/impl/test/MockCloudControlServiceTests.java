/* ==================================================================
 * MockCloudControlServiceTests.java - 15/11/2025 2:03:58 pm
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

package net.solarnetwork.central.c2c.biz.impl.test;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Map.entry;
import static net.solarnetwork.central.c2c.biz.CommonInstructionTopic.SetControlParameter;
import static net.solarnetwork.central.domain.BasicSolarNodeOwnership.ownershipFor;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Completed;
import static net.solarnetwork.domain.InstructionStatus.InstructionState.Queued;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.threeten.extra.MutableClock;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudControlService;
import net.solarnetwork.central.c2c.biz.impl.MockCloudControlService;
import net.solarnetwork.central.c2c.biz.impl.MockCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudControlConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudControlConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.InstructionStatus;
import net.solarnetwork.domain.InstructionStatus.InstructionState;

/**
 * Test cases for the {@link MockCloudControlService} class.
 *
 * @author matt
 * @version 1.0
 */
@SuppressWarnings("static-access")
@ExtendWith(MockitoExtension.class)
public class MockCloudControlServiceTests implements CloudIntegrationsUserEvents {

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private TextEncryptor encryptor;

	@Mock
	private CloudIntegrationConfigurationDao integrationDao;

	@Mock
	private CloudControlConfigurationDao controlDao;

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	private MutableClock clock;

	private MockCloudControlService service;

	@BeforeEach
	public void setup() {
		clock = MutableClock.of(Instant.now(), UTC);
		service = new MockCloudControlService(userEventAppenderBiz, encryptor, clock, integrationDao,
				controlDao);

		ResourceBundleMessageSource msg = new ResourceBundleMessageSource();
		msg.setBasenames(MockCloudControlService.class.getName(),
				BaseCloudControlService.class.getName());
		service.setMessageSource(msg);
	}

	private SolarNodeOwnership randomOwnership() {
		return ownershipFor(randomLong(), randomLong());
	}

	private CloudControlConfiguration newControl(SolarNodeOwnership owner) {
		final CloudControlConfiguration control = new CloudControlConfiguration(owner.getUserId(),
				randomLong(), now());
		control.setServiceIdentifier(MockCloudControlService.SERVICE_IDENTIFIER);
		control.setIntegrationId(randomLong());
		control.setNodeId(owner.getNodeId());
		control.setControlId(randomString());
		control.setEnabled(true);
		control.setControlReference(randomString());
		return control;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> executionEventData(CloudControlConfiguration control,
			NodeInstruction instruction, InstructionState state) {
		// @formatter:off
		List<Entry<String, Object>> entries = new ArrayList<>(List.of(
				  entry(CONFIG_ID_DATA_KEY, control.getConfigId())
				, entry(INTEGRATION_ID_DATA_KEY, control.getIntegrationId())
				, entry(INSTRUCTION_ID_DATA_KEY, instruction.getId())
				, entry(INSTRUCTION_STATE_DATA_KEY, state.name())
				, entry(INSTRUCTION_TOPIC_DATA_KEY, instruction.getInstruction().getTopic())
				, entry(INSTRUCTION_DATA_KEY, JsonUtils.getStringMap(JsonUtils.getJSONString(instruction, null)))
				));
		// @formatter:on

		return Map.ofEntries(entries.toArray(Entry[]::new));
	}

	@Test
	public void execute_SetControlParameter() {
		// GIVEN
		final SolarNodeOwnership owner = randomOwnership();
		final CloudControlConfiguration control = newControl(owner);

		// look up control
		given(controlDao.get(control.getId())).willReturn(control);

		// look up associated integration
		CloudIntegrationConfiguration integration = new CloudIntegrationConfiguration(
				control.getUserId(), control.getIntegrationId(), now());
		integration.setServiceIdentifier(MockCloudIntegrationService.SERVICE_IDENTIFIER);
		given(integrationDao
				.get(new UserLongCompositePK(control.getUserId(), control.getIntegrationId())))
						.willReturn(integration);

		final String controlValue = randomString();
		final NodeInstruction instruction = new NodeInstruction(SetControlParameter.name(), now(),
				owner.getNodeId());
		instruction.setId(randomLong());
		instruction.getInstruction().setState(Queued);
		instruction.getInstruction().setParams(Map.of(control.getControlId(), controlValue));

		// WHEN
		InstructionStatus result = service.executeInstruction(control.getId(), instruction);

		// THEN
		// @formatter:off
		and.then(result)
			.as("Result provided")
			.isNotNull()
			.as("State is Completed")
			.returns(Completed, from(InstructionStatus::getInstructionState))
			;

		then(userEventAppenderBiz).should().addEvent(eq(owner.getUserId()), eventCaptor.capture());
		then(userEventAppenderBiz).shouldHaveNoMoreInteractions();

		and.then(eventCaptor.getValue())
			.as("Event tags for control instructions")
			.returns(INTEGRATION_CONTROL_INSTRUCTION_TAGS.toArray(String[]::new), from(LogEventInfo::getTags))
			.as("Event data is JSON object")
			.extracting(event -> JsonUtils.getStringMap(event.getData()), map(String.class, Object.class))
			.as("Event data values")
			.containsExactlyInAnyOrderEntriesOf(executionEventData(control, instruction, Completed))
			;
		// @formatter:on
	}

}
