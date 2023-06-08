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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
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

	private final String mqttTopic;
	private final Executor executor;

	/**
	 * Constructor.
	 * 
	 * @param mqttTopic
	 *        the topic to subscribe to
	 * @param executor
	 *        the executor
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public MqttConnectionDebugger(String mqttTopic, Executor executor) {
		super();
		this.mqttTopic = ObjectUtils.requireNonNullArgument(mqttTopic, "mqttTopic");
		this.executor = ObjectUtils.requireNonNullArgument(executor, "executor");
	}

	@Override
	public void onMqttServerConnectionEstablished(MqttConnection connection, boolean reconnected) {
		super.onMqttServerConnectionEstablished(connection, reconnected);
		try {
			connection.subscribe(mqttTopic, MqttQos.AtLeastOnce, this).get(getSubscribeTimeoutSeconds(),
					TimeUnit.SECONDS);
			log.info("Subscribed to MQTT topic {} @ {}", mqttTopic, connection);
		} catch ( InterruptedException | ExecutionException | TimeoutException e ) {
			log.error("Failed to subscribe to MQTT topic {} @ {}: {}", mqttTopic, connection,
					e.toString());
		}
		logThreadDump();
	}

	private void logThreadDump() {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					ThreadMXBean bean = ManagementFactory.getThreadMXBean();
					ThreadInfo[] infos = bean.dumpAllThreads(true, true);
					log.debug("THREAD DUMP:\n{}", Arrays.stream(infos).map(e -> stackDump(e, 50))
							.collect(Collectors.joining()));
				} catch ( Exception e ) {
					log.error("Error logging thread dump: {}", e.toString());
				}
			}

		});
	}

	private static String stackDump(ThreadInfo info, final int maxFrames) {
		StringBuilder sb = new StringBuilder("\"" + info.getThreadName() + "\""
				+ (info.isDaemon() ? " daemon" : "") + " prio=" + info.getPriority() + " Id="
				+ info.getThreadId() + " " + info.getThreadState());
		if ( info.getLockName() != null ) {
			sb.append(" on " + info.getLockName());
		}
		if ( info.getLockOwnerName() != null ) {
			sb.append(" owned by \"" + info.getLockOwnerName() + "\" Id=" + info.getLockOwnerId());
		}
		if ( info.isSuspended() ) {
			sb.append(" (suspended)");
		}
		if ( info.isInNative() ) {
			sb.append(" (in native)");
		}
		sb.append('\n');
		final StackTraceElement[] stackTrace = info.getStackTrace();
		int i = 0;
		for ( ; i < stackTrace.length && i < maxFrames; i++ ) {
			StackTraceElement ste = stackTrace[i];
			sb.append("\tat " + ste.toString());
			sb.append('\n');
			if ( i == 0 && info.getLockInfo() != null ) {
				Thread.State ts = info.getThreadState();
				switch (ts) {
					case BLOCKED:
						sb.append("\t-  blocked on " + info.getLockInfo());
						sb.append('\n');
						break;
					case WAITING:
						sb.append("\t-  waiting on " + info.getLockInfo());
						sb.append('\n');
						break;
					case TIMED_WAITING:
						sb.append("\t-  waiting on " + info.getLockInfo());
						sb.append('\n');
						break;
					default:
				}
			}

			for ( MonitorInfo mi : info.getLockedMonitors() ) {
				if ( mi.getLockedStackDepth() == i ) {
					sb.append("\t-  locked " + mi);
					sb.append('\n');
				}
			}
		}
		if ( i < stackTrace.length ) {
			sb.append("\t...");
			sb.append('\n');
		}

		LockInfo[] locks = info.getLockedSynchronizers();
		if ( locks.length > 0 ) {
			sb.append("\n\tNumber of locked synchronizers = " + locks.length);
			sb.append('\n');
			for ( LockInfo li : locks ) {
				sb.append("\t- " + li);
				sb.append('\n');
			}
		}
		sb.append('\n');
		return sb.toString();

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
