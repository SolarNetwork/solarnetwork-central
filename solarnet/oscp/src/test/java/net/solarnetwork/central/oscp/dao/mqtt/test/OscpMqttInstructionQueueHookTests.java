/* ==================================================================
 * OscpMqttInstructionQueueHookTests.java - 8/10/2022 5:53:23 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao.mqtt.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static net.solarnetwork.central.instructor.domain.InstructionState.Executing;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_ACTION_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_CAPACITY_OPTIMIZER_ID_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_MESSAGE_PARAM;
import static net.solarnetwork.central.oscp.util.OscpInstructionUtils.OSCP_V20_TOPIC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import java.time.Instant;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.domain.LogEventInfo;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructionQueueHook;
import net.solarnetwork.central.oscp.mqtt.OscpMqttInstructions;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.User;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.common.mqtt.MqttConnection;
import oscp.v20.GroupCapacityComplianceError;

/**
 * Test cases for the {@link OscpMqttInstructionQueueHook} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class OscpMqttInstructionQueueHookTests implements OscpMqttInstructions {

	private static final Long TEST_NODE_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_LOC_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_USER_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CO_ID = UUID.randomUUID().getMostSignificantBits();
	private static final Long TEST_CP_ID = UUID.randomUUID().getMostSignificantBits();
	private static final String TEST_CG_IDENT = UUID.randomUUID().toString();

	private static final Logger log = LoggerFactory.getLogger(OscpMqttInstructionQueueHookTests.class);

	@Captor
	private ArgumentCaptor<LogEventInfo> eventCaptor;

	@Mock
	private UserNodeDao userNodeDao;

	@Mock
	private CapacityGroupConfigurationDao capacityGroupDao;

	@Mock
	private CapacityOptimizerConfigurationDao capacityOptimizerDao;

	@Mock
	private CapacityProviderConfigurationDao capacityProviderDao;

	@Mock
	private UserEventAppenderBiz userEventAppenderBiz;

	@Mock
	private MqttConnection conn;

	private ObjectMapper mapper;
	private OscpMqttInstructionQueueHook hook;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newDatumObjectMapper();
		hook = new OscpMqttInstructionQueueHook(mapper, userNodeDao, capacityGroupDao,
				capacityOptimizerDao, capacityProviderDao);
		hook.setUserEventAppenderBiz(userEventAppenderBiz);
	}

	@Test
	public void processInstruction() {
		// GIVEN
		final NodeInstruction instruction = new NodeInstruction(OSCP_V20_TOPIC, Instant.now(),
				TEST_NODE_ID);
		instruction
				.setParams(Map.of(OSCP_ACTION_PARAM, GroupCapacityComplianceError.class.getSimpleName(),
						OSCP_CAPACITY_OPTIMIZER_ID_PARAM, TEST_CO_ID.toString(),
						OSCP_CAPACITY_GROUP_IDENTIFIER_PARAM, TEST_CG_IDENT, OSCP_MESSAGE_PARAM, """
								{
										"message":"Oops!",
										"forecasted_blocks":[
											{
												"capacity"   : 123.456,
												"phase"      : "ALL",
												"unit"       : "KW",
												"start_time" : "2022-10-08T18:00:00Z",
												"end_time"   : "2022-10-08T18:15:00Z"
											}
										]
								}
								"""));
		UserNode userNode = new UserNode(new User(TEST_USER_ID, "user@localhost"),
				new SolarNode(TEST_NODE_ID, TEST_LOC_ID));
		given(userNodeDao.get(TEST_NODE_ID)).willReturn(userNode);

		CapacityOptimizerConfiguration optimizer = new CapacityOptimizerConfiguration(TEST_USER_ID,
				TEST_CO_ID, Instant.now());
		optimizer.setEnabled(true);
		given(capacityOptimizerDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CO_ID)))
				.willReturn(optimizer);

		CapacityGroupConfiguration group = new CapacityGroupConfiguration(TEST_USER_ID,
				UUID.randomUUID().getMostSignificantBits(), Instant.now());
		group.setCapacityOptimizerId(TEST_CO_ID);
		group.setCapacityProviderId(TEST_CP_ID);
		group.setEnabled(true);
		given(capacityGroupDao.findForCapacityOptimizer(TEST_USER_ID, TEST_CO_ID, TEST_CG_IDENT))
				.willReturn(group);

		CapacityProviderConfiguration provider = new CapacityProviderConfiguration(TEST_USER_ID,
				TEST_CP_ID, Instant.now());
		provider.setEnabled(true);
		given(capacityProviderDao.get(new UserLongCompositePK(TEST_USER_ID, TEST_CP_ID)))
				.willReturn(provider);

		// publish to MQTT
		given(conn.isEstablished()).willReturn(true);
		given(conn.publish(any())).willReturn(completedFuture(null));

		// WHEN
		hook.onMqttServerConnectionEstablished(conn, false);
		NodeInstruction result = hook.willQueueNodeInstruction(instruction);

		// THEN
		then(userEventAppenderBiz).should(atLeastOnce()).addEvent(eq(TEST_USER_ID),
				eventCaptor.capture());
		List<LogEventInfo> events = eventCaptor.getAllValues();
		log.debug("Got events: {}", events);

		assertThat("Result is same instance", result, is(sameInstance(instruction)));
		assertThat("Instruction status updated for execution", result.getState(),
				is(equalTo(Executing)));

	}

}
