/* ==================================================================
 * MqttConnectionDebugger.java - 9/06/2023 8:02:35 am
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.mqttconntest.impl;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import net.solarnetwork.central.support.BaseMqttConnectionObserver;
import net.solarnetwork.common.mqtt.MqttConnection;
import net.solarnetwork.common.mqtt.MqttMessage;
import net.solarnetwork.common.mqtt.MqttMessageHandler;
import net.solarnetwork.common.mqtt.MqttQos;
import net.solarnetwork.util.ObjectUtils;

/**
 * MQTT connection debugger.
 * 
 * @author matt
 * @version 1.0
 */
public class MqttConnectionDebugger extends BaseMqttConnectionObserver implements MqttMessageHandler {

	private String mqttTopic;

	/**
	 * Constructor.
	 * 
	 * @param mqttTopic
	 *        the topic to subscribe to
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MqttConnectionDebugger(String mqttTopic) {
		super();
		this.mqttTopic = ObjectUtils.requireNonNullArgument(mqttTopic, "mqttTopic");
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		logThreadDump();
		try {
			connection.subscribe(mqttTopic, MqttQos.AtLeastOnce, this).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {}", mqttTopic, connection);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {}: {}", mqttTopic, connection,
					e.toString());
		}
	}

	private void logThreadDump() {
		try {
			ThreadMXBean bean = ManagementFactory.getThreadMXBean();
			ThreadInfo[] infos = bean.dumpAllThreads(true, true);
			log.debug("THREAD DUMP:\n{}",
					Arrays.stream(infos).map(Object::toString).collect(Collectors.joining()));
		} catch ( Exception e ) {
			log.error("Error logging thread dump: {}", e.toString());
		}
	}

	@Override
	public void onMqttServerConnectionLost(MqttConnection connection, boolean willReconnect,
			Throwable cause) {
		super.onMqttServerConnectionLost(connection, willReconnect, cause);
		logThreadDump();
	}

	@Override
	public void onMqttMessage(MqttMessage message) {
		byte[] body = message.getPayload();
		log.debug("Received message on [{}]: {} bytes", message.getTopic(),
				body != null ? body.length : 0);
	}

}
